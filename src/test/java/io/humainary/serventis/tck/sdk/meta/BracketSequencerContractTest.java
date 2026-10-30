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
/// documented status trajectory from an `OPERATION` bracket classification and a canonical `STATUS`
/// map (see `SEQUENCERS.md`). Processes and Changes publish their `OPERATION` maps; for the other
/// domains exercised here the map is built in this class, as a consumer builds one for a stream it
/// has already separated — one lock holder, one retrying thread, one transaction — since those
/// vocabularies interleave on a shared subject and publish none. The SAME operator drives all of
/// them.
///
/// Two structural scenarios are pinned: a denied acquisition followed by a release, and a completed
/// task followed by a cancel — both must close on the first terminal sign and read the trailing closer
/// as an **orphan** `DEFECTIVE`, not a false `STABLE`. The forced-close scenarios are pinned too: a
/// lease `REVOKE` (`DEFECTIVE`), a process `KILL` (`DEGRADED`), and a transaction `CONFLICT` answered
/// by `ROLLBACK` (`DEGRADED`) must surface their canonical severity rather than collapse to a false
/// `STABLE` — the reason the sequencer reads `STATUS`, not a coarser success/fail verdict. A process
/// restarted after a crash opens a fresh episode rather than leaving its clean stop an orphan.

final class BracketSequencerContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("bracket.test");

  // Episode maps for separated streams: what a consumer builds with SIGNS.map once it has one
  // holder's, one thread's or one run's signs on a subject of their own.

  private static final SignMap< Locks.Sign, Operations.Sign > LOCK_HOLDER =
    Locks.SIGNS.map(sign -> switch (sign) {
      case ATTEMPT, ACQUIRE -> Operations.Sign.BEGIN;
      case GRANT, CONTEST, UPGRADE, DOWNGRADE -> Operations.Sign.ADVANCE;
      case DENY, TIMEOUT, RELEASE, ABANDON -> Operations.Sign.END;
    });

  private static final SignMap< Resources.Sign, Operations.Sign > RESOURCE_HOLDER =
    Resources.SIGNS.map(sign -> switch (sign) {
      case ATTEMPT, ACQUIRE -> Operations.Sign.BEGIN;
      case GRANT -> Operations.Sign.ADVANCE;
      case DENY, TIMEOUT, RELEASE -> Operations.Sign.END;
    });

  private static final SignMap< Atomics.Sign, Operations.Sign > RETRYING_THREAD =
    Atomics.SIGNS.map(sign -> switch (sign) {
      case ATTEMPT -> Operations.Sign.BEGIN;
      case FAIL, SPIN, YIELD, BACKOFF, PARK -> Operations.Sign.ADVANCE;
      case SUCCESS, EXHAUST -> Operations.Sign.END;
    });

  private static final SignMap< Leases.Sign, Operations.Sign > LEASE_HOLDER =
    Leases.SIGNS.map(sign -> switch (sign) {
      case ACQUIRE -> Operations.Sign.BEGIN;
      case GRANT, EXTEND, RENEW, PROBE -> Operations.Sign.ADVANCE;
      case DENY, EXPIRE, RELEASE, REVOKE -> Operations.Sign.END;
    });

  private static final SignMap< Tasks.Sign, Operations.Sign > TASK_RUN =
    Tasks.SIGNS.map(sign -> switch (sign) {
      case SUBMIT -> Operations.Sign.BEGIN;
      case SCHEDULE, START, PROGRESS, SUSPEND, RESUME -> Operations.Sign.ADVANCE;
      case CANCEL, REJECT, COMPLETE, FAIL, TIMEOUT -> Operations.Sign.END;
    });

  private static final SignMap< Transactions.Sign, Operations.Sign > TRANSACTION =
    Transactions.SIGNS.map(sign -> switch (sign) {
      case START -> Operations.Sign.BEGIN;
      case PREPARE, COMPENSATE, EXPIRE, CONFLICT -> Operations.Sign.ADVANCE;
      case COMMIT, ROLLBACK, ABORT -> Operations.Sign.END;
    });

  private static final SignMap< Probes.Sign, Operations.Sign > CONNECTION =
    Probes.SIGNS.map(sign -> switch (sign) {
      case CONNECT -> Operations.Sign.BEGIN;
      case TRANSFER, PROCESS, SUCCEED, FAIL -> Operations.Sign.ADVANCE;
      case DISCONNECT -> Operations.Sign.END;
    });

  // One sign per episode role, for driving the folds with any status reading: ATTEMPT opens, GRANT
  // advances, RELEASE closes, and the rest are not used.
  private static final SignMap< Locks.Sign, Operations.Sign > SYNTHETIC_SPAN =
    Locks.SIGNS.map(sign -> switch (sign) {
      case ATTEMPT -> Operations.Sign.BEGIN;
      case GRANT -> Operations.Sign.ADVANCE;
      case RELEASE -> Operations.Sign.END;
      default -> null;
    });

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

  /// A map a consumer derives may abstain; an abstained sign is silent and
  /// leaves the span as it is, rather than failing the scan.
  @Test
  void abstainingOperationIsSilent() {

    final var operation =
      Resources.SIGNS.map(
        sign -> sign == Resources.Sign.ATTEMPT ? null : RESOURCE_HOLDER.apply(sign)
      );

    final var in =
      attach(Sequencers.flow(operation, Resources.STATUS));

    in.emit(Resources.Sign.ATTEMPT); // abstained              → silent
    in.emit(Resources.Sign.ACQUIRE); // OPEN                   → DIVERGING
    in.emit(Resources.Sign.ATTEMPT); // abstained, span intact → silent (not an overlapping open)
    in.emit(Resources.Sign.GRANT);   // ADVANCE, STATUS STABLE → CONVERGING
    in.emit(Resources.Sign.RELEASE); // CLOSE                  → STABLE

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void atomicsExhaustClosesDefective() {

    final var in =
      attach(Sequencers.flow(RETRYING_THREAD, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN                     → DIVERGING
    in.emit(Atomics.Sign.FAIL);    // ADVANCE (retry), DEGRADED  → DEGRADED, span stays open
    in.emit(Atomics.Sign.EXHAUST); // CLOSE, STATUS DEFECTIVE   → DEFECTIVE (gave up)

    assertOutcomes(DIVERGING, DEGRADED, DEFECTIVE);

  }

  @Test
  void atomicsFailAdvancesThenRecovers() {

    final var in =
      attach(Sequencers.flow(RETRYING_THREAD, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN                    → DIVERGING
    in.emit(Atomics.Sign.FAIL);    // ADVANCE (retry), DEGRADED → DEGRADED, span STAYS OPEN
    in.emit(Atomics.Sign.SUCCESS); // CLOSE + ok              → STABLE (the retry won)

    assertOutcomes(DIVERGING, DEGRADED, STABLE);

  }

  @Test
  void atomicsTerminalSuccessClosesStable() {

    final var in =
      attach(Sequencers.flow(RETRYING_THREAD, Atomics.STATUS));

    in.emit(Atomics.Sign.ATTEMPT); // OPEN          → DIVERGING
    in.emit(Atomics.Sign.SUCCESS); // CLOSE + ok    → STABLE (the CAS is done)

    assertOutcomes(DIVERGING, STABLE);

  }

  @Test
  void changesCloseOnTheirOwnReading() {

    final var in =
      attach(Sequencers.flow(Changes.OPERATION, Changes.STATUS));

    in.emit(Changes.Sign.START);    // OPEN                              → DIVERGING
    in.emit(Changes.Sign.REVERT);   // CLOSE, STATUS DEGRADED            → DEGRADED
    in.emit(Changes.Sign.START);    // OPEN                              → DIVERGING
    in.emit(Changes.Sign.REJECT);   // CLOSE, STATUS abstains            → STABLE (a refused change is the system working)
    in.emit(Changes.Sign.START);    // OPEN                              → DIVERGING
    in.emit(Changes.Sign.PROGRESS); // ADVANCE, STATUS abstains          → (silent)
    in.emit(Changes.Sign.APPLY);    // CLOSE, STATUS STABLE              → STABLE

    assertOutcomes(DIVERGING, DEGRADED, DIVERGING, STABLE, DIVERGING, STABLE);

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

  /// The B.4.1 example: a `DOWN` advance followed by a clean close reads two readings in sequence,
  /// `DEFECTIVE` then `STABLE`. The operator carries only the open bit, so the close does not
  /// remember the severe advance before it.
  @Test
  void downAdvanceThenCleanCloseReadsDefectiveThenStable() {

    final var in =
      attach(Sequencers.flow(SYNTHETIC_SPAN, Locks.SIGNS.map(sign -> sign==Locks.Sign.GRANT ? DOWN:null)));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN                   → DIVERGING
    in.emit(Locks.Sign.GRANT);     // ADVANCE, STATUS DOWN   → DEFECTIVE
    in.emit(Locks.Sign.RELEASE);   // CLOSE, STATUS abstains → STABLE

    assertOutcomes(DIVERGING, DEFECTIVE, STABLE);

  }

  /// Every status reading, and abstention, folds as the B.4.1 tables give — once advancing and
  /// once closing. A synthetic map supplies each value in turn, since no canonical `STATUS` map
  /// reads `CONVERGING`, `DIVERGING`, `ERRATIC` or `DOWN`; without it a fold that softened a severe
  /// advance to `DEGRADED` would pass every other case here.
  @Test
  void everyStatusFoldsAsSpecifiedWhenAdvancingAndClosing() {

    final var readings = new ArrayList< Statuses.Sign >(List.of(Statuses.Sign.values()));
    readings.add(null);

    for (final var reading : readings) {

      final var in =
        attach(Sequencers.flow(SYNTHETIC_SPAN, Locks.SIGNS.map(sign -> sign==Locks.Sign.ATTEMPT ? null:reading)));

      in.emit(Locks.Sign.ATTEMPT);   // OPEN
      in.emit(Locks.Sign.GRANT);     // ADVANCE, STATUS = reading
      in.emit(Locks.Sign.RELEASE);   // CLOSE, STATUS = reading

      final var expected = new ArrayList< Statuses.Sign >();
      expected.add(DIVERGING);

      final var advancing = advancing(reading);
      if (advancing!=null) {
        expected.add(advancing);
      }

      expected.add(closing(reading));

      assertEquals(expected, captures.drainEmissions().toList(), "reading " + reading);

    }

  }

  /// B.4.1's advancing fold: an advance is silent where its sign abstains.
  private static Statuses.Sign advancing(
    final Statuses.Sign reading
  ) {

    return
      reading==null ? null:switch (reading) {
        case STABLE, CONVERGING -> CONVERGING;
        case DEGRADED, ERRATIC, DIVERGING -> DEGRADED;
        case DEFECTIVE, DOWN -> DEFECTIVE;
      };

  }

  /// B.4.1's closing fold: a close that abstains still reads `STABLE`, because an episode completed.
  private static Statuses.Sign closing(
    final Statuses.Sign reading
  ) {

    return
      reading==null ? STABLE:switch (reading) {
        case STABLE, CONVERGING -> STABLE;
        case DEGRADED, ERRATIC, DIVERGING -> DEGRADED;
        case DEFECTIVE, DOWN -> DEFECTIVE;
      };

  }

  @Test
  void leasesRevokeCloseIsDefective() {

    final var in =
      attach(Sequencers.flow(LEASE_HOLDER, Leases.STATUS));

    in.emit(Leases.Sign.ACQUIRE);   // OPEN                    → DIVERGING
    in.emit(Leases.Sign.REVOKE);    // CLOSE, STATUS DEFECTIVE → DEFECTIVE (a forced, operation-only close)
    in.emit(Leases.Sign.ACQUIRE);   // OPEN again              → DIVERGING (proves REVOKE closed, not advanced)

    assertOutcomes(DIVERGING, DEFECTIVE, DIVERGING);

  }

  @Test
  void locksCleanLifecycle() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN          → DIVERGING
    in.emit(Locks.Sign.GRANT);     // ADVANCE + ok  → CONVERGING (still held)
    in.emit(Locks.Sign.RELEASE);   // CLOSE         → STABLE

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void locksDeniedThenReleaseIsOrphan() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN              → DIVERGING
    in.emit(Locks.Sign.DENY);      // CLOSE + fail      → DEGRADED, span popped
    in.emit(Locks.Sign.RELEASE);   // CLOSE, none open  → DEFECTIVE (orphan)

    assertOutcomes(DIVERGING, DEGRADED, DEFECTIVE);

  }

  @Test
  void locksMidSpanOperationsAreSilent() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);
    in.emit(Locks.Sign.UPGRADE);   // ADVANCE, STATUS abstains → silent
    in.emit(Locks.Sign.GRANT);
    in.emit(Locks.Sign.RELEASE);

    assertOutcomes(DIVERGING, CONVERGING, STABLE);

  }

  @Test
  void locksOrphanCloseIsDefective() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

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

  /// `flow(operation, status)` rejects an absent map of either kind, as documented.
  @Test
  void nullMapsAreRejected() {

    assertThrows(NullPointerException.class, () -> Sequencers.flow(null, Locks.STATUS));
    assertThrows(NullPointerException.class, () -> Sequencers.flow(LOCK_HOLDER, null));

  }

  @Test
  void overlappingOpenIsDefective() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

    in.emit(Locks.Sign.ATTEMPT);   // OPEN                       → DIVERGING
    in.emit(Locks.Sign.ATTEMPT);   // second OPEN, prior unclosed → DEFECTIVE (re-opens)

    assertOutcomes(DIVERGING, DEFECTIVE);

  }

  @Test
  void probesConnectionLifecycle() {

    final var in =
      attach(Sequencers.flow(CONNECTION, Probes.STATUS));

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
    in.emit(Processes.Sign.SPAWN);   // OPEN again             → DIVERGING (proves KILL closed, not advanced)

    assertOutcomes(DIVERGING, DEGRADED, DIVERGING);

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

  }

  @Test
  void strayAdvanceWithNoOpenSpanIsSilent() {

    final var in =
      attach(Sequencers.flow(LOCK_HOLDER, Locks.STATUS));

    in.emit(Locks.Sign.GRANT);   // ADVANCE with nothing open → silent (no positive reading)

    assertOutcomes();

  }

  @Test
  void tasksCompleteThenCancelIsOrphan() {

    final var in =
      attach(Sequencers.flow(TASK_RUN, Tasks.STATUS));

    in.emit(Tasks.Sign.SUBMIT);    // OPEN              → DIVERGING
    in.emit(Tasks.Sign.COMPLETE);  // CLOSE + ok        → STABLE, span popped
    in.emit(Tasks.Sign.CANCEL);    // CLOSE, none open  → DEFECTIVE (orphan)

    assertOutcomes(DIVERGING, STABLE, DEFECTIVE);

  }

  @AfterEach
  void teardown() {

    circuit.closeAwait();

  }

  @Test
  void transactionsCommitTrajectory() {

    final var in =
      attach(Sequencers.flow(TRANSACTION, Transactions.STATUS));

    in.emit(Transactions.Sign.START);
    in.emit(Transactions.Sign.COMMIT);

    assertOutcomes(DIVERGING, STABLE);

  }

  @Test
  void transactionsConflictThenRollbackIsDegraded() {

    final var in =
      attach(Sequencers.flow(TRANSACTION, Transactions.STATUS));

    in.emit(Transactions.Sign.START);      // OPEN                    → DIVERGING
    in.emit(Transactions.Sign.CONFLICT);   // ADVANCE, STATUS DEGRADED → DEGRADED (not a false STABLE)
    in.emit(Transactions.Sign.ROLLBACK);   // CLOSE, STATUS DEGRADED   → DEGRADED (not an orphan)
    in.emit(Transactions.Sign.START);      // OPEN again               → DIVERGING (the span closed)

    assertOutcomes(DIVERGING, DEGRADED, DEGRADED, DIVERGING);

  }

  @Test
  void processesRestartAfterCrashOpensFreshEpisode() {

    final var in =
      attach(Sequencers.flow(Processes.OPERATION, Processes.STATUS));

    in.emit(Processes.Sign.SPAWN);     // OPEN                    → DIVERGING
    in.emit(Processes.Sign.START);     // ADVANCE, no STATUS      → silent
    in.emit(Processes.Sign.CRASH);     // CLOSE, STATUS DEFECTIVE → DEFECTIVE
    in.emit(Processes.Sign.RESTART);   // OPEN                    → DIVERGING
    in.emit(Processes.Sign.START);     // ADVANCE, no STATUS      → silent
    in.emit(Processes.Sign.STOP);      // CLOSE, STATUS STABLE    → STABLE (not an orphan)

    assertOutcomes(DIVERGING, DEFECTIVE, DIVERGING, STABLE);

  }

}
