// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk.meta;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.humainary.serventis.opt.flow.Breakers.Sign.*;
import static io.humainary.serventis.opt.sync.Locks.Sign.*;
import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.serventis.sdk.meta.Sequencers.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"SwitchStatementWithTooFewBranches", "DataFlowIssue"})
final class SequencersContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("sequencer.test");

  private static final SignSet< Locks.Sign > LOCKS =
    Locks.SIGNS;

  private static final SignSet< Breakers.Sign > BREAKERS =
    Breakers.SIGNS;

  private Circuit circuit;
  private CaptureBuffer< Statuses.Sign > captures;

  private static Flow< Locks.Sign, Statuses.Sign > locks() {

    return
      Sequencers.flow(
        locksRoot()
      );

  }

  private static SignMap< Locks.Sign, Sequencers.Transition< Locks.Sign > > locksRoot() {

    return
      LOCKS.map(
        sign -> switch (sign) {
          case ACQUIRE -> emit(
            DIVERGING,
            LOCKS.map(
              next -> switch (next) {
                case GRANT -> emit(
                  CONVERGING,
                  LOCKS.map(
                    end -> switch (end) {
                      case RELEASE -> emit(STABLE);
                      default -> null;
                    }
                  )
                );
                case TIMEOUT -> emit(DEGRADED);
                case RELEASE -> emit(DEFECTIVE);
                default -> null;
              }
            )
          );
          case RELEASE, ABANDON -> emit(DEFECTIVE);
          default -> emit(STABLE);
        }
      );

  }

  @Test
  void activeBreakWithoutRootPathSuppressesBaselineForThatAdmission() {

    final var in =
      attach(
        Sequencers.flow(
          LOCKS.map(
            sign -> switch (sign) {
              case ACQUIRE -> emit(
                DIVERGING,
                LOCKS.map(
                  next -> switch (next) {
                    case TIMEOUT -> emit(DEGRADED);
                    case RELEASE -> emit(null);
                    default -> null;
                  }
                )
              );
              case GRANT -> emit(
                null,
                LOCKS.map(
                  next -> switch (next) {
                    case RELEASE -> emit(CONVERGING);
                    default -> null;
                  }
                )
              );
              default -> emit(STABLE);
            }
          )
        )
      );

    in.emit(ACQUIRE);
    in.emit(RELEASE);
    in.emit(UPGRADE);

    assertOutcomes(DIVERGING, STABLE);

  }

  @Test
  void activeNullTransitionsSuppressBaselineAndKeepState() {

    final var in =
      attach(
        Sequencers.flow(
          LOCKS.map(
            sign -> switch (sign) {
              case ACQUIRE -> emit(
                null,
                LOCKS.map(
                  next -> switch (next) {
                    case TIMEOUT -> emit(DEGRADED);
                    default -> null;
                  }
                )
              );
              default -> emit(STABLE);
            }
          )
        )
      );

    in.emit(ACQUIRE);  // moves active silently
    in.emit(UPGRADE);  // null transition: stay active, no baseline
    in.emit(TIMEOUT);

    assertOutcomes(DEGRADED);

  }

  private void assertOutcomes(
    final Statuses.Sign... expected
  ) {

    assertEquals(
      List.of(expected),
      captures.drainEmissions().toList()
    );

  }

  private < S > Pipe< S > attach(
    final Flow< S, Statuses.Sign > flow
  ) {

    final var conduit =
      circuit.conduit(Statuses.Sign.class);

    captures =
      CaptureBuffer.of(circuit, conduit);

    return
      conduit.pool(flow)
        .get(NAME);

  }

  // ========== BASELINE AND SUPPRESSION ==========

  @Test
  void breakersFlapAsTransitionPairs() {

    final var in =
      attach(
        Sequencers.flow(
          BREAKERS.map(
            sign -> switch (sign) {
              case TRIP -> emit(DIVERGING);
              case HALF_OPEN -> emit(
                DIVERGING,
                BREAKERS.map(
                  next -> switch (next) {
                    case OPEN -> emit(DEGRADED);
                    case CLOSE -> emit(STABLE);
                    default -> null;
                  }
                )
              );
              default -> null;
            }
          )
        )
      );

    in.emit(TRIP);
    in.emit(HALF_OPEN);
    in.emit(OPEN);
    in.emit(HALF_OPEN);
    in.emit(CLOSE);

    assertOutcomes(
      DIVERGING,
      DIVERGING,
      DEGRADED,
      DIVERGING,
      STABLE
    );

  }

  @Test
  void breakingSignCanReanchorFromTheIdleState() {

    final var in = attach(locks());

    in.emit(ACQUIRE);
    in.emit(RELEASE);

    assertOutcomes(DIVERGING, DEFECTIVE);

  }

  @Test
  void flowWithoutBaselineFiltersIdleUnmatchedSigns() {

    final var in =
      attach(
        Sequencers.flow(
          LOCKS.map(
            sign -> switch (sign) {
              case RELEASE -> emit(DEFECTIVE);
              default -> null;
            }
          )
        )
      );

    in.emit(UPGRADE);
    in.emit(RELEASE);

    assertOutcomes(DEFECTIVE);

  }

  // ========== STATE TRANSITIONS ==========

  @Test
  void idleBaselineSpeaksWhenNoPathIsActive() {

    final var in = attach(locks());

    in.emit(UPGRADE);
    in.emit(DENY);

    assertOutcomes(STABLE, STABLE);

  }

  @Test
  void locksTrajectoryScenario() {

    final var in = attach(locks());

    in.emit(UPGRADE);
    in.emit(ACQUIRE);
    in.emit(GRANT);
    in.emit(RELEASE);
    in.emit(ACQUIRE);
    in.emit(TIMEOUT);

    assertOutcomes(
      STABLE,
      DIVERGING,
      CONVERGING,
      STABLE,
      DIVERGING,
      DEGRADED
    );

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

  }

  // ========== BREAK AND RE-ANCHOR ==========

  @AfterEach
  void teardown() {

    if (captures!=null) {
      captures.close();
    }
    circuit.closeAwait();

  }

  @Test
  void temporalPolicyArrivesAsExplicitSigns() {

    final var in =
      attach(
        Sequencers.flow(
          LOCKS.map(
            sign -> switch (sign) {
              case ACQUIRE -> emit(
                null,
                LOCKS.map(
                  next -> switch (next) {
                    case TIMEOUT -> emit(DEGRADED);
                    default -> null;
                  }
                )
              );
              default -> null;
            }
          )
        )
      );

    in.emit(ACQUIRE);
    in.emit(TIMEOUT);

    assertOutcomes(DEGRADED);

  }

  // ========== DOMAIN SCENARIOS ==========

  @Test
  void terminalTransitionResetsTheWalk() {

    final var in = attach(locks());

    in.emit(ACQUIRE);
    in.emit(TIMEOUT);
    in.emit(UPGRADE);

    assertOutcomes(DIVERGING, DEGRADED, STABLE);

  }

  @Test
  void transitionsSpeakImmediatelyAsTheWalkAdvances() {

    final var in = attach(locks());

    in.emit(ACQUIRE);
    in.emit(GRANT);
    in.emit(RELEASE);

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  // ========== PER-SUBJECT WALKS ==========

  @Test
  void walksArePerSubject() {

    final var conduit =
      circuit.conduit(Statuses.Sign.class);

    final var pool =
      conduit.pool(
        Sequencers.flow(
          LOCKS.map(
            sign -> switch (sign) {
              case ACQUIRE -> emit(
                DIVERGING,
                LOCKS.map(
                  next -> switch (next) {
                    case TIMEOUT -> emit(DEGRADED);
                    default -> null;
                  }
                )
              );
              default -> null;
            }
          )
        )
      );

    final var name1 = CORTEX.name("sequencer.a");
    final var name2 = CORTEX.name("sequencer.b");

    final var in1 = pool.get(name1);
    final var in2 = pool.get(name2);

    final var captures =
      CaptureBuffer.of(circuit, conduit);

    in1.emit(ACQUIRE);
    in2.emit(ACQUIRE);
    in1.emit(TIMEOUT);

    final var captured =
      captures.drain().toList();
    captures.close();

    assertEquals(3, captured.size());
    assertEquals(name1, captured.get(0).subject().name());
    assertEquals(DIVERGING, captured.get(0).emission());
    assertEquals(name2, captured.get(1).subject().name());
    assertEquals(DIVERGING, captured.get(1).emission());
    assertEquals(name1, captured.get(2).subject().name());
    assertEquals(DEGRADED, captured.get(2).emission());

  }

  // ========== ARGUMENT VALIDATION ==========

  @Nested
  final class ArgumentTests {

    private final SignMap< Locks.Sign, Sequencers.Transition< Locks.Sign > > state =
      LOCKS.map(
        _ -> null
      );

    @Test
    void flowRejectsNullArguments() {

      assertThrows(
        NullPointerException.class,
        () ->
          Sequencers.flow(
            (SignMap< Locks.Sign, Sequencers.Transition< Locks.Sign > >) null
          )
      );

    }

    @Test
    void stateMapMayReturnNullToStaySilent() {

      final var in =
        attach(
          Sequencers.flow(
            state
          )
        );

      in.emit(ACQUIRE);
      in.emit(UPGRADE);

      assertOutcomes();

    }

  }

}
