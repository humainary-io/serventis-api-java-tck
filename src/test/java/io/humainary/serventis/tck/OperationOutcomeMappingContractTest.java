// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.sdk.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.function.*;
import java.util.stream.*;

import static io.humainary.serventis.api.Serventis.Kind.*;
import static io.humainary.serventis.sdk.Operations.Sign.*;
import static io.humainary.serventis.sdk.Outcomes.Sign.*;
import static org.junit.jupiter.api.Assertions.*;

/// Convention checks on protocol-domain `OPERATION` bracket classifications and domain `OUTCOME`
/// verdict maps (see `SEQUENCERS.md`). An API may publish `OUTCOME` without defining a protocol
/// span, so the two map families have independent registries.
///
/// `OPERATION` (sign → [Operations.Sign] `BEGIN`/`ADVANCE`/`END`) is the **episode structure** of each
/// sign — *total* and *cross-kind*: a terminal outcome is `END`, a mid-span outcome is `ADVANCE`. The
/// one kind-aligned invariant is that an opener is always an act: `BEGIN ⟹ KIND=OPERATION`. A
/// well-formed map has at least one `BEGIN` and one `END`. `OUTCOME` (sign → [Outcomes.Sign]
/// `SUCCESS`/`FAIL`/`UNKNOWN`, else `null`) is the verdict axis and stays `KIND`-restricted: anything it
/// reads is `KIND=OUTCOME`.
///
/// The two empty-ish OUTCOME results are **not** the same: `null` means the sign does not project onto
/// the verdict axis at all — it is not verdict-bearing (an operation/lifecycle sign like `Tasks.START`
/// or an explicit non-verdict result like `Evals.SKIP`), so it is *exempt* from the `KIND=OUTCOME`
/// check; `UNKNOWN` means the sign *is* verdict-bearing but its polarity is unsettled or intentionally
/// indeterminate (`Transactions.CONFLICT`, `Locks.CONTEST`), so it still *must* be `KIND=OUTCOME`.
/// The consistency check can't see the exact classification, so each domain is also pinned.
/// @author William David Louth
/// @since 3.0

final class OperationOutcomeMappingContractTest {

  private static void assertOperation(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Kind > kind,
    final SignMap< ?, Operations.Sign > operation
  ) {

    final var kindMap = cast(kind);
    final var opMap = cast(operation);

    var hasBegin = false;
    var hasEnd = false;

    for (final var sign : signs.getEnumConstants()) {

      final var op = opMap.apply(sign);

      assertNotNull(
        op,
        () -> name + "." + sign.name() + " must have an episode role (OPERATION is total)"
      );

      if (op==BEGIN) {

        hasBegin = true;

        assertEquals(
          OPERATION,
          kindMap.apply(sign),
          () -> name + "." + sign.name() + " opens a span but is not KIND=OPERATION"
        );

      }

      if (op==END) {
        hasEnd = true;
      }

    }

    assertTrue(hasBegin, () -> name + " OPERATION must open a span (a BEGIN)");
    assertTrue(hasEnd, () -> name + " OPERATION must close a span (an END)");

  }

  private static void assertOutcome(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Kind > kind,
    final SignMap< ?, Outcomes.Sign > outcome
  ) {

    final var kindMap = cast(kind);
    final var outMap = cast(outcome);

    var hasVerdict = false;

    for (final var sign : signs.getEnumConstants()) {

      final var verdict = outMap.apply(sign);

      if (verdict!=null) {

        hasVerdict = true;

        assertEquals(
          OUTCOME,
          kindMap.apply(sign),
          () -> name + "." + sign.name() + " carries a verdict but is not KIND=OUTCOME"
        );

      }

    }

    assertTrue(hasVerdict, () -> name + " OUTCOME must read at least one verdict");

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static < T > Function< Enum< ? >, T > cast(
    final SignMap< ?, T > map
  ) {

    return
      (Function) map;

  }

  private static Stream< Arguments > operationDomains() {

    return
      Stream.of(
        Arguments.of("Locks", Locks.Sign.class, Locks.KIND, Locks.OPERATION),
        Arguments.of("Resources", Resources.Sign.class, Resources.KIND, Resources.OPERATION),
        Arguments.of("Leases", Leases.Sign.class, Leases.KIND, Leases.OPERATION),
        Arguments.of("Latches", Latches.Sign.class, Latches.KIND, Latches.OPERATION),
        Arguments.of("Atomics", Atomics.Sign.class, Atomics.KIND, Atomics.OPERATION),
        Arguments.of("Tasks", Tasks.Sign.class, Tasks.KIND, Tasks.OPERATION),
        Arguments.of("Transactions", Transactions.Sign.class, Transactions.KIND, Transactions.OPERATION),
        Arguments.of("Processes", Processes.Sign.class, Processes.KIND, Processes.OPERATION),
        Arguments.of("Probes", Probes.Sign.class, Probes.KIND, Probes.OPERATION),
        Arguments.of("Agents", Agents.Sign.class, Agents.KIND, Agents.OPERATION)
      );

  }

  private static Stream< Arguments > outcomeDomains() {

    return
      Stream.of(
        Arguments.of("Locks", Locks.Sign.class, Locks.KIND, Locks.OUTCOME),
        Arguments.of("Resources", Resources.Sign.class, Resources.KIND, Resources.OUTCOME),
        Arguments.of("Leases", Leases.Sign.class, Leases.KIND, Leases.OUTCOME),
        Arguments.of("Latches", Latches.Sign.class, Latches.KIND, Latches.OUTCOME),
        Arguments.of("Atomics", Atomics.Sign.class, Atomics.KIND, Atomics.OUTCOME),
        Arguments.of("Tasks", Tasks.Sign.class, Tasks.KIND, Tasks.OUTCOME),
        Arguments.of("Transactions", Transactions.Sign.class, Transactions.KIND, Transactions.OUTCOME),
        Arguments.of("Processes", Processes.Sign.class, Processes.KIND, Processes.OUTCOME),
        Arguments.of("Probes", Probes.Sign.class, Probes.KIND, Probes.OUTCOME),
        Arguments.of("Agents", Agents.Sign.class, Agents.KIND, Agents.OUTCOME),
        Arguments.of("Evals", Evals.Sign.class, Evals.KIND, Evals.OUTCOME)
      );

  }

  @Test
  void agentsPins() {

    assertEquals(BEGIN, Agents.OPERATION.apply(Agents.Sign.OFFER));
    assertEquals(ADVANCE, Agents.OPERATION.apply(Agents.Sign.PROMISE));
    assertEquals(END, Agents.OPERATION.apply(Agents.Sign.RETRACT));
    assertEquals(END, Agents.OPERATION.apply(Agents.Sign.FULFILL));

    assertEquals(SUCCESS, Agents.OUTCOME.apply(Agents.Sign.FULFILL));
    assertEquals(FAIL, Agents.OUTCOME.apply(Agents.Sign.BREACH));

  }

  @Test
  void atomicsPins() {

    assertEquals(BEGIN, Atomics.OPERATION.apply(Atomics.Sign.ATTEMPT));
    assertEquals(ADVANCE, Atomics.OPERATION.apply(Atomics.Sign.SPIN));
    assertEquals(ADVANCE, Atomics.OPERATION.apply(Atomics.Sign.FAIL));   // mid-episode retry, NOT terminal
    assertEquals(END, Atomics.OPERATION.apply(Atomics.Sign.SUCCESS));
    assertEquals(END, Atomics.OPERATION.apply(Atomics.Sign.EXHAUST));

    assertEquals(SUCCESS, Atomics.OUTCOME.apply(Atomics.Sign.SUCCESS));
    assertEquals(UNKNOWN, Atomics.OUTCOME.apply(Atomics.Sign.FAIL));    // contention, will retry — not a settled defeat
    assertEquals(FAIL, Atomics.OUTCOME.apply(Atomics.Sign.EXHAUST));

  }

  // ===== exact pins — catch a wrong episode role or verdict polarity =====

  @Test
  void latchesPins() {

    assertEquals(BEGIN, Latches.OPERATION.apply(Latches.Sign.AWAIT));
    assertEquals(ADVANCE, Latches.OPERATION.apply(Latches.Sign.ARRIVE));
    assertEquals(END, Latches.OPERATION.apply(Latches.Sign.RELEASE));

    assertEquals(SUCCESS, Latches.OUTCOME.apply(Latches.Sign.RELEASE));
    assertEquals(FAIL, Latches.OUTCOME.apply(Latches.Sign.TIMEOUT));

  }

  @Test
  void leasesPins() {

    assertEquals(BEGIN, Leases.OPERATION.apply(Leases.Sign.ACQUIRE));
    assertEquals(ADVANCE, Leases.OPERATION.apply(Leases.Sign.EXTEND));
    assertEquals(END, Leases.OPERATION.apply(Leases.Sign.REVOKE));
    assertEquals(END, Leases.OPERATION.apply(Leases.Sign.EXPIRE));

    assertEquals(SUCCESS, Leases.OUTCOME.apply(Leases.Sign.EXTEND));
    assertEquals(FAIL, Leases.OUTCOME.apply(Leases.Sign.EXPIRE));

  }

  @Test
  void locksPins() {

    assertEquals(BEGIN, Locks.OPERATION.apply(Locks.Sign.ATTEMPT));
    assertEquals(BEGIN, Locks.OPERATION.apply(Locks.Sign.ACQUIRE));
    assertEquals(ADVANCE, Locks.OPERATION.apply(Locks.Sign.GRANT));
    assertEquals(ADVANCE, Locks.OPERATION.apply(Locks.Sign.CONTEST));
    assertEquals(END, Locks.OPERATION.apply(Locks.Sign.RELEASE));
    assertEquals(END, Locks.OPERATION.apply(Locks.Sign.DENY));
    assertEquals(END, Locks.OPERATION.apply(Locks.Sign.ABANDON));

    assertEquals(SUCCESS, Locks.OUTCOME.apply(Locks.Sign.GRANT));
    assertEquals(FAIL, Locks.OUTCOME.apply(Locks.Sign.DENY));
    assertEquals(UNKNOWN, Locks.OUTCOME.apply(Locks.Sign.CONTEST));

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("operationDomains")
  void operationMapsAreWellFormed(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Kind > kind,
    final SignMap< ?, Operations.Sign > operation
  ) {

    assertOperation(name, signs, kind, operation);

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("outcomeDomains")
  void outcomeMapsAreWellFormed(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Kind > kind,
    final SignMap< ?, Outcomes.Sign > outcome
  ) {

    assertOutcome(name, signs, kind, outcome);

  }

  @Test
  void probesPins() {

    assertEquals(BEGIN, Probes.OPERATION.apply(Probes.Sign.CONNECT));
    assertEquals(ADVANCE, Probes.OPERATION.apply(Probes.Sign.SUCCEED));
    assertEquals(END, Probes.OPERATION.apply(Probes.Sign.DISCONNECT));

    assertEquals(SUCCESS, Probes.OUTCOME.apply(Probes.Sign.SUCCEED));

  }

  @Test
  void processesPins() {

    assertEquals(BEGIN, Processes.OPERATION.apply(Processes.Sign.SPAWN));
    assertEquals(ADVANCE, Processes.OPERATION.apply(Processes.Sign.SUSPEND));
    assertEquals(END, Processes.OPERATION.apply(Processes.Sign.KILL));
    assertEquals(END, Processes.OPERATION.apply(Processes.Sign.CRASH));

    assertEquals(SUCCESS, Processes.OUTCOME.apply(Processes.Sign.STOP));

  }

  @Test
  void registryIsComplete() {

    assertEquals(10, operationDomains().count());
    assertEquals(11, outcomeDomains().count());

  }

  @Test
  void resourcesPins() {

    assertEquals(BEGIN, Resources.OPERATION.apply(Resources.Sign.ATTEMPT));
    assertEquals(ADVANCE, Resources.OPERATION.apply(Resources.Sign.GRANT));
    assertEquals(END, Resources.OPERATION.apply(Resources.Sign.RELEASE));
    assertEquals(END, Resources.OPERATION.apply(Resources.Sign.TIMEOUT));

    assertEquals(SUCCESS, Resources.OUTCOME.apply(Resources.Sign.GRANT));

  }

  @Test
  void tasksPins() {

    assertEquals(BEGIN, Tasks.OPERATION.apply(Tasks.Sign.SUBMIT));
    assertEquals(ADVANCE, Tasks.OPERATION.apply(Tasks.Sign.PROGRESS));
    assertEquals(END, Tasks.OPERATION.apply(Tasks.Sign.COMPLETE));
    assertEquals(END, Tasks.OPERATION.apply(Tasks.Sign.CANCEL));

    assertEquals(SUCCESS, Tasks.OUTCOME.apply(Tasks.Sign.COMPLETE));

  }

  @Test
  void transactionsPins() {

    assertEquals(BEGIN, Transactions.OPERATION.apply(Transactions.Sign.START));
    assertEquals(ADVANCE, Transactions.OPERATION.apply(Transactions.Sign.PREPARE));
    assertEquals(END, Transactions.OPERATION.apply(Transactions.Sign.COMMIT));
    assertEquals(END, Transactions.OPERATION.apply(Transactions.Sign.ROLLBACK));

    assertEquals(SUCCESS, Transactions.OUTCOME.apply(Transactions.Sign.COMMIT));
    assertEquals(UNKNOWN, Transactions.OUTCOME.apply(Transactions.Sign.CONFLICT));

  }

}
