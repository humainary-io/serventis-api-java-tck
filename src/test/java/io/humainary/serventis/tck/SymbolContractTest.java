// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.specs.api.Specs.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/// Cross-domain checks on the symbol contract: set membership, dimension kind, and homograph
/// separation.
///
/// Four specification requirements are structural rather than per-domain, and none of them is
/// covered by the domain contract tests:
///
///   - **Set membership and order** (`4.1`, `4.5`, `8.2`, `10.4`) — every published sign set and
///     dimension set holds exactly the members the registry records, in exactly that order. The
///     domain tests each pin their own vocabulary, in several idioms and to several depths; these
///     two pin all of them at once, so a sign inserted mid-set is a deliberate edit here rather
///     than a silent renumbering of every index after it.
///   - **Dimension kind** (`4.3`) — every dimension is exactly one of [Category] and
///     [Spectrum], and every published dimension set is uniform in kind. Java cannot make a mixed
///     enum unrepresentable, because both marker interfaces are non-sealed so that domain
///     vocabularies can implement them; this test is the enforcement.
///   - **Homograph separation** (`8.4`) — symbols sharing a lexeme across distinct sets
///     are distinct symbols, whether their meanings differ or coincide exactly.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class SymbolContractTest {

  private static Arguments arguments(
    final Class< ? > type
  ) {

    return
      Arguments.of(
        type.getDeclaringClass().getSimpleName(),
        type
      );

  }

  private static Enum< ? > asEnum(
    final Object constant
  ) {

    return
      (Enum< ? >) constant;

  }

  /// Every dimension set published by the API — registered domains, universal vocabularies, and
  /// both vocabulary templates — discovered from the artifact rather than listed.

  private static Stream< Arguments > dimensions() {

    return
      Vocabularies.published()
        .stream()
        .map(vocabulary -> Vocabularies.nested(vocabulary, "Dimension"))
        .filter(Objects::nonNull)
        .map(SymbolContractTest::arguments);

  }

  /// The declared members of every published set of the given nested enum name, by vocabulary.

  private static Map< String, List< String > > membership(
    final String kind
  ) {

    final Map< String, List< String > > published =
      new HashMap<>();

    for (final var vocabulary : Vocabularies.published()) {

      final var type =
        Vocabularies.nested(vocabulary, kind);

      if (type==null) continue;

      published.put(
        vocabulary.getSimpleName(),
        Stream.of(type.getEnumConstants())
          .map(SymbolContractTest::asEnum)
          .map(Enum::name)
          .toList()
      );

    }

    return published;

  }

  /// Groups symbols by name, keeping only names carried by more than one set, mapped to the sorted
  /// simple names of the vocabularies declaring them.

  private static Map< String, List< String > > sharedLexemes(
    final List< ? extends Enum< ? > > symbols
  ) {

    final Map< String, List< String > > byName =
      new HashMap<>();

    for (final Enum< ? > symbol : symbols) {

      byName
        .computeIfAbsent(
          symbol.name(),
          _ -> new ArrayList<>()
        )
        .add(
          symbol.getDeclaringClass().getDeclaringClass().getSimpleName()
        );

    }

    final Map< String, List< String > > shared =
      new HashMap<>();

    byName.forEach(
      (name, owners) -> {

        if (owners.size() > 1) {

          shared.put(
            name,
            owners.stream().sorted().toList()
          );

        }

      }
    );

    return shared;

  }

  /// Every sign set published by the API, for the homograph check. Discovered on the same basis:
  /// a vocabulary missing from a hand-written list here is one whose lexemes never enter the
  /// inventory, so a collision it introduces would go unrecorded rather than reported.

  private static Stream< Class< ? > > signs() {

    return
      Vocabularies.concrete()
        .stream()
        .map(vocabulary -> Vocabularies.nested(vocabulary, "Sign"));

  }

  /// `4.3` — a dimension is exactly one of [Category] and [Spectrum], never both and
  /// never neither, and a dimension set is uniform: every member takes the same kind.

  @SpecRef("4.3")
  @DisplayName("dimension set has exactly one uniform kind")
  @ParameterizedTest(name = "{0}")
  @MethodSource("dimensions")
  void dimensionKindIsExactlyOneAndUniform(
    final String name,
    final Class< ? > type
  ) {

    final var constants =
      type.getEnumConstants();

    assertNotNull(
      constants,
      name + ".Dimension must be an enum"
    );

    assertTrue(
      constants.length > 0,
      name + ".Dimension must have members"
    );

    Boolean expected = null;

    for (final var dimension : constants) {

      final var category =
        dimension instanceof Category;

      final var spectrum =
        dimension instanceof Spectrum;

      assertNotEquals(
        category,
        spectrum,
        () -> name + "." + dimension
          + " must be exactly one of Category and Spectrum, not "
          + (category ? "both":"neither")
      );

      if (expected==null) {

        expected = category;

      } else {

        assertEquals(
          expected,
          category,
          () -> name + " mixes Category and Spectrum members; a dimension set's kind says whether "
            + "its index order carries meaning and cannot differ between members"
        );

      }

    }

  }

  /// `4.1`, `4.5`, `8.2`, `10.4` — the exact ordered membership of every published dimension set,
  /// on the same basis as [#signSetMembershipMatchesRegistry()] and for the same reason: a
  /// dimension inserted mid-set renumbers every index after it, and an index is what a set is.

  @SpecRef({
    "4.1", "4.5", "8.2", "10.4", "7.1", "7.2", "7.5", "7.8", "7.9", "registry:agents",
    "registry:evals",
    "registry:exchanges", "registry:flows", "registry:leases", "registry:probes",
    "registry:sensors", "registry:services", "registry:timers", "registry:transactions"
  })
  @DisplayName("dimension set membership matches the registry")
  @Test
  void dimensionSetMembershipMatchesRegistry() {

    assertEquals(
      Map.ofEntries(
        Map.entry("Agents", List.of("PROMISER", "PROMISEE")),
        Map.entry("Cycles", List.of("SINGLE", "REPEAT", "RETURN")),
        Map.entry(
          "Evals",
          List.of(
            "CORRECTNESS", "COMPLETENESS", "GROUNDEDNESS", "HELPFULNESS", "ADHERENCE", "TOOLING",
            "ROUTING", "SAFETY"
          )
        ),
        Map.entry("Exchanges", List.of("PROVIDER", "RECEIVER")),
        Map.entry("Flows", List.of("INGRESS", "TRANSIT", "EGRESS")),
        Map.entry("Leases", List.of("LESSOR", "LESSEE")),
        Map.entry("Probes", List.of("OUTBOUND", "INBOUND")),
        Map.entry("Sensors", List.of("BASELINE", "THRESHOLD", "TARGET")),
        Map.entry("Services", List.of("CALLER", "CALLEE")),
        Map.entry("Situations", List.of("CONSTANT", "VARIABLE", "VOLATILE")),
        Map.entry("Statuses", List.of("TENTATIVE", "MEASURED", "CONFIRMED")),
        Map.entry("Surveys", List.of("DIVIDED", "MAJORITY", "UNANIMOUS")),
        Map.entry("Systems", List.of("SPACE", "FLOW", "LINK", "TIME")),
        Map.entry("Timers", List.of("DEADLINE", "THRESHOLD")),
        Map.entry("Transactions", List.of("COORDINATOR", "PARTICIPANT"))
      ),
      membership("Dimension"),
      "dimension set membership drifted from REGISTRY.md"
    );

  }

  /// `8.4` — the same inventory over dimension sets. `THRESHOLD` in `Timers` and in `Sensors` both
  /// mean a configured limit, and remain two distinct dimensions in two distinct sets; the aligned
  /// meaning is documentation, not something an implementation may rely on.

  @SpecRef("8.4")
  @DisplayName("shared dimension lexemes match the registry inventory")
  @Test
  void sharedDimensionLexemesMatchRegistry() {

    assertEquals(
      Map.of("THRESHOLD", List.of("Sensors", "Timers")),
      sharedLexemes(
        dimensions()
          .map(arguments -> (Class< ? >) arguments.get()[1])
          .map(Class::getEnumConstants)
          .flatMap(Arrays::stream)
          .map(SymbolContractTest::asEnum)
          .toList()
      ),
      "shared dimension lexemes drifted from REGISTRY.md — update §5.4 and the Shared Lexeme "
        + "Index in Part III with the change"
    );

  }

  /// `8.4` — the registry's shared-lexeme inventory, pinned.
  ///
  /// Java already guarantees that constants of distinct enum types are never identical or equal, so
  /// asserting that homographs are "distinct symbols" would only retest the language. What is worth
  /// testing is the **inventory**: which lexemes are shared, and by which vocabularies. A new sign
  /// that collides with another vocabulary's lexeme, or a rename that removes a collision, changes
  /// what `REGISTRY.md` records and what a consumer building any name-keyed view must handle.
  /// Pinning it here makes that a deliberate edit rather than a silent one.
  ///
  /// The expected map is the complete set, so it corresponds to Part III's *Shared Lexeme Index*
  /// rather than to §4, which covers only the subset whose meanings differ across families.

  @SpecRef("8.4")
  @DisplayName("shared sign lexemes match the registry inventory")
  @Test
  void sharedSignLexemesMatchRegistry() {

    assertEquals(
      Map.ofEntries(
        Map.entry("ABANDON", List.of("Latches", "Locks")),
        Map.entry("ACQUIRE", List.of("Leases", "Locks", "Resources")),
        Map.entry("ATTEMPT", List.of("Atomics", "Locks", "Resources")),
        Map.entry("CONTRACT", List.of("Exchanges", "Pools", "Valves")),
        Map.entry("DENY", List.of("Actors", "Leases", "Locks", "Resources", "Valves")),
        Map.entry("DISCONNECT", List.of("Probes", "Services")),
        Map.entry("DROP", List.of("Routers", "Valves")),
        Map.entry("EXPAND", List.of("Pools", "Valves")),
        Map.entry("EXPIRE", List.of("Caches", "Leases", "Services", "Transactions")),
        Map.entry("FAIL", List.of("Atomics", "Evals", "Flows", "Outcomes", "Probes", "Processes", "Services", "Tasks")),
        Map.entry("GRANT", List.of("Leases", "Locks", "Resources")),
        Map.entry("INCREMENT", List.of("Counters", "Gauges")),
        Map.entry("MISS", List.of("Caches", "Timers")),
        Map.entry("NORMAL", List.of("Situations", "Systems")),
        Map.entry("OVERFLOW", List.of("Counters", "Gauges", "Pipelines", "Queues", "Stacks")),
        Map.entry("PASS", List.of("Evals", "Valves")),
        Map.entry("PROBE", List.of("Breakers", "Leases")),
        Map.entry("PROMISE", List.of("Actors", "Agents")),
        Map.entry("REJECT", List.of("Services", "Tasks")),
        Map.entry("RELEASE", List.of("Latches", "Leases", "Locks", "Resources")),
        Map.entry("RESET", List.of("Breakers", "Counters", "Gauges", "Latches")),
        Map.entry("RESUME", List.of("Processes", "Services", "Tasks")),
        Map.entry("SCHEDULE", List.of("Services", "Tasks")),
        Map.entry("SKIP", List.of("Evals", "Pipelines")),
        Map.entry("STABLE", List.of("Statuses", "Trends")),
        Map.entry("START", List.of("Processes", "Services", "Tasks", "Transactions")),
        Map.entry("STOP", List.of("Processes", "Services")),
        Map.entry("SUCCESS", List.of("Atomics", "Flows", "Outcomes", "Services")),
        Map.entry("SUSPEND", List.of("Processes", "Services", "Tasks")),
        Map.entry("TIMEOUT", List.of("Latches", "Locks", "Resources", "Tasks")),
        Map.entry("TRANSFER", List.of("Exchanges", "Probes")),
        Map.entry("UNDERFLOW", List.of("Gauges", "Queues", "Stacks")),
        Map.entry("UNKNOWN", List.of("Evals", "Outcomes")),
        Map.entry("WARNING", List.of("Logs", "Situations"))
      ),
      sharedLexemes(
        signs()
          .map(Class::getEnumConstants)
          .flatMap(Arrays::stream)
          .map(SymbolContractTest::asEnum)
          .toList()
      ),
      "shared sign lexemes drifted from the Shared Lexeme Index in REGISTRY.md Part III — "
        + "update the registry with the change"
    );

  }

  /// `4.5`, `8.2`, `10.4` — the exact ordered membership of every published sign set.
  ///
  /// The domain tests each pin their own vocabulary, in several different idioms and with several
  /// different degrees of thoroughness; a handful pin only a size, and a few pin nothing at all. A
  /// sign inserted in the middle of one of those would keep every one of them passing while
  /// renumbering every index after it — and an index is what a published set *is* (`4.5`), what
  /// every property map is keyed on, and what a persisted or transmitted observation resolves
  /// through. `10.4` asks a suite to verify that a vocabulary published under a registered name
  /// matches the registry's membership exactly. This is that check, once, for all of them.
  ///
  /// The expected tables are REGISTRY.md, transcribed. Where a vocabulary appears in the registry
  /// only as a mirror of §7 — the universal vocabularies and the two templates — §7 governs and the
  /// registry agrees with it; either way the membership below is the published one.
  ///
  /// The inventory is discovered, so a vocabulary added to the API and not to the table fails here
  /// rather than passing unexamined.
  ///
  /// Every vocabulary the table holds is named in the annotation — its registry entry, or its §7
  /// section where §7 governs it. The list is long because the table is: a consolidated pin carries
  /// the traceability of everything consolidated into it, and a search for `registry:locks` that
  /// missed the one test asserting what Locks contains would point at the wrong places to edit when
  /// Locks changes.
  ///
  /// Pinning the order is also what `10.4` asks for in its second form — that symbol indices are
  /// dense, stable, and unique within each set. In this projection a set is an enum and its index is
  /// the ordinal, so density and uniqueness hold by construction and cannot be violated; what can
  /// drift is which members there are and in what order, and that is what these tables hold still.

  @SpecRef({
    "4.1", "4.5", "8.2", "10.4", "7.1", "7.2", "7.3", "7.4", "7.5", "7.6",
    "registry:actors",
    "registry:agents", "registry:atomics", "registry:breakers", "registry:caches",
    "registry:counters", "registry:evals", "registry:exchanges", "registry:flows",
    "registry:gauges", "registry:latches", "registry:leases", "registry:locks", "registry:logs",
    "registry:pipelines", "registry:pools", "registry:probes", "registry:processes",
    "registry:queues", "registry:resources", "registry:routers", "registry:sensors",
    "registry:services", "registry:stacks", "registry:tasks", "registry:timers",
    "registry:transactions", "registry:valves"
  })
  @DisplayName("sign set membership matches the registry")
  @Test
  void signSetMembershipMatchesRegistry() {

    assertEquals(
      Map.ofEntries(
        Map.entry(
          "Actors",
          List.of(
            "ASK", "AFFIRM", "EXPLAIN", "REPORT", "REQUEST", "COMMAND", "ACKNOWLEDGE", "DENY",
            "CLARIFY", "PROMISE", "DELIVER"
          )
        ),
        Map.entry(
          "Agents",
          List.of(
            "OFFER", "PROMISE", "ACCEPT", "FULFILL", "RETRACT", "BREACH", "INQUIRE", "OBSERVE",
            "DEPEND", "VALIDATE"
          )
        ),
        Map.entry(
          "Atomics",
          List.of(
            "ATTEMPT", "SUCCESS", "FAIL", "SPIN", "YIELD", "BACKOFF", "PARK", "EXHAUST"
          )
        ),
        Map.entry("Breakers", List.of("CLOSE", "OPEN", "HALF_OPEN", "TRIP", "PROBE", "RESET")),
        Map.entry("Caches", List.of("LOOKUP", "HIT", "MISS", "STORE", "EVICT", "EXPIRE", "REMOVE")),
        Map.entry("Counters", List.of("INCREMENT", "OVERFLOW", "RESET")),
        Map.entry("Evals", List.of("PASS", "FAIL", "UNKNOWN", "SKIP", "ERROR")),
        Map.entry("Exchanges", List.of("CONTRACT", "TRANSFER")),
        Map.entry("Flows", List.of("SUCCESS", "FAIL")),
        Map.entry("Gauges", List.of("INCREMENT", "DECREMENT", "OVERFLOW", "UNDERFLOW", "RESET")),
        Map.entry("Latches", List.of("AWAIT", "ARRIVE", "RELEASE", "TIMEOUT", "RESET", "ABANDON")),
        Map.entry(
          "Leases",
          List.of(
            "ACQUIRE", "DENY", "EXTEND", "EXPIRE", "GRANT", "PROBE", "RELEASE", "RENEW", "REVOKE"
          )
        ),
        Map.entry(
          "Locks",
          List.of(
            "ATTEMPT", "ACQUIRE", "GRANT", "DENY", "TIMEOUT", "RELEASE", "UPGRADE", "DOWNGRADE",
            "CONTEST", "ABANDON"
          )
        ),
        Map.entry("Logs", List.of("SEVERE", "WARNING", "INFO", "DEBUG")),
        Map.entry("Operations", List.of("BEGIN", "ADVANCE", "END")),
        Map.entry("Outcomes", List.of("SUCCESS", "FAIL", "UNKNOWN")),
        Map.entry(
          "Pipelines",
          List.of(
            "INPUT", "OUTPUT", "TRANSFORM", "FILTER", "AGGREGATE", "BUFFER", "BACKPRESSURE",
            "OVERFLOW", "CHECKPOINT", "WATERMARK", "LAG", "SKIP"
          )
        ),
        Map.entry("Pools", List.of("EXPAND", "CONTRACT", "BORROW", "RECLAIM")),
        Map.entry(
          "Probes",
          List.of(
            "CONNECT", "DISCONNECT", "TRANSFER", "PROCESS", "SUCCEED", "FAIL"
          )
        ),
        Map.entry(
          "Processes",
          List.of(
            "SPAWN", "START", "STOP", "FAIL", "CRASH", "KILL", "RESTART", "SUSPEND", "RESUME"
          )
        ),
        Map.entry("Queues", List.of("ENQUEUE", "DEQUEUE", "OVERFLOW", "UNDERFLOW")),
        Map.entry("Resources", List.of("ATTEMPT", "ACQUIRE", "GRANT", "DENY", "TIMEOUT", "RELEASE")),
        Map.entry(
          "Routers",
          List.of(
            "SEND", "RECEIVE", "FORWARD", "ROUTE", "DROP", "FRAGMENT", "REASSEMBLE", "CORRUPT",
            "REORDER"
          )
        ),
        Map.entry("Sensors", List.of("BELOW", "NOMINAL", "ABOVE")),
        Map.entry(
          "Services",
          List.of(
            "START", "STOP", "CALL", "SUCCESS", "FAIL", "RECOURSE", "REDIRECT", "EXPIRE", "RETRY",
            "REJECT", "DISCARD", "DELAY", "SCHEDULE", "SUSPEND", "RESUME", "DISCONNECT"
          )
        ),
        Map.entry("Situations", List.of("NORMAL", "WARNING", "CRITICAL")),
        Map.entry("Stacks", List.of("PUSH", "POP", "OVERFLOW", "UNDERFLOW")),
        Map.entry(
          "Statuses",
          List.of(
            "CONVERGING", "STABLE", "DIVERGING", "ERRATIC", "DEGRADED", "DEFECTIVE", "DOWN"
          )
        ),
        Map.entry("Systems", List.of("NORMAL", "LIMIT", "ALARM", "FAULT")),
        Map.entry(
          "Tasks",
          List.of(
            "SUBMIT", "REJECT", "SCHEDULE", "START", "PROGRESS", "SUSPEND", "RESUME", "COMPLETE",
            "FAIL", "CANCEL", "TIMEOUT"
          )
        ),
        Map.entry("Timers", List.of("MEET", "MISS")),
        Map.entry(
          "Transactions",
          List.of(
            "START", "PREPARE", "COMMIT", "ROLLBACK", "ABORT", "EXPIRE", "CONFLICT", "COMPENSATE"
          )
        ),
        Map.entry("Trends", List.of("STABLE", "DRIFT", "SPIKE", "CYCLE", "CHAOS")),
        Map.entry("Valves", List.of("PASS", "DENY", "EXPAND", "CONTRACT", "DROP", "DRAIN"))
      ),
      membership("Sign"),
      "sign set membership drifted from REGISTRY.md — a vocabulary is fixed in membership and "
        + "order once published (8.3), so this is a new version of it, not an edit to this one"
    );

  }

}
