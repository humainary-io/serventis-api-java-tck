// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.sdk.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.*;
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
/// `SUCCESS`/`FAIL`/`UNKNOWN`, else `null`) is the verdict axis. The published maps read verdicts on
/// outcomes, but that is not checked as a rule: SPEC §5.2 says kind must not decide how a sign is
/// read, so an operation carrying a verdict is a conformant divergence.
///
/// The two empty-ish OUTCOME results are **not** the same: `null` means the sign does not project onto
/// the verdict axis at all — it is not verdict-bearing (an operation/lifecycle sign like `Tasks.START`
/// or an explicit non-verdict result like `Evals.SKIP`); `UNKNOWN` means the sign *is* verdict-bearing
/// but its polarity is unsettled or intentionally indeterminate (`Transactions.CONFLICT`,
/// `Locks.CONTEST`). The consistency checks can't see the exact classification, so every sign of
/// every domain is also pinned, in [#everyPublishedReadingIsPinned].
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

  /// An `OUTCOME` map must read at least one verdict. Which signs may carry one is not tied to
  /// their kind: SPEC §5.2 says kind must not decide how a sign is read, and gives a forced
  /// revocation as an operation that carries a verdict.

  private static void assertOutcome(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Outcomes.Sign > outcome
  ) {

    final var outMap = cast(outcome);

    var hasVerdict = false;

    for (final var sign : signs.getEnumConstants()) {

      if (outMap.apply(sign) != null) {
        hasVerdict = true;
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
        Arguments.of("Processes", Processes.Sign.class, Processes.KIND, Processes.OPERATION),
        Arguments.of("Changes", Changes.Sign.class, Changes.KIND, Changes.OPERATION)
      );

  }

  private static Stream< Arguments > outcomeDomains() {

    return
      Stream.of(
        Arguments.of("Locks", Locks.Sign.class, Locks.OUTCOME),
        Arguments.of("Resources", Resources.Sign.class, Resources.OUTCOME),
        Arguments.of("Leases", Leases.Sign.class, Leases.OUTCOME),
        Arguments.of("Latches", Latches.Sign.class, Latches.OUTCOME),
        Arguments.of("Atomics", Atomics.Sign.class, Atomics.OUTCOME),
        Arguments.of("Tasks", Tasks.Sign.class, Tasks.OUTCOME),
        Arguments.of("Transactions", Transactions.Sign.class, Transactions.OUTCOME),
        Arguments.of("Processes", Processes.Sign.class, Processes.OUTCOME),
        Arguments.of("Probes", Probes.Sign.class, Probes.OUTCOME),
        Arguments.of("Agents", Agents.Sign.class, Agents.OUTCOME),
        Arguments.of("Evals", Evals.Sign.class, Evals.OUTCOME),
        Arguments.of("Changes", Changes.Sign.class, Changes.OUTCOME),
        Arguments.of("Guards", Guards.Sign.class, Guards.OUTCOME)
      );

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
    final SignMap< ?, Outcomes.Sign > outcome
  ) {

    assertOutcome(name, signs, outcome);

  }

  /// Every sign of every domain is pinned, in both maps it publishes, to the reading REGISTRY.md
  /// records for it — not only a sample per domain. Changing one verdict or one bracket role is a
  /// deliberate edit here and in the registry together.

  @Test
  void everyPublishedReadingIsPinned() {

    assertPins(
      Agents.Sign.class, Outcomes.Sign.class, Agents.OUTCOME::apply,
      "OFFER=- PROMISE=- ACCEPT=- FULFILL=SUCCESS RETRACT=- BREACH=FAIL " +
        "INQUIRE=- OBSERVE=- DEPEND=- VALIDATE=-"
    );

    assertPins(
      Atomics.Sign.class, Outcomes.Sign.class, Atomics.OUTCOME::apply,
      "ATTEMPT=- SUCCESS=SUCCESS FAIL=UNKNOWN SPIN=- YIELD=- BACKOFF=- PARK=- " +
        "EXHAUST=FAIL"
    );

    assertPins(
      Changes.Sign.class, Operations.Sign.class, Changes.OPERATION::apply,
      "START=BEGIN APPLY=END FAIL=END REJECT=END REVERT=END PROGRESS=ADVANCE"
    );

    assertPins(
      Changes.Sign.class, Outcomes.Sign.class, Changes.OUTCOME::apply,
      "START=- APPLY=SUCCESS FAIL=FAIL REJECT=FAIL REVERT=FAIL PROGRESS=-"
    );

    assertPins(
      Evals.Sign.class, Outcomes.Sign.class, Evals.OUTCOME::apply,
      "PASS=SUCCESS FAIL=FAIL UNKNOWN=UNKNOWN SKIP=- ERROR=UNKNOWN"
    );

    assertPins(
      Guards.Sign.class, Outcomes.Sign.class, Guards.OUTCOME::apply,
      "ATTEMPT=- CHALLENGE=- GRANT=SUCCESS DENY=FAIL"
    );

    assertPins(
      Latches.Sign.class, Outcomes.Sign.class, Latches.OUTCOME::apply,
      "AWAIT=- ARRIVE=- RELEASE=SUCCESS TIMEOUT=FAIL RESET=- ABANDON=FAIL"
    );

    assertPins(
      Leases.Sign.class, Outcomes.Sign.class, Leases.OUTCOME::apply,
      "ACQUIRE=- DENY=FAIL EXTEND=SUCCESS EXPIRE=FAIL GRANT=SUCCESS PROBE=- " +
        "RELEASE=- RENEW=- REVOKE=-"
    );

    assertPins(
      Locks.Sign.class, Outcomes.Sign.class, Locks.OUTCOME::apply,
      "ATTEMPT=- ACQUIRE=- GRANT=SUCCESS DENY=FAIL TIMEOUT=FAIL RELEASE=- " +
        "UPGRADE=- DOWNGRADE=- CONTEST=UNKNOWN ABANDON=FAIL"
    );

    assertPins(
      Probes.Sign.class, Outcomes.Sign.class, Probes.OUTCOME::apply,
      "CONNECT=- DISCONNECT=- TRANSFER=- PROCESS=- SUCCEED=SUCCESS FAIL=FAIL"
    );

    assertPins(
      Processes.Sign.class, Operations.Sign.class, Processes.OPERATION::apply,
      "SPAWN=BEGIN START=ADVANCE STOP=END FAIL=END CRASH=END KILL=END " +
        "RESTART=BEGIN SUSPEND=ADVANCE RESUME=ADVANCE"
    );

    assertPins(
      Processes.Sign.class, Outcomes.Sign.class, Processes.OUTCOME::apply,
      "SPAWN=- START=- STOP=SUCCESS FAIL=FAIL CRASH=FAIL KILL=- RESTART=- " +
        "SUSPEND=- RESUME=-"
    );

    assertPins(
      Resources.Sign.class, Outcomes.Sign.class, Resources.OUTCOME::apply,
      "ATTEMPT=- ACQUIRE=- GRANT=SUCCESS DENY=FAIL TIMEOUT=FAIL RELEASE=-"
    );

    assertPins(
      Tasks.Sign.class, Outcomes.Sign.class, Tasks.OUTCOME::apply,
      "SUBMIT=- REJECT=FAIL SCHEDULE=- START=- PROGRESS=- SUSPEND=- RESUME=- " +
        "COMPLETE=SUCCESS FAIL=FAIL CANCEL=- TIMEOUT=FAIL"
    );

    assertPins(
      Transactions.Sign.class, Outcomes.Sign.class, Transactions.OUTCOME::apply,
      "START=- PREPARE=- COMMIT=SUCCESS ROLLBACK=FAIL ABORT=FAIL EXPIRE=FAIL " +
        "CONFLICT=UNKNOWN COMPENSATE=-"
    );

  }

  /// Asserts `map` gives each sign the value `pins` names (`-` for abstention), and that `pins`
  /// names every sign, so a sign added to the vocabulary cannot go unpinned.

  private static < S extends Enum< S >, V extends Enum< V > > void assertPins(
    final Class< S > signs,
    final Class< V > values,
    final Function< S, V > map,
    final String pins
  ) {

    final var pinned = EnumSet.noneOf(signs);

    for (final var pin : pins.split(" ")) {

      final var parts = pin.split("=");
      final var sign = Enum.valueOf(signs, parts[0]);
      final V expected = parts[1].equals("-") ? null : Enum.valueOf(values, parts[1]);

      assertEquals(
        expected,
        map.apply(sign),
        () -> signs.getEnclosingClass().getSimpleName() + "." + sign
      );

      pinned.add(sign);

    }

    assertEquals(
      EnumSet.allOf(signs),
      pinned,
      () -> signs.getEnclosingClass().getSimpleName() + ": every sign is pinned"
    );

  }

  @Test
  void registryIsComplete() {

    assertEquals(
      Vocabularies.publishing("OPERATION"),
      Vocabularies.enclosing(operationDomains()),
      "every vocabulary publishing OPERATION is checked here, and only those"
    );

    assertEquals(
      Vocabularies.publishing("OUTCOME"),
      Vocabularies.enclosing(outcomeDomains()),
      "every vocabulary publishing OUTCOME is checked here, and only those"
    );

  }

}
