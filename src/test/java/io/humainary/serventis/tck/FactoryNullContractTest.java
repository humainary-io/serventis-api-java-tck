// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.stream.*;

import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

/// Factory precondition tests shared by the Serventis instrument families.
///
/// This is Java-projection coverage, not a portable requirement, so the methods carry no
/// `@SpecRef`. SPEC §6.4 specifies the *shape* of the two factories and the pooling semantics of
/// what they return; it says nothing about how a projection reacts to an absent pipe, conduit, or
/// sign set, which §9 does not reach either — §9 constrains the sign and dimension arguments of an
/// **instrument operation**, not the arguments of a factory. What is asserted here is the
/// precondition every factory declares in its own contract: each argument is `@NotNull`, that
/// annotation is `SOURCE`-retained and enforces nothing on its own, and each factory therefore
/// backs it with an explicit check. Only the enforcement is observable, and it is the half that
/// matters.
///
/// The matrix is complete: every argument of every factory, made absent one at a time while the
/// others stay valid. A factory has two shapes — a concrete vocabulary takes the endpoint alone, a
/// template (§7.7) leads with the caller's sign set — so across the four factories there are six
/// argument positions: one endpoint each for the concrete `of` and `pool`, and a sign set and an
/// endpoint each for the template pair. All six are covered, for every vocabulary the artifact
/// publishes. Checking only the first argument of each would leave a `pool` that validated its
/// sign set and dereferenced its conduit indistinguishable from one that validated both.
///
/// The inventory comes from [Vocabularies], which discovers it from the API artifact: a vocabulary
/// left out of a hand-written list here would be one whose factories nothing checks.

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class FactoryNullContractTest {

  /// A sign set standing in for the caller's, wherever a template needs a valid one. Which set it
  /// is does not matter to a precondition: every template rejects the argument beside it before
  /// this one is read for anything.

  private static final SignSet< Statuses.Sign > SIGNS = Statuses.SIGNS;

  @FunctionalInterface
  private interface Endpoints {

    void accept(Conduit< ? > conduit, Pipe< ? > pipe);

  }

  /// Asserts that invoking a static factory with `arguments` is rejected with a
  /// [NullPointerException], reported through the reflective wrapper the invocation raises.

  private static void assertRejected(
    final Method factory,
    final String description,
    final Object... arguments
  ) {

    final var thrown =
      assertThrows(
        InvocationTargetException.class,
        () ->
          factory.invoke(null, arguments),
        () -> description + " was accepted"
      );

    assertInstanceOf(
      NullPointerException.class,
      thrown.getCause(),
      () -> description + " was rejected with " + thrown.getCause()
        + " rather than a NullPointerException"
    );

  }

  private static Stream< Arguments > concrete() {

    return
      Vocabularies.concrete()
        .stream()
        .map(vocabulary -> arguments(vocabulary.getSimpleName(), vocabulary));

  }

  private static Stream< Arguments > templates() {

    return
      Vocabularies.templates()
        .stream()
        .map(vocabulary -> arguments(vocabulary.getSimpleName(), vocabulary));

  }

  /// Runs `body` with a circuit whose conduit carries the vocabulary's emission type, handing it a
  /// live conduit and a live pipe drawn from it.

  @SuppressWarnings("unchecked")
  private static void withEndpoints(
    final Class< ? > vocabulary,
    final Endpoints body
  ) {

    final var circuit =
      cortex()
        .circuit();

    try {

      final var conduit =
        circuit.conduit(
          (Class< Object >) Vocabularies.emission(vocabulary)
        );

      body.accept(
        conduit,
        conduit.get(
          cortex().name("factory.contract")
        )
      );

    } finally {

      circuit
        .closeAwait();

    }

  }

  @DisplayName("of rejects an absent pipe")
  @ParameterizedTest(name = "{0}")
  @MethodSource("concrete")
  void ofRejectsAbsentPipe(
    final String name,
    final Class< ? > vocabulary
  ) {

    assertRejected(
      Vocabularies.of(vocabulary),
      name + ".of(null)",
      (Object) null
    );

  }

  @DisplayName("pool rejects an absent conduit")
  @ParameterizedTest(name = "{0}")
  @MethodSource("concrete")
  void poolRejectsAbsentConduit(
    final String name,
    final Class< ? > vocabulary
  ) {

    assertRejected(
      Vocabularies.pool(vocabulary),
      name + ".pool(null)",
      (Object) null
    );

  }

  @DisplayName("a template's of rejects an absent pipe")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templateOfRejectsAbsentPipe(
    final String name,
    final Class< ? > vocabulary
  ) {

    assertRejected(
      Vocabularies.of(vocabulary),
      name + ".of(signs, null)",
      SIGNS,
      null
    );

  }

  @DisplayName("a template's of rejects an absent sign set")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templateOfRejectsAbsentSignSet(
    final String name,
    final Class< ? > vocabulary
  ) {

    withEndpoints(
      vocabulary,
      (_, pipe) ->
        assertRejected(
          Vocabularies.of(vocabulary),
          name + ".of(null, pipe)",
          null,
          pipe
        )
    );

  }

  @DisplayName("a template's pool rejects an absent conduit")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templatePoolRejectsAbsentConduit(
    final String name,
    final Class< ? > vocabulary
  ) {

    assertRejected(
      Vocabularies.pool(vocabulary),
      name + ".pool(signs, null)",
      SIGNS,
      null
    );

  }

  @DisplayName("a template's pool rejects an absent sign set")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templatePoolRejectsAbsentSignSet(
    final String name,
    final Class< ? > vocabulary
  ) {

    withEndpoints(
      vocabulary,
      (conduit, _) ->
        assertRejected(
          Vocabularies.pool(vocabulary),
          name + ".pool(null, conduit)",
          null,
          conduit
        )
    );

  }

}
