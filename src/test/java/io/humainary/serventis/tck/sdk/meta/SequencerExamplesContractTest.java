// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk.meta;

import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.humainary.serventis.opt.role.Actors.Sign.*;
import static io.humainary.serventis.opt.sync.Atomics.Sign.*;
import static io.humainary.serventis.sdk.Operations.Sign.*;
import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.serventis.sdk.meta.Sequencers.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

/// Executable form of the worked examples in `SEQUENCERS.md` (§8). Each machine is the one shown in
/// the guide; the scenarios assert the status trajectory it produces. Keeping these as tests keeps the
/// guide honest — the examples must compile against the real sign sets and behave as documented.
///
/// (Locks — §8.1 — is already covered by [SequencersContractTest]; this file adds Operations, Actors, Atomics.)

final class SequencerExamplesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("sequencer.example");

  private Circuit circuit;
  private CaptureBuffer< Statuses.Sign > captures;

  // ========== §8.2 Operations — the minimal BEGIN/END bracket ==========

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  private static Flow< Actors.Sign, Statuses.Sign > actors() {

    final var a = Actors.SIGNS;

    return
      Sequencers.flow(
        a.map(
          sign -> switch (sign) {                                   // idle
            case REQUEST -> emit(DIVERGING,                         // asked — awaiting commitment
              a.map(
                m -> switch (m) {                                   // awaiting
                  case PROMISE -> emit(CONVERGING,                  // committed
                    a.map(
                      d -> switch (d) {                             // promised
                        case DELIVER -> emit(STABLE);              // fulfilled → reset
                        case REQUEST -> emit(DEFECTIVE);           // new ask before delivery: promise broke → reset
                        default -> null;                              // stay promised
                      }
                    )
                  );
                  case DENY -> emit(DEGRADED);                     // refused → reset
                  default -> null;                                    // stay awaiting (ACKNOWLEDGE, CLARIFY, …)
                }
              )
            );
            default -> null;                                          // idle, silent
          }
        )
      );

  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  private static Flow< Atomics.Sign, Statuses.Sign > atomics() {

    final var a = Atomics.SIGNS;

    return
      Sequencers.flow(
        a.map(
          sign -> switch (sign) {                                   // idle
            case ATTEMPT -> emit(CONVERGING,                        // attempting
              a.map(
                light -> switch (light) {                           // contended (light)
                  case SUCCESS -> emit(STABLE);                    // won
                  case EXHAUST -> emit(DEFECTIVE);                 // gave up
                  case FAIL -> emit(
                    DIVERGING,                                        // first failure → escalate
                    a.map(
                      heavy -> switch (heavy) {                     // retrying (heavy)
                        case SUCCESS -> emit(CONVERGING);          // recovered → reset
                        case EXHAUST -> emit(DEFECTIVE);           // gave up → reset
                        case BACKOFF, PARK -> emit(DEGRADED);      // deep wait → reset
                        default -> null;                              // SPIN/YIELD/FAIL: stay heavy
                      }
                    )
                  );
                  default -> null;                                    // SPIN/YIELD/BACKOFF/PARK before a FAIL: stay light
                }
              )
            );
            default -> null;                                          // idle, silent
          }
        )
      );

  }

  private static Flow< Operations.Sign, Statuses.Sign > operations() {

    final var o = Operations.SIGNS;

    return
      Sequencers.flow(
        o.map(
          sign -> switch (sign) {                 // idle
            case BEGIN -> emit(DIVERGING,         // opened
              o.map(
                inner -> switch (inner) {         // in flight
                  case END -> emit(STABLE);      // closed cleanly → reset
                  case BEGIN -> emit(DEFECTIVE); // overlapping begin: prior bracket broke → reset
                  case ADVANCE -> null;             // mid-span step → stay, silent
                }
              )
            );
            case END -> emit(DEFECTIVE);         // close with nothing open (orphan)
            case ADVANCE -> null;                   // idle, silent
          }
        )
      );

  }

  @Test
  void actorsBrokenPromise() {

    final var in = attach(actors());

    in.emit(REQUEST);
    in.emit(PROMISE);
    in.emit(REQUEST);   // new ask before delivery

    assertOutcomes(DIVERGING, CONVERGING, DEFECTIVE);

  }

  // ========== §8.3 Actors — a conversation (structure is the only status) ==========

  @Test
  void actorsFulfilledExchange() {

    final var in = attach(actors());

    in.emit(REQUEST);
    in.emit(PROMISE);
    in.emit(DELIVER);

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void actorsRefusal() {

    final var in = attach(actors());

    in.emit(REQUEST);
    in.emit(DENY);

    assertOutcomes(DIVERGING, DEGRADED);

  }

  @Test
  void actorsSwallowsInterveningActs() {

    final var in = attach(actors());

    in.emit(REQUEST);
    in.emit(ACKNOWLEDGE);   // activity* — swallowed, stays awaiting
    in.emit(PROMISE);
    in.emit(DELIVER);

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  private void assertOutcomes(
    final Statuses.Sign... expected
  ) {

    assertEquals(
      List.of(expected),
      captures.drainEmissions().toList()
    );

  }

  @Test
  void atomicsDeepensThenDegrades() {

    final var in = attach(atomics());

    in.emit(ATTEMPT);
    in.emit(FAIL);
    in.emit(BACKOFF);

    assertOutcomes(CONVERGING, DIVERGING, DEGRADED);

  }

  // ========== §8.4 Atomics — a deepening contention trajectory ==========

  @Test
  void atomicsExhausts() {

    final var in = attach(atomics());

    in.emit(ATTEMPT);
    in.emit(FAIL);
    in.emit(EXHAUST);

    assertOutcomes(CONVERGING, DIVERGING, DEFECTIVE);

  }

  @Test
  void atomicsQuickWin() {

    final var in = attach(atomics());

    in.emit(ATTEMPT);
    in.emit(SUCCESS);

    assertOutcomes(CONVERGING, STABLE);

  }

  @Test
  void atomicsRecoversAfterContention() {

    final var in = attach(atomics());

    in.emit(ATTEMPT);
    in.emit(FAIL);
    in.emit(SUCCESS);

    assertOutcomes(CONVERGING, DIVERGING, CONVERGING);

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

  @Test
  void operationsCleanBracket() {

    final var in = attach(operations());

    in.emit(BEGIN);
    in.emit(END);

    assertOutcomes(DIVERGING, STABLE);

  }

  // ========== harness ==========

  @Test
  void operationsOrphanClose() {

    final var in = attach(operations());

    in.emit(END);

    assertOutcomes(DEFECTIVE);

  }

  @Test
  void operationsOverlappingOpenBreaks() {

    final var in = attach(operations());

    in.emit(BEGIN);
    in.emit(BEGIN);   // second open before a close: prior bracket broke, emit + reset to idle

    assertOutcomes(DIVERGING, DEFECTIVE);

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

  }

  @AfterEach
  void teardown() {

    captures.close();
    circuit.closeAwait();

  }

}
