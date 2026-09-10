// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.opt.data.*;
import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.sdk.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

/// Cross-domain check on the canonical `STATUS` translation convention.
///
/// Each outcome-oriented Serventis domain publishes a `public static final
/// SignMap<Sign, Statuses.Sign> STATUS` — the *immediate interpretant* of its ascent into
/// [Statuses]. This test pins, per domain, the exact sign→status mapping (so a swapped
/// success/failure reading or an incorrect abstention fails), checks each map is well-formed
/// (total + polarity), and runs a real emission through [Scorecards] for both a bare-`Sign` and a
/// `Signal`-emitting domain to prove end-to-end usability.
///
/// Movement/sequencing, conversational, and dimension-essential domains deliberately publish no
/// `STATUS` (their ascent is the sequencing operator or a dimension-aware ballot) and are absent
/// from the registry below by design.
/// @author William David Louth
/// @since 3.0

final class StatusMappingContractTest {

  /// Asserts `status` maps exactly `expected` for every sign — any sign absent from `expected` must
  /// abstain (`null`). Catches swapped readings and incorrect abstentions, not just polarity.

  private static < S extends Enum< S > > void assertExact(
    final Class< S > signs,
    final SignMap< ?, Statuses.Sign > status,
    final Map< S, Statuses.Sign > expected
  ) {

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Statuses.Sign > ballot =
      (Function) status;

    for (final S sign : signs.getEnumConstants()) {

      assertEquals(
        expected.get(sign),
        ballot.apply(sign),
        () -> signs.getSimpleName() + "." + sign.name()
      );

    }

  }

  /// Every domain that publishes a canonical `STATUS` map, paired with its sign enum.

  private static Stream< Arguments > statusMaps() {

    return
      Stream.of(
        Arguments.of("Resources", Resources.Sign.class, Resources.STATUS),
        Arguments.of("Leases", Leases.Sign.class, Leases.STATUS),
        Arguments.of("Atomics", Atomics.Sign.class, Atomics.STATUS),
        Arguments.of("Latches", Latches.Sign.class, Latches.STATUS),
        Arguments.of("Locks", Locks.Sign.class, Locks.STATUS),
        Arguments.of("Tasks", Tasks.Sign.class, Tasks.STATUS),
        Arguments.of("Transactions", Transactions.Sign.class, Transactions.STATUS),
        Arguments.of("Processes", Processes.Sign.class, Processes.STATUS),
        Arguments.of("Timers", Timers.Sign.class, Timers.STATUS),
        Arguments.of("Services", Services.Sign.class, Services.STATUS),
        Arguments.of("Caches", Caches.Sign.class, Caches.STATUS),
        Arguments.of("Agents", Agents.Sign.class, Agents.STATUS),
        Arguments.of("Evals", Evals.Sign.class, Evals.STATUS),
        Arguments.of("Probes", Probes.Sign.class, Probes.STATUS),
        Arguments.of("Logs", Logs.Sign.class, Logs.STATUS),
        Arguments.of("Flows", Flows.Sign.class, Flows.STATUS),
        Arguments.of("Breakers", Breakers.Sign.class, Breakers.STATUS),
        Arguments.of("Valves", Valves.Sign.class, Valves.STATUS),
        Arguments.of("Routers", Routers.Sign.class, Routers.STATUS)
      );

  }

  /// Pins the exact canonical mapping for every domain (the ratified immediate interpretant).

  @Test
  void mappingsAreExact() {

    assertExact(
      Resources.Sign.class, Resources.STATUS,
      Map.of(
        Resources.Sign.GRANT, STABLE,
        Resources.Sign.DENY, DEGRADED,
        Resources.Sign.TIMEOUT, DEGRADED
      )
    );

    assertExact(
      Leases.Sign.class, Leases.STATUS,
      Map.of(
        Leases.Sign.GRANT, STABLE,
        Leases.Sign.EXTEND, STABLE,
        Leases.Sign.DENY, DEGRADED,
        Leases.Sign.EXPIRE, DEGRADED,
        Leases.Sign.REVOKE, DEFECTIVE
      )
    );

    assertExact(
      Atomics.Sign.class, Atomics.STATUS,
      Map.of(
        Atomics.Sign.SUCCESS, STABLE,
        Atomics.Sign.FAIL, DEGRADED,
        Atomics.Sign.PARK, DEGRADED,
        Atomics.Sign.EXHAUST, DEFECTIVE
      )
    );

    assertExact(
      Latches.Sign.class, Latches.STATUS,
      Map.of(
        Latches.Sign.RELEASE, STABLE,
        Latches.Sign.TIMEOUT, DEGRADED,
        Latches.Sign.ABANDON, DEFECTIVE
      )
    );

    assertExact(
      Locks.Sign.class, Locks.STATUS,
      Map.of(
        Locks.Sign.GRANT, STABLE,
        Locks.Sign.DENY, DEGRADED,
        Locks.Sign.TIMEOUT, DEGRADED,
        Locks.Sign.CONTEST, DEGRADED,
        Locks.Sign.ABANDON, DEFECTIVE
      )
    );

    assertExact(
      Tasks.Sign.class, Tasks.STATUS,
      Map.of(
        Tasks.Sign.COMPLETE, STABLE,
        Tasks.Sign.FAIL, DEGRADED,
        Tasks.Sign.REJECT, DEGRADED,
        Tasks.Sign.TIMEOUT, DEGRADED
      )
    );

    assertExact(
      Transactions.Sign.class, Transactions.STATUS,
      Map.of(
        Transactions.Sign.COMMIT, STABLE,
        Transactions.Sign.ROLLBACK, DEGRADED,
        Transactions.Sign.ABORT, DEFECTIVE,
        Transactions.Sign.EXPIRE, DEGRADED,
        Transactions.Sign.CONFLICT, DEGRADED
      )
    );

    assertExact(
      Processes.Sign.class, Processes.STATUS,
      Map.of(
        Processes.Sign.STOP, STABLE,
        Processes.Sign.FAIL, DEGRADED,
        Processes.Sign.KILL, DEGRADED,
        Processes.Sign.CRASH, DEFECTIVE
      )
    );

    assertExact(
      Timers.Sign.class, Timers.STATUS,
      Map.of(
        Timers.Sign.MEET, STABLE,
        Timers.Sign.MISS, DEGRADED
      )
    );

    assertExact(
      Services.Sign.class, Services.STATUS,
      Map.of(
        Services.Sign.SUCCESS, STABLE,
        Services.Sign.FAIL, DEGRADED,
        Services.Sign.DISCONNECT, DEGRADED,
        Services.Sign.REJECT, DEGRADED,
        Services.Sign.EXPIRE, DEGRADED,
        Services.Sign.RECOURSE, DEGRADED
      )
    );

    assertExact(
      Caches.Sign.class, Caches.STATUS,
      Map.of(
        Caches.Sign.HIT, STABLE,
        Caches.Sign.MISS, DEGRADED
      )
    );

    assertExact(
      Agents.Sign.class, Agents.STATUS,
      Map.of(
        Agents.Sign.FULFILL, STABLE,
        Agents.Sign.BREACH, DEGRADED
      )
    );

    assertExact(
      Evals.Sign.class, Evals.STATUS,
      Map.of(
        Evals.Sign.PASS, STABLE,
        Evals.Sign.FAIL, DEGRADED
      )
    );

    assertExact(
      Probes.Sign.class, Probes.STATUS,
      Map.of(
        Probes.Sign.SUCCEED, STABLE,
        Probes.Sign.FAIL, DEGRADED
      )
    );

    assertExact(
      Logs.Sign.class, Logs.STATUS,
      Map.of(
        Logs.Sign.INFO, STABLE,
        Logs.Sign.WARNING, DEGRADED,
        Logs.Sign.SEVERE, DEFECTIVE
      )
    );

    assertExact(
      Flows.Sign.class, Flows.STATUS,
      Map.of(
        Flows.Sign.SUCCESS, STABLE,
        Flows.Sign.FAIL, DEGRADED
      )
    );

    assertExact(
      Breakers.Sign.class, Breakers.STATUS,
      Map.of(
        Breakers.Sign.CLOSE, STABLE,
        Breakers.Sign.OPEN, DEFECTIVE,
        Breakers.Sign.TRIP, DEGRADED
      )
    );

    assertExact(
      Valves.Sign.class, Valves.STATUS,
      Map.of(
        Valves.Sign.PASS, STABLE,
        Valves.Sign.DENY, DEGRADED,
        Valves.Sign.DROP, DEFECTIVE
      )
    );

    assertExact(
      Routers.Sign.class, Routers.STATUS,
      Map.of(
        Routers.Sign.FORWARD, STABLE,
        Routers.Sign.DROP, DEGRADED,
        Routers.Sign.CORRUPT, DEFECTIVE
      )
    );

  }

  /// Guards the registry size so adding a domain forces a deliberate decision here.

  @Test
  void registryIsComplete() {

    assertEquals(
      19,
      statusMaps().count()
    );

  }

  /// A bare-`Sign` domain feeds its `STATUS` straight into `Scorecards.flow` and produces a status.

  @Test
  void scorecardConsumesBareSignStatus() {

    final var circuit =
      cortex().circuit();

    try {

      final var statuses =
        circuit.conduit(Statuses.Signal.class);

      final var captures =
        CaptureBuffer.of(circuit, statuses, 16);

      final Pool< Pipe< Resources.Sign > > scored =
        statuses.pool(Scorecards.flow(Resources.STATUS));

      scored.get(cortex().name("res"))
        .emit(Resources.Sign.GRANT);

      final var out =
        captures
          .drainEmissions()
          .toList();
      captures.close();

      assertFalse(out.isEmpty());
      assertEquals(STABLE, out.getLast().sign());

    } finally {

      circuit.closeAwait();

    }

  }

  /// A `Signal`-emitting domain projects the sign (`STATUS.compose(Signal::sign)`) into the ballot.

  @Test
  void scorecardConsumesSignalStatusViaProjection() {

    final var circuit =
      cortex().circuit();

    try {

      final var statuses =
        circuit.conduit(Statuses.Signal.class);

      final var captures =
        CaptureBuffer.of(circuit, statuses, 16);

      final Pool< Pipe< Services.Signal > > scored =
        statuses.pool(
          Scorecards.flow(Services.STATUS.compose(Services.Signal::sign))
        );

      scored.get(cortex().name("svc"))
        .emit(new Services.Signal(Services.Sign.SUCCESS, Services.Dimension.CALLEE));

      final var out =
        captures
          .drainEmissions()
          .toList();
      captures.close();

      assertFalse(out.isEmpty());
      assertEquals(STABLE, out.getLast().sign());

    } finally {

      circuit.closeAwait();

    }

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statusMaps")
  void statusMapIsWellFormed(
    final String name,
    final Class< ? extends Enum< ? > > signs,
    final SignMap< ?, Statuses.Sign > status
  ) {

    assertNotNull(
      status,
      () -> name + " must publish a STATUS map"
    );

    @SuppressWarnings({"unchecked", "rawtypes"}) final Function< Enum< ? >, Statuses.Sign > ballot =
      (Function) status;

    var healthy = false;
    var unhealthy = false;

    for (final var sign : signs.getEnumConstants()) {

      // Totality: a SignMap is total by construction — apply must never throw for any sign.
      final var resolved =
        ballot.apply(sign);

      if (resolved==STABLE) {
        healthy = true;
      } else if (resolved!=null) {
        unhealthy = true;
      }

    }

    assertTrue(
      healthy,
      () -> name + " STATUS must read at least one sign as STABLE"
    );

    assertTrue(
      unhealthy,
      () -> name + " STATUS must read at least one sign as a non-STABLE status"
    );

  }

}
