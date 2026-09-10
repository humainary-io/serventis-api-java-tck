// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk.meta;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

/// Proves the universal bracket sequencer — `Sequencers.flow ( OPERATION, STATUS )` — produces the
/// documented status trajectory for *any* domain straight from its `OPERATION` bracket classification
/// and its canonical `STATUS` map (see `SEQUENCERS.md`). The SAME operator drives Locks, Atomics,
/// Transactions, Tasks, Probes, Leases, and Processes.
///
/// Two structural scenarios are pinned: a denied acquisition followed by a release, and a completed
/// task followed by a cancel — both must close on the first terminal sign and read the trailing closer
/// as an **orphan** `DEFECTIVE`, not a false `STABLE`. The forced-close scenarios are pinned too: a
/// lease `REVOKE` (`DEFECTIVE`), a process `KILL` (`DEGRADED`), and a transaction `CONFLICT`
/// (`DEGRADED`) must surface their canonical severity rather than collapse to a false `STABLE` — the
/// reason the sequencer reads `STATUS`, not a coarser success/fail verdict.

final class BracketSequencerContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("bracket.test");

  private Circuit circuit;
  private CaptureBuffer< Statuses.Sign > captures;

  private void assertOutcomes(
    final Statuses.Sign... expected
  ) {

    assertEquals(
      List.of(expected),
      captures.drainEmissions().toList()
    );

  }

  @Test
  void atomicsExhaustClosesDefective() {

    final var in =
      attach(Sequencers.flow(Atomics.OPERATION, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN                     → DIVERGING
    in.emit(Atomics.Sign.FAIL);    // ADVANCE (retry), DEGRADED  → DEGRADED, span stays open
    in.emit(Atomics.Sign.EXHAUST); // CLOSE, STATUS DEFECTIVE   → DEFECTIVE (gave up)

    assertOutcomes(DIVERGING, DEGRADED, DEFECTIVE);

  }

  @Test
  void atomicsFailAdvancesThenRecovers() {

    final var in =
      attach(Sequencers.flow(Atomics.OPERATION, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN                    → DIVERGING
    in.emit(Atomics.Sign.FAIL);    // ADVANCE (retry), DEGRADED → DEGRADED, span STAYS OPEN
    in.emit(Atomics.Sign.SUCCESS); // CLOSE + ok              → STABLE (the retry won)

    assertOutcomes(DIVERGING, DEGRADED, STABLE);

  }

  @Test
  void atomicsTerminalSuccessClosesStable() {

    final var in =
      attach(Sequencers.flow(Atomics.OPERATION, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN          → DIVERGING
    in.emit(Atomics.Sign.SUCCESS); // CLOSE + ok    → STABLE (the CAS is done)

    assertOutcomes(DIVERGING, STABLE);

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
  void leasesRevokeCloseIsDefective() {

    final var in =
      attach(Sequencers.flow(Leases.OPERATION, Leases.STATUS));

    in.emit(Leases.Sign.ACQUIRE);   // OPEN                    → DIVERGING
    in.emit(Leases.Sign.REVOKE);    // CLOSE, STATUS DEFECTIVE → DEFECTIVE (a forced, operation-only close)

    assertOutcomes(DIVERGING, DEFECTIVE);

  }

  @Test
  void locksCleanLifecycle() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN          → DIVERGING
    in.emit(Locks.Sign.GRANT);     // ADVANCE + ok  → CONVERGING (still held)
    in.emit(Locks.Sign.RELEASE);   // CLOSE         → STABLE

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void locksDeniedThenReleaseIsOrphan() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN              → DIVERGING
    in.emit(Locks.Sign.DENY);      // CLOSE + fail      → DEGRADED, span popped
    in.emit(Locks.Sign.RELEASE);   // CLOSE, none open  → DEFECTIVE (orphan)

    assertOutcomes(DIVERGING, DEGRADED, DEFECTIVE);

  }

  @Test
  void locksMidSpanOperationsAreSilent() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);
    in.emit(Locks.Sign.UPGRADE);   // ADVANCE, STATUS abstains → silent
    in.emit(Locks.Sign.GRANT);
    in.emit(Locks.Sign.RELEASE);

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void locksOrphanCloseIsDefective() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.RELEASE);   // CLOSE with nothing open

    assertOutcomes(DEFECTIVE);

  }

  @Test
  void nonLatticeStatusFoldsBySeverityNotMasked() {

    // A canonical STATUS map yields only STABLE/DEGRADED/DEFECTIVE/null, but the overload is typed for
    // any Statuses.Sign. A (synthetic) map that advances on ERRATIC and closes on DOWN must surface the
    // severity — ERRATIC → DEGRADED, DOWN → DEFECTIVE — never silently mask it as a healthy STABLE.

    final var operation =
      Locks.SIGNS.map(
        sign -> switch (sign) {
          case ATTEMPT -> Operations.Sign.BEGIN;
          case RELEASE -> Operations.Sign.END;
          default -> Operations.Sign.ADVANCE;
        }
      );

    final var status =
      Locks.SIGNS.map(
        sign -> switch (sign) {
          case GRANT -> ERRATIC;   // mid-span, non-lattice
          case RELEASE -> DOWN;      // close, non-lattice (most severe)
          default -> null;
        }
      );

    final var in =
      attach(Sequencers.flow(operation, status));

    in.emit(Locks.Sign.ATTEMPT);   // BEGIN                   → DIVERGING
    in.emit(Locks.Sign.GRANT);     // ADVANCE, STATUS ERRATIC → DEGRADED (not silent)
    in.emit(Locks.Sign.RELEASE);   // END, STATUS DOWN        → DEFECTIVE (not a masked STABLE)

    assertOutcomes(DIVERGING, DEGRADED, DEFECTIVE);

  }

  @Test
  void overlappingOpenIsDefective() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN                       → DIVERGING
    in.emit(Locks.Sign.ATTEMPT);   // second OPEN, prior unclosed → DEFECTIVE (re-opens)

    assertOutcomes(DIVERGING, DEFECTIVE);

  }

  @Test
  void probesConnectionLifecycle() {

    final var in =
      attach(Sequencers.flow(Probes.OPERATION, Probes.STATUS));

    in.emit(Probes.Sign.CONNECT);    // OPEN            → DIVERGING
    in.emit(Probes.Sign.SUCCEED);    // ADVANCE + ok    → CONVERGING (still connected)
    in.emit(Probes.Sign.DISCONNECT); // CLOSE           → STABLE

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void processesKillCloseIsDegraded() {

    final var in =
      attach(Sequencers.flow(Processes.OPERATION, Processes.STATUS));

    in.emit(Processes.Sign.SPAWN);   // OPEN                   → DIVERGING
    in.emit(Processes.Sign.KILL);    // CLOSE, STATUS DEGRADED → DEGRADED (a forced, operation-only close)

    assertOutcomes(DIVERGING, DEGRADED);

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

  }

  @Test
  void strayAdvanceWithNoOpenSpanIsSilent() {

    final var in =
      attach(Sequencers.flow(Locks.OPERATION, Locks.STATUS));

    in.emit(Locks.Sign.GRANT);   // ADVANCE with nothing open → silent (no positive reading)

    assertOutcomes();

  }

  // ===== harness =====

  @Test
  void tasksCompleteThenCancelIsOrphan() {

    final var in =
      attach(Sequencers.flow(Tasks.OPERATION, Tasks.STATUS));

    in.emit(Tasks.Sign.SUBMIT);    // OPEN              → DIVERGING
    in.emit(Tasks.Sign.COMPLETE);  // CLOSE + ok        → STABLE, span popped
    in.emit(Tasks.Sign.CANCEL);    // CLOSE, none open  → DEFECTIVE (orphan)

    assertOutcomes(DIVERGING, STABLE, DEFECTIVE);

  }

  @AfterEach
  void teardown() {

    captures.close();
    circuit.closeAwait();

  }

  @Test
  void transactionsCommitTrajectory() {

    final var in =
      attach(Sequencers.flow(Transactions.OPERATION, Transactions.STATUS));

    in.emit(Transactions.Sign.START);
    in.emit(Transactions.Sign.COMMIT);

    assertOutcomes(DIVERGING, STABLE);

  }

  @Test
  void transactionsConflictCloseIsDegraded() {

    final var in =
      attach(Sequencers.flow(Transactions.OPERATION, Transactions.STATUS));

    in.emit(Transactions.Sign.START);      // OPEN                  → DIVERGING
    in.emit(Transactions.Sign.CONFLICT);   // CLOSE, STATUS DEGRADED → DEGRADED (not a false STABLE)

    assertOutcomes(DIVERGING, DEGRADED);

  }

}
