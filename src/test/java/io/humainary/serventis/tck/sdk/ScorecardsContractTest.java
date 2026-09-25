// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.api.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.sdk.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Resources.Sign.*;
import static io.humainary.serventis.sdk.Statuses.Dimension.*;
import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

/// TCK for [Scorecards] — the sign-to-status translation operator.
///
/// Ballot: GRANT → STABLE, DENY/TIMEOUT → DEGRADED, everything else abstains.
/// Policy under test: DECAY 0.95, warm-up 0.25, share bands 0.50 / 0.80. With a single
/// target repeatedly voted the winner share is always 1.0, so the band is governed purely
/// by accumulated evidence `1 - 0.95^votes` — which first reaches 0.25 on the 6th vote.

@SuppressWarnings("DataFlowIssue")  // deliberate null-rejection coverage
final class ScorecardsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("service.scorecard");

  private Circuit circuit;
  private CaptureBuffer< Statuses.Signal > captures;
  private Pipe< Resources.Sign > input;

  private static Statuses.Sign ballot(
    final Resources.Sign sign
  ) {

    return
      switch (sign) {
        case GRANT -> STABLE;
        case DENY, TIMEOUT -> DEGRADED;
        default -> null;
      };

  }

  @Test
  void abstainEmitsNothing() {

    input.emit(RELEASE);   // not in the ballot -> abstain

    assertEquals(0, drain().size());

  }

  @Test
  void abstainIsSkippedBetweenVotes() {

    input.emit(GRANT);
    input.emit(RELEASE);   // abstain: no emission, tally untouched
    input.emit(GRANT);

    final var signals = drain();

    assertEquals(2, signals.size());
    assertEquals(STABLE, signals.get(0).sign());
    assertEquals(STABLE, signals.get(1).sign());

  }

  @Test
  void ballotReadsSignalDimension() {

    // Free E with a Signal input: the same sign (FAIL) maps to different statuses depending on the
    // dimension — proving the ballot weighs sign × dimension, not just the bare sign.
    assertEquals(DEGRADED, lastTraceStatus(View.CALLEE).sign());
    assertEquals(Statuses.Sign.DIVERGING, lastTraceStatus(View.CALLER).sign());

  }

  @Test
  void dominanceBecomesConfirmed() {

    // 6 identical votes: evidence crosses the warm-up gate on the 6th, share is 1.0 throughout.
    for (var i = 0; i < 6; i++) {
      input.emit(GRANT);
    }

    final var signals = drain();

    assertEquals(6, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(TENTATIVE, signals.getFirst().dimension());
    assertEquals(STABLE, signals.getLast().sign());
    assertEquals(CONFIRMED, signals.getLast().dimension());

  }

  private java.util.List< Statuses.Signal > drain() {

    return
      captures
        .drainEmissions()
        .toList();

  }

  private Statuses.Signal lastTraceStatus(
    final View view
  ) {

    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    final var in =
      statuses.pool(
        Scorecards.< Trace > flow(
          trace -> switch (trace.sign()) {
            case CALL -> STABLE;
            case FAIL -> trace.dimension()==View.CALLEE
              ? DEGRADED
              :Statuses.Sign.DIVERGING;
          }
        )
      ).get(CORTEX.name("service.trace." + view));

    final var captures =
      CaptureBuffer.of(circuit, statuses);

    for (var i = 0; i < 6; i++) {
      in.emit(new Trace(Op.FAIL, view));
    }

    final var status =
      captures.drainEmissions().toList().getLast();
    captures.close();

    return status;

  }

  @Test
  void measuredBandWhenShareBetweenWeakAndStrong() {

    // Warm STABLE to CONFIRMED (6 votes, share = 1.0), then dilute with DENY so STABLE's
    // decayed share drops out of CONFIRMED (>= 0.80) into the MEASURED band [0.50, 0.80).
    for (var i = 0; i < 6; i++) {
      input.emit(GRANT);
    }

    input.emit(DENY);   // STABLE share ~0.834 — still >= 0.80, CONFIRMED
    input.emit(DENY);   // STABLE share ~0.710 — now in [0.50, 0.80), MEASURED

    final var signals = drain();

    assertEquals(8, signals.size());

    // 7th emission (1st DENY): STABLE still dominant, above the 0.80 boundary.
    assertEquals(STABLE, signals.get(6).sign());
    assertEquals(CONFIRMED, signals.get(6).dimension());

    // 8th emission (2nd DENY): STABLE share has fallen into the MEASURED band.
    assertEquals(STABLE, signals.get(7).sign());
    assertEquals(MEASURED, signals.get(7).dimension());

  }

  @Test
  void nullArgumentsRejected() {

    assertThrows(
      NullPointerException.class,
      () -> Scorecards.< Resources.Sign > flow(null)
    );

  }

  @Test
  void perSubjectIsolation() {

    final var name1 = CORTEX.name("service.a.scorecard");
    final var name2 = CORTEX.name("service.b.scorecard");

    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    final var pool =
      statuses.pool(
        Scorecards.flow(
          ScorecardsContractTest::ballot
        )
      );

    final var in1 = pool.get(name1);
    final var in2 = pool.get(name2);
    final var captures = CaptureBuffer.of(circuit, statuses);

    in1.emit(GRANT);   // -> STABLE
    in2.emit(DENY);    // -> DEGRADED
    in1.emit(GRANT);

    final var captured = captures.drain().toList();
    captures.close();

    assertEquals(3, captured.size());
    assertEquals(name1, captured.get(0).subject().name());
    assertEquals(STABLE, captured.get(0).emission().sign());
    assertEquals(name2, captured.get(1).subject().name());
    assertEquals(DEGRADED, captured.get(1).emission().sign());
    assertEquals(name1, captured.get(2).subject().name());
    assertEquals(STABLE, captured.get(2).emission().sign());

  }

  @Test
  void scoreIgnoresAbstainsAndFiltersEmptyWindow() {

    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    final var in =
      statuses.pool(
        CORTEX.flow(Resources.Sign.class)
          .window(4)
          .map(w -> Scorecards.score(w, ScorecardsContractTest::ballot))
      ).get(CORTEX.name("service.scorecard.window.abstain"));

    final var captures =
      CaptureBuffer.of(circuit, statuses);

    in.emit(RELEASE);      // window [R]     -> no votes -> filtered
    in.emit(RELEASE);      // window [R,R]   -> no votes -> filtered
    in.emit(GRANT);        // window [R,R,G] -> STABLE 1/1 (RELEASE abstains)

    final var signals =
      captures.drainEmissions().toList();
    captures.close();

    assertEquals(1, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(CONFIRMED, signals.getFirst().dimension());

  }

  @Test
  void scoreOverWindowHasNoMemoryBeyondIt() {

    // Windowed scoring: assess the last 4 signs, equal weight, nothing carried beyond the window.
    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    final var in =
      statuses.pool(
        CORTEX.flow(Resources.Sign.class)
          .window(4)
          .map(w -> Scorecards.score(w, ScorecardsContractTest::ballot))
      ).get(CORTEX.name("service.scorecard.window"));

    final var captures =
      CaptureBuffer.of(circuit, statuses);

    for (var i = 0; i < 4; i++) {
      in.emit(GRANT);      // window fills with GRANT
    }
    for (var i = 0; i < 4; i++) {
      in.emit(DENY);       // GRANTs age out; window becomes all DENY
    }

    final var signals =
      captures.drainEmissions().toList();
    captures.close();

    assertEquals(8, signals.size());

    // 4th emission: window [G,G,G,G] -> STABLE, unanimous over the window.
    assertEquals(STABLE, signals.get(3).sign());
    assertEquals(CONFIRMED, signals.get(3).dimension());

    // 8th emission: window [D,D,D,D] -> the GRANTs are gone, so DEGRADED (no memory beyond window).
    assertEquals(DEGRADED, signals.getLast().sign());
    assertEquals(CONFIRMED, signals.getLast().dimension());

  }

  @Test
  void scoreRejectsNullWindow() {

    assertThrows(
      NullPointerException.class,
      () -> Scorecards.score(null, ScorecardsContractTest::ballot)
    );

  }

  @Test
  void scoresNonSignValues() {

    // Free E with a raw value stream: a metric classified into status by the ballot.
    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    final var in =
      statuses.pool(
        Scorecards.< Integer > flow(
          latency -> latency < 100 ? STABLE:DEGRADED
        )
      ).get(CORTEX.name("service.latency"));

    final var captures =
      CaptureBuffer.of(circuit, statuses);

    for (var i = 0; i < 6; i++) {
      in.emit(250);   // all over threshold -> DEGRADED
    }

    final var signals =
      captures.drainEmissions().toList();
    captures.close();

    assertEquals(DEGRADED, signals.getLast().sign());
    assertEquals(CONFIRMED, signals.getLast().dimension());

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final Conduit< Statuses.Signal > statuses =
      circuit.conduit(Statuses.Signal.class);

    input =
      statuses.pool(
        Scorecards.flow(
          ScorecardsContractTest::ballot
        )
      ).get(NAME);

    captures =
      CaptureBuffer.of(circuit, statuses);

  }

  @Test
  void singleVoteIsTentative() {

    input.emit(GRANT);

    final var signals = drain();

    assertEquals(1, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(TENTATIVE, signals.getFirst().dimension());

  }

  @AfterEach
  void teardown() {

    captures.close();
    circuit
      .closeAwait();

  }

  @Test
  void winnerFlipsOnExactVote() {

    // 10 STABLE votes establish STABLE as winner; DENY votes then erode its decayed share.
    // With DECAY = 0.95, DEGRADED overtakes STABLE on exactly the 7th DENY (overall emission 17):
    //   after 6 DENY: STABLE 0.2950 > DEGRADED 0.2649  (STABLE still wins)
    //   after 7 DENY: DEGRADED 0.3017 > STABLE 0.2802  (flip)
    for (var i = 0; i < 10; i++) {
      input.emit(GRANT);
    }

    for (var i = 0; i < 7; i++) {
      input.emit(DENY);
    }

    final var signals = drain();

    assertEquals(17, signals.size());

    // index 15 = 6th DENY: STABLE still the winner.
    assertEquals(STABLE, signals.get(15).sign());

    // index 16 = 7th DENY: DEGRADED overtakes — the exact flip vote.
    assertEquals(DEGRADED, signals.get(16).sign());

  }

  @Test
  void winnerSwitches() {

    for (var i = 0; i < 10; i++) {
      input.emit(GRANT);
    }

    for (var i = 0; i < 30; i++) {
      input.emit(DENY);
    }

    final var signals = drain();

    assertEquals(40, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(DEGRADED, signals.getLast().sign());
    assertEquals(CONFIRMED, signals.getLast().dimension());

  }

  /// Local Signaler-style fixture: a sign with a perspective dimension (cf. Services CALLER/CALLEE).
  enum Op implements Serventis.Sign {
    CALL,
    FAIL
  }

  enum View implements Serventis.Category {
    CALLER,
    CALLEE
  }

  record Trace(
    Op sign,
    View dimension
  ) implements Serventis.Signal< Op, View > {
  }

}
