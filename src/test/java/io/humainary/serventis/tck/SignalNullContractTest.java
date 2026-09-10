// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.*;
import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.specs.api.Specs.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.*;

/// Cross-domain check on the absence constraint of every `Signal` record.
///
/// This is Java-projection coverage, not a portable requirement. SPEC §9 constrains the sign and
/// dimension arguments of an **instrument operation**; it says nothing about a record's canonical
/// constructor, which is a projection artifact — §4.4 requires only that a signal *is* a sign
/// together with a dimension, so a component-less signal is not one. Every `Signal` component is
/// marked `@NotNull`, but that annotation is `SOURCE`-retained and enforces nothing on its own, so
/// each record backs it with a validating compact constructor.
///
/// The instrument path is covered separately, by the §9 absence tests — so the surface this test
/// defends is the record's canonical constructor, which records publish
/// whether or not the domain wants it. Without the constructor, a directly built `Signal` carrying a
/// null component fails much later inside a `SignMap` lookup, far from the construction site.
///
/// Components are probed reflectively rather than by hand: the property is structural, not
/// semantic, so unlike the mapping conventions there is nothing per-domain to pin, and a component
/// added later is covered without editing this test. The registry of record types stays explicit,
/// so that each entry names the domain it belongs to in the parameterized report — but it is
/// checked against the artifact rather than against itself, so a `Signal` added anywhere in the API
/// fails until it is registered here.
///
/// The `@NotNull` declarations themselves are not asserted — `SOURCE` retention discards them
/// before runtime, so reflection cannot see them. Only the enforcement is observable, which is the
/// half that matters; the annotation is documentation for the reader.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class SignalNullContractTest {

  /// Every `Signal` record the API artifact publishes, discovered rather than listed.
  ///
  /// [Vocabularies] reads the artifact back off its own code source; every record among what it
  /// publishes that implements [Serventis.Signal] is one of these. Scanning is what makes the
  /// completeness check mean something: an inventory compared against a hand-maintained count would
  /// agree with the list beside it whatever the API did.

  private static Set< String > published() {

    return
      Vocabularies.classes()
        .stream()
        .filter(Class::isRecord)
        .filter(Serventis.Signal.class::isAssignableFrom)
        .map(Class::getName)
        .collect(toCollection(TreeSet::new));

  }

  /// A valid argument for a record component, used to isolate one null at a time.
  ///
  /// Concrete enum components resolve to their first constant. The generic records
  /// ([Cycles.Signal], [Surveys.Signal]) erase their sign type variable to the [Sign] interface, so
  /// no constant is reachable from the component type and any sign instance will do.

  private static Object sample(
    final Class< ? > type
  ) {

    if (type.isEnum()) {
      return type.getEnumConstants()[0];
    }

    if (type==Sign.class) {
      return Outcomes.Sign.SUCCESS;
    }

    if (type==Dimension.class || type==Category.class) {
      return Statuses.Dimension.MEASURED;
    }

    return fail(
      "no sample value for component type " + type.getName()
        + " — extend sample() when a Signal gains a component of a new kind"
    );

  }

  private static Stream< Arguments > signals() {

    return
      Stream.of(
        Arguments.of("Services", Services.Signal.class),
        Arguments.of("Transactions", Transactions.Signal.class),
        Arguments.of("Timers", Timers.Signal.class),
        Arguments.of("Leases", Leases.Signal.class),
        Arguments.of("Exchanges", Exchanges.Signal.class),
        Arguments.of("Agents", Agents.Signal.class),
        Arguments.of("Probes", Probes.Signal.class),
        Arguments.of("Sensors", Sensors.Signal.class),
        Arguments.of("Evals", Evals.Signal.class),
        Arguments.of("Flows", Flows.Signal.class),
        Arguments.of("Statuses", Statuses.Signal.class),
        Arguments.of("Situations", Situations.Signal.class),
        Arguments.of("Systems", Systems.Signal.class),
        Arguments.of("Surveys", Surveys.Signal.class),
        Arguments.of("Cycles", Cycles.Signal.class)
      );

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("signals")
  void constructorRejectsNullComponents(
    final String name,
    final Class< ? extends Record > type
  ) throws ReflectiveOperationException {

    final var components = type.getRecordComponents();

    final var types =
      Stream.of(components)
        .map(RecordComponent::getType)
        .toArray(Class< ? >[]::new);

    final var constructor = type.getDeclaredConstructor(types);

    final var valid =
      Stream.of(types)
        .map(SignalNullContractTest::sample)
        .toArray();

    assertNotNull(
      constructor.newInstance(valid),
      () -> name + ".Signal rejected a fully populated argument list"
    );

    for (var index = 0; index < components.length; index++) {

      final var arguments = valid.clone();
      arguments[index] = null;

      final var component = components[index];

      final var thrown =
        assertThrows(
          InvocationTargetException.class,
          () -> constructor.newInstance(arguments),
          () -> name + ".Signal accepted a null " + component.getName()
        );

      assertInstanceOf(
        NullPointerException.class,
        thrown.getCause(),
        () -> name + ".Signal signalled a null " + component.getName()
          + " with " + thrown.getCause()
          + " rather than a NullPointerException"
      );

    }

  }

  @Test
  void registryIsComplete() {

    assertEquals(
      published(),
      signals()
        .map(arguments -> ((Class< ? >) arguments.get()[1]).getName())
        .collect(toCollection(TreeSet::new))
    );

  }

}
