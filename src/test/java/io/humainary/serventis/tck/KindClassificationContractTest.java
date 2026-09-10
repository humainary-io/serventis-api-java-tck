// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.opt.data.*;
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
import static org.junit.jupiter.api.Assertions.*;

/// Cross-domain check on the canonical `KIND` classification convention.
///
/// Each outcome-oriented Serventis domain publishes a `public static final SignMap<Sign, Kind> KIND`
/// — the *act/result* classification of its sign set, the structural twin of the partial,
/// outcome-reading `STATUS` map. Where `STATUS` is **partial** (operations abstain), `KIND` is
/// **total** (every sign maps to [Kind#OPERATION] or [Kind#OUTCOME]). This test pins, per domain,
/// the exact partition (so a flipped classification fails), checks totality, and pins the **seam**:
/// the signs that are `OPERATION` yet carry a `STATUS` reading (act/result is deliberately
/// independent of the status reading — see [Kind]).
///
/// The registry mirrors [StatusMappingContractTest]; the same 19 domains classify their signs.
/// @author William David Louth
/// @since 3.0

final class KindClassificationContractTest {

  /// Asserts `kind` is total (never abstains) and that the signs it reads as `OUTCOME` are exactly
  /// `outcomes` — every other sign must therefore be `OPERATION`. Catches a flipped classification.

  private static < S extends Enum< S > > void assertPartition(
    final Class< S > signs,
    final SignMap< ?, Kind > kind,
    final Set< S > outcomes
  ) {

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Kind > map =
      (Function) kind;

    for (final S sign : signs.getEnumConstants()) {

      final var resolved =
        map.apply(sign);

      // Totality: every sign maps to a kind; none abstains.
      assertNotNull(
        resolved,
        () -> signs.getSimpleName() + "." + sign.name() + " must have a Kind"
      );

      assertEquals(
        outcomes.contains(sign) ? OUTCOME:OPERATION,
        resolved,
        () -> signs.getSimpleName() + "." + sign.name()
      );

    }

  }

  /// Asserts the **seam** for a domain — the set of signs that are `OPERATION` yet carry a non-null
  /// `STATUS` reading — equals `expected`. Makes the act/result-vs-status divergence explicit, and
  /// fails on any new, unratified seam.

  private static < S extends Enum< S > > void assertSeam(
    final Class< S > signs,
    final SignMap< ?, Kind > kind,
    final SignMap< ?, Statuses.Sign > status,
    final Set< S > expected
  ) {

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Kind > kindMap =
      (Function) kind;

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Statuses.Sign > statusMap =
      (Function) status;

    final Set< S > seams =
      EnumSet.noneOf(signs);

    for (final S sign : signs.getEnumConstants()) {
      if (kindMap.apply(sign)==OPERATION && statusMap.apply(sign)!=null) {
        seams.add(sign);
      }
    }

    assertEquals(
      expected,
      seams,
      () -> signs.getSimpleName() + " seam (OPERATION signs with a STATUS reading)"
    );

  }

  /// Every domain that publishes a canonical `KIND` map, paired with its sign enum.

  private static Stream< Arguments > kindMaps() {

    return
      Stream.of(
        Arguments.of("Resources", Resources.Sign.class, Resources.KIND),
        Arguments.of("Leases", Leases.Sign.class, Leases.KIND),
        Arguments.of("Atomics", Atomics.Sign.class, Atomics.KIND),
        Arguments.of("Latches", Latches.Sign.class, Latches.KIND),
        Arguments.of("Locks", Locks.Sign.class, Locks.KIND),
        Arguments.of("Tasks", Tasks.Sign.class, Tasks.KIND),
        Arguments.of("Transactions", Transactions.Sign.class, Transactions.KIND),
        Arguments.of("Processes", Processes.Sign.class, Processes.KIND),
        Arguments.of("Timers", Timers.Sign.class, Timers.KIND),
        Arguments.of("Services", Services.Sign.class, Services.KIND),
        Arguments.of("Caches", Caches.Sign.class, Caches.KIND),
        Arguments.of("Agents", Agents.Sign.class, Agents.KIND),
        Arguments.of("Evals", Evals.Sign.class, Evals.KIND),
        Arguments.of("Probes", Probes.Sign.class, Probes.KIND),
        Arguments.of("Logs", Logs.Sign.class, Logs.KIND),
        Arguments.of("Flows", Flows.Sign.class, Flows.KIND),
        Arguments.of("Breakers", Breakers.Sign.class, Breakers.KIND),
        Arguments.of("Valves", Valves.Sign.class, Valves.KIND),
        Arguments.of("Routers", Routers.Sign.class, Routers.KIND)
      );

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("kindMaps")
  void kindMapIsTotal(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Kind > kind
  ) {

    assertNotNull(
      kind,
      () -> name + " must publish a KIND map"
    );

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Kind > map =
      (Function) kind;

    var hasOutcome = false;

    for (final var sign : signs.getEnumConstants()) {

      final var resolved =
        map.apply(sign);

      assertNotNull(
        resolved,
        () -> name + "." + sign.name() + " must have a Kind (KIND is total)"
      );

      if (resolved==OUTCOME) {
        hasOutcome = true;
      }

    }

    // Every outcome-oriented domain reads at least one sign as an OUTCOME (operations alone never
    // ascend); pure-operation families are out of scope for KIND.
    assertTrue(
      hasOutcome,
      () -> name + " KIND must classify at least one sign as OUTCOME"
    );

  }

  /// Pins the exact OPERATION/OUTCOME partition for every domain (the ratified classification).

  @Test
  void partitionsAreExact() {

    assertPartition(
      Resources.Sign.class, Resources.KIND,
      Set.of(Resources.Sign.GRANT, Resources.Sign.DENY, Resources.Sign.TIMEOUT)
    );

    assertPartition(
      Leases.Sign.class, Leases.KIND,
      Set.of(Leases.Sign.GRANT, Leases.Sign.DENY, Leases.Sign.EXTEND, Leases.Sign.EXPIRE)
    );

    assertPartition(
      Atomics.Sign.class, Atomics.KIND,
      Set.of(Atomics.Sign.SUCCESS, Atomics.Sign.FAIL, Atomics.Sign.EXHAUST)
    );

    assertPartition(
      Latches.Sign.class, Latches.KIND,
      Set.of(Latches.Sign.RELEASE, Latches.Sign.TIMEOUT, Latches.Sign.ABANDON)
    );

    assertPartition(
      Locks.Sign.class, Locks.KIND,
      Set.of(
        Locks.Sign.GRANT, Locks.Sign.DENY, Locks.Sign.TIMEOUT,
        Locks.Sign.CONTEST, Locks.Sign.ABANDON
      )
    );

    assertPartition(
      Tasks.Sign.class, Tasks.KIND,
      Set.of(Tasks.Sign.REJECT, Tasks.Sign.COMPLETE, Tasks.Sign.FAIL, Tasks.Sign.TIMEOUT)
    );

    assertPartition(
      Transactions.Sign.class, Transactions.KIND,
      Set.of(
        Transactions.Sign.COMMIT, Transactions.Sign.ROLLBACK, Transactions.Sign.ABORT,
        Transactions.Sign.EXPIRE, Transactions.Sign.CONFLICT
      )
    );

    assertPartition(
      Processes.Sign.class, Processes.KIND,
      Set.of(Processes.Sign.STOP, Processes.Sign.FAIL, Processes.Sign.CRASH)
    );

    assertPartition(
      Timers.Sign.class, Timers.KIND,
      Set.of(Timers.Sign.MEET, Timers.Sign.MISS)
    );

    assertPartition(
      Services.Sign.class, Services.KIND,
      Set.of(
        Services.Sign.SUCCESS, Services.Sign.FAIL, Services.Sign.EXPIRE,
        Services.Sign.REJECT, Services.Sign.DISCONNECT
      )
    );

    assertPartition(
      Caches.Sign.class, Caches.KIND,
      Set.of(Caches.Sign.HIT, Caches.Sign.MISS, Caches.Sign.EVICT, Caches.Sign.EXPIRE)
    );

    assertPartition(
      Agents.Sign.class, Agents.KIND,
      Set.of(Agents.Sign.FULFILL, Agents.Sign.BREACH)
    );

    assertPartition(
      Evals.Sign.class, Evals.KIND,
      Set.of(
        Evals.Sign.PASS, Evals.Sign.FAIL, Evals.Sign.UNKNOWN, Evals.Sign.SKIP, Evals.Sign.ERROR
      )
    );

    assertPartition(
      Probes.Sign.class, Probes.KIND,
      Set.of(Probes.Sign.SUCCEED, Probes.Sign.FAIL)
    );

    assertPartition(
      Logs.Sign.class, Logs.KIND,
      Set.of(Logs.Sign.INFO, Logs.Sign.WARNING, Logs.Sign.SEVERE, Logs.Sign.DEBUG)
    );

    assertPartition(
      Flows.Sign.class, Flows.KIND,
      Set.of(Flows.Sign.SUCCESS, Flows.Sign.FAIL)
    );

    assertPartition(
      Breakers.Sign.class, Breakers.KIND,
      Set.of(
        Breakers.Sign.CLOSE, Breakers.Sign.OPEN, Breakers.Sign.HALF_OPEN, Breakers.Sign.TRIP
      )
    );

    assertPartition(
      Valves.Sign.class, Valves.KIND,
      Set.of(Valves.Sign.PASS, Valves.Sign.DENY)
    );

    assertPartition(
      Routers.Sign.class, Routers.KIND,
      Set.of(Routers.Sign.CORRUPT, Routers.Sign.REORDER)
    );

  }

  /// Guards the registry size so adding a domain forces a deliberate decision here.

  @Test
  void registryIsComplete() {

    assertEquals(
      19,
      kindMaps().count()
    );

  }

  /// Pins the seam — `OPERATION` signs that nonetheless carry a `STATUS` reading — across all 19
  /// domains. Seven signs diverge by design; everything else must agree (operations abstain in
  /// `STATUS`, outcomes are read).

  @Test
  void seamIsDocumented() {

    assertSeam(Resources.Sign.class, Resources.KIND, Resources.STATUS, Set.of());
    assertSeam(Leases.Sign.class, Leases.KIND, Leases.STATUS, Set.of(Leases.Sign.REVOKE));
    assertSeam(Atomics.Sign.class, Atomics.KIND, Atomics.STATUS, Set.of(Atomics.Sign.PARK));
    assertSeam(Latches.Sign.class, Latches.KIND, Latches.STATUS, Set.of());
    assertSeam(Locks.Sign.class, Locks.KIND, Locks.STATUS, Set.of());
    assertSeam(Tasks.Sign.class, Tasks.KIND, Tasks.STATUS, Set.of());
    assertSeam(Transactions.Sign.class, Transactions.KIND, Transactions.STATUS, Set.of());
    assertSeam(Processes.Sign.class, Processes.KIND, Processes.STATUS, Set.of(Processes.Sign.KILL));
    assertSeam(Timers.Sign.class, Timers.KIND, Timers.STATUS, Set.of());
    assertSeam(Services.Sign.class, Services.KIND, Services.STATUS, Set.of(Services.Sign.RECOURSE));
    assertSeam(Caches.Sign.class, Caches.KIND, Caches.STATUS, Set.of());
    assertSeam(Agents.Sign.class, Agents.KIND, Agents.STATUS, Set.of());
    assertSeam(Evals.Sign.class, Evals.KIND, Evals.STATUS, Set.of());
    assertSeam(Probes.Sign.class, Probes.KIND, Probes.STATUS, Set.of());
    assertSeam(Logs.Sign.class, Logs.KIND, Logs.STATUS, Set.of());
    assertSeam(Flows.Sign.class, Flows.KIND, Flows.STATUS, Set.of());
    assertSeam(Breakers.Sign.class, Breakers.KIND, Breakers.STATUS, Set.of());
    assertSeam(Valves.Sign.class, Valves.KIND, Valves.STATUS, Set.of(Valves.Sign.DROP));
    assertSeam(
      Routers.Sign.class, Routers.KIND, Routers.STATUS,
      Set.of(Routers.Sign.FORWARD, Routers.Sign.DROP)
    );

  }

}
