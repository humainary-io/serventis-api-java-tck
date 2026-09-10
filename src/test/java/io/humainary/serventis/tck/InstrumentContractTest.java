// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static io.humainary.substrates.api.Substrates.*;
import static java.util.Objects.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

/// Instrument obligations that hold across every vocabulary, stated once here rather than repeated
/// in each domain's contract test.
///
/// Three are covered.
///
/// **Emission** (`6.1`, `6.2`, `6.5`): the generic operation emits the observation it was given,
/// through the endpoint the factory was handed, altering nothing. Each is called three times with
/// the same arguments and all three emissions are read back, because `6.5` also forbids an
/// instrument to buffer, coalesce, reorder, drop, or accumulate — a shared signal table that
/// recorded anything about what had already passed through it shows up as a later call behaving
/// unlike the first. The domain tests emit through instruments they built one way, so nothing there
/// establishes that the *other* factory produces an instrument that emits at all.
///
/// **Absence** (`9`): a sign or dimension argument to an instrument operation MUST be rejected
/// rather than emitted as a partially formed observation. The domain tests exercise the operations
/// with valid arguments only, so without this class the constraint is unverified.
///
/// **Pooling** (`6.4`): the pool a vocabulary derives from a conduit is a Substrates Pool and
/// inherits that contract unchanged, so one name MUST resolve to the canonically identical
/// instrument. The domain tests retrieve instruments under differing names, which never exercises
/// the identity the contract turns on.
///
/// Emission and absence are properties of *an instrument*, and `10.3` gives two ways to obtain one,
/// so both are run against each: `of(pipe)` over a pipe drawn from the conduit, and
/// `pool(conduit).get(name)`. Exercising one would pass an implementation whose factories
/// disagreed — a pooled instrument holding an endpoint of its own and never emitting through the
/// conduit it was given, or an `of` validating an argument its pooled twin dereferenced. Pooling
/// itself is asserted only of the pool, since `of` constructs rather than resolves.
///
/// Lazy materialization is part of the same inherited contract and is not asserted here: it is not
/// observable through the Serventis surface, and the Substrates TCK verifies it where it is.
///
/// The **vocabulary templates** ([Surveys], [Cycles]) are held to all three, materialized over a
/// caller's sign set (`7.7`). Their factories take that set as an extra argument (`6.4`), so
/// nothing in the vocabulary sweep reaches them, and their own contract tests exercise `signal`
/// with valid arguments only — leaving the obligations unverified for exactly the instruments whose
/// sign set is not fixed at publication.
///
/// The inventory comes from [Vocabularies], which discovers it from the API artifact, and the
/// instruments are driven reflectively: they share no common supertype that admits an absent
/// argument without raw casts at every site, and a vocabulary left out of a hand-written list here
/// would be one whose emissions, whose pool, and whose absence handling nothing checks.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class InstrumentContractTest {

  private static final Name NAME = cortex().name("instrument.contract");

  @FunctionalInterface
  private interface Consumer2 {

    void accept(Object first, Object second);

  }

  @FunctionalInterface
  private interface Emitting {

    void accept(Object instrument, CaptureBuffer< Object > captures) throws Exception;

  }

  /// Asserts that invoking `operation` with `arguments` is rejected as an absence violation.

  private static void assertAbsenceRejected(
    final Method operation,
    final Object instrument,
    final Object... arguments
  ) {

    final var thrown =
      assertThrows(
        InvocationTargetException.class,
        () ->
          operation.invoke(instrument, arguments)
      );

    assertInstanceOf(
      NullPointerException.class,
      thrown.getCause()
    );

  }

  /// Emits the same signal three times and asserts all three arrive, unaltered and in order.
  ///
  /// Three rather than one because `6.5` forbids an instrument to buffer, coalesce, or accumulate:
  /// a signal table is shared, and one that recorded anything about what had already passed through
  /// it would show up here as a second call behaving differently from the first.

  private static void assertSignalsEmitted(
    final Class< ? > vocabulary,
    final Construction construction,
    final Object instrument,
    final CaptureBuffer< Object > captures,
    final Object sign,
    final Object dimension
  ) throws Exception {

    final var operation =
      operation(Signaler.class);

    operation.invoke(instrument, sign, dimension);
    operation.invoke(instrument, sign, dimension);
    operation.invoke(instrument, sign, dimension);

    final var emitted =
      captures
        .drainEmissions()
        .toList();

    assertEquals(
      3,
      emitted.size(),
      () -> vocabulary.getSimpleName() + " built via " + construction
        + " carried " + emitted.size() + " signals through the conduit it was given, not three"
    );

    for (final var signal : emitted) {

      assertEquals(sign, ((Signal< ?, ? >) signal).sign());
      assertEquals(dimension, ((Signal< ?, ? >) signal).dimension());

    }

  }

  private static Stream< Arguments > built(
    final Stream< Arguments > source
  ) {

    return
      source.flatMap(
        arguments ->
          Stream.of(Construction.values())
            .map(
              construction ->
                Arguments.of(
                  Stream.concat(
                    Stream.of(arguments.get()),
                    Stream.of(construction)
                  ).toArray()
                )
            )
      );

  }

  private static Stream< Arguments > builtSignalers() {

    return built(signalers());

  }

  /// The signers, the signalers, and the templates, each crossed with both constructions.

  private static Stream< Arguments > builtSigners() {

    return built(signers());

  }

  private static Stream< Arguments > builtTemplates() {

    return built(templates());

  }

  /// `Vocabulary.of(pipe)`, or `Template.of(signs, pipe)`.

  private static Object instrument(
    final Class< ? > vocabulary,
    final SignSet< ? > signs,
    final Pipe< ? > pipe
  ) throws Exception {

    final var factory =
      Vocabularies.of(vocabulary);

    return
      signs==null
        ? factory.invoke(null, pipe)
        :factory.invoke(null, signs, pipe);

  }

  /// The one operation a [Signer] or [Signaler] declares, taken from the protocol rather than from
  /// the instrument.
  ///
  /// Resolving it on the implementation would mean matching by name and arity, and
  /// `Class.getMethods()` orders nothing: a projection that added a same-arity convenience
  /// `sign`/`signal` beside the required one would leave which method this suite exercised up to
  /// the reflection layer. That is a false pass waiting to happen — the convenience overload could
  /// reject an absent argument while the normative operation did not, and `9` would read as
  /// satisfied.
  ///
  /// Taken from the interface, there is nothing to choose between: `Signer` and `Signaler` each
  /// declare exactly one method, and invoking it against the instrument dispatches virtually to
  /// whatever implements it. What is exercised is the operation the contract names, whatever else
  /// the instrument publishes.

  private static Method operation(
    final Class< ? > contract
  ) {

    final var declared =
      Stream.of(contract.getDeclaredMethods())
        .filter(method -> !method.isBridge())
        .filter(method -> !method.isSynthetic())
        .toList();

    assertEquals(
      1,
      declared.size(),
      () -> contract.getSimpleName() + " must declare exactly one operation (6.1, 6.2)"
    );

    return
      declared.getFirst();

  }

  /// `Vocabulary.pool(conduit)`, or `Template.pool(signs, conduit)`.

  private static Pool< ? > pool(
    final Class< ? > vocabulary,
    final SignSet< ? > signs,
    final Conduit< ? > conduit
  ) throws Exception {

    final var factory =
      Vocabularies.pool(vocabulary);

    return
      (Pool< ? >) (
        signs==null
          ? factory.invoke(null, conduit)
          :factory.invoke(null, signs, conduit)
      );

  }

  /// A valid member of an enum symbol set, to hold one argument valid while the other is absent.

  private static Object sample(
    final Class< ? > vocabulary,
    final String nested
  ) throws Exception {

    return
      Class.forName(vocabulary.getName() + "$" + nested)
        .getEnumConstants()[0];

  }

  /// Every vocabulary emitting a qualified sign, with the signal type its conduit carries.

  private static Stream< Arguments > signalers() {

    return
      Vocabularies.concrete()
        .stream()
        .filter(Vocabularies::qualified)
        .map(InstrumentContractTest::vocabulary);

  }

  /// Every vocabulary emitting a bare sign: the vocabulary and its conduit's emission type.
  ///
  /// Discovered rather than listed. The unqualified vocabularies are the concrete ones publishing
  /// no dimension set, which is the same fact as their instrument being a Signer.

  private static Stream< Arguments > signers() {

    return
      Vocabularies.concrete()
        .stream()
        .filter(vocabulary -> !Vocabularies.qualified(vocabulary))
        .map(InstrumentContractTest::vocabulary);

  }

  /// Every vocabulary template, with the signal type its conduit carries, a caller's sign set to
  /// materialize it over, and that set's sign type — the source of a valid sign, since a template
  /// publishes none of its own.

  private static Stream< Arguments > templates() {

    final var materializations =
      Map.of(
        Surveys.class, arguments(Surveys.class, Surveys.Signal.class, Statuses.SIGNS, Statuses.Sign.class),
        Cycles.class, arguments(Cycles.class, Cycles.Signal.class, Resources.SIGNS, Resources.Sign.class)
      );

    // Which sign set to materialize over is a choice; which templates exist is not. A template the
    // artifact publishes and this map does not name would otherwise be a template no case reaches.
    return
      Vocabularies.templates()
        .stream()
        .map(
          template ->
            requireNonNull(
              materializations.get(template),
              () -> template.getSimpleName() + " is published as a template but named by no case here"
            )
        );

  }

  /// Both streams, for the pooling contract, which does not care which shape a vocabulary takes.

  private static Stream< Arguments > vocabularies() {

    return
      Vocabularies.concrete()
        .stream()
        .map(InstrumentContractTest::vocabulary);

  }

  private static Arguments vocabulary(
    final Class< ? > vocabulary
  ) {

    return
      arguments(
        vocabulary,
        Vocabularies.emission(vocabulary)
      );

  }

  /// Runs `body` against one instrument built the given way, with a buffer capturing what the
  /// conduit carries.

  @SuppressWarnings("unchecked")
  private static void withInstrument(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Construction construction,
    final Emitting body
  ) throws Exception {

    final var circuit =
      cortex()
        .circuit();

    try {

      final var conduit =
        circuit.conduit(
          (Class< Object >) emission
        );

      final var captures =
        CaptureBuffer.of(circuit, conduit);

      try {

        body.accept(
          construction==Construction.OF
            ? instrument(vocabulary, signs, conduit.get(NAME))
            :pool(vocabulary, signs, conduit).get(NAME),
          captures
        );

      } finally {

        captures.close();

      }

    } finally {

      circuit
        .closeAwait();

    }

  }

  /// Runs `body` against instruments drawn twice from one vocabulary's pool, under one name.

  private static void withPool(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Consumer2 body
  ) throws Exception {

    withPool(
      vocabulary,
      emission,
      null,
      body
    );

  }

  /// The same, materializing a template over `signs` where one is given.

  @SuppressWarnings("unchecked")
  private static void withPool(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Consumer2 body
  ) throws Exception {

    final var circuit =
      cortex()
        .circuit();

    try {

      final var conduit =
        circuit.conduit(
          (Class< Object >) emission
        );

      final var pool =
        pool(vocabulary, signs, conduit);

      body.accept(
        pool.get(NAME),
        pool.get(NAME)
      );

    } finally {

      circuit
        .closeAwait();

    }

  }

  @SpecRef({"6.4", "substrates:10.1"})
  @DisplayName("a pool resolves one name to the canonically identical instrument")
  @ParameterizedTest(name = "{0}")
  @MethodSource("vocabularies")
  void poolResolvesOneNameToOneInstrument(
    final Class< ? > vocabulary,
    final Class< ? > emission
  ) throws Exception {

    withPool(
      vocabulary,
      emission,
      Assertions::assertSame
    );

  }

  @SpecRef({"6.1", "6.5"})
  @DisplayName("sign emits what it was given, once per call")
  @ParameterizedTest(name = "{0} via {2}")
  @MethodSource("builtSigners")
  void signEmitsWhatItWasGiven(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      null,
      construction,
      (instrument, captures) -> {

        final var sign =
          sample(vocabulary, "Sign");

        final var operation =
          operation(Signer.class);

        operation.invoke(instrument, sign);
        operation.invoke(instrument, sign);
        operation.invoke(instrument, sign);

        assertEquals(
          List.of(sign, sign, sign),
          captures.drainEmissions().toList(),
          () -> vocabulary.getSimpleName() + " built via " + construction
            + " did not carry three identical signs through the conduit it was given"
        );

      }
    );

  }

  @SpecRef("9")
  @DisplayName("sign rejects an absent sign")
  @ParameterizedTest(name = "{0} via {2}")
  @MethodSource("builtSigners")
  void signRejectsAbsentSign(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      null,
      construction,
      (instrument, ignored) ->
        assertAbsenceRejected(
          operation(Signer.class),
          instrument,
          (Object) null
        )
    );

  }

  @SpecRef({"6.2", "6.5"})
  @DisplayName("signal emits what it was given, once per call")
  @ParameterizedTest(name = "{0} via {2}")
  @MethodSource("builtSignalers")
  void signalEmitsWhatItWasGiven(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      null,
      construction,
      (instrument, captures) ->
        assertSignalsEmitted(
          vocabulary,
          construction,
          instrument,
          captures,
          sample(vocabulary, "Sign"),
          sample(vocabulary, "Dimension")
        )
    );

  }

  @SpecRef("9")
  @DisplayName("signal rejects an absent dimension")
  @ParameterizedTest(name = "{0} via {2}")
  @MethodSource("builtSignalers")
  void signalRejectsAbsentDimension(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      null,
      construction,
      (instrument, ignored) ->
        assertAbsenceRejected(
          operation(Signaler.class),
          instrument,
          sample(vocabulary, "Sign"),
          null
        )
    );

  }

  @SpecRef("9")
  @DisplayName("signal rejects an absent sign")
  @ParameterizedTest(name = "{0} via {2}")
  @MethodSource("builtSignalers")
  void signalRejectsAbsentSign(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      null,
      construction,
      (instrument, ignored) ->
        assertAbsenceRejected(
          operation(Signaler.class),
          instrument,
          null,
          sample(vocabulary, "Dimension")
        )
    );

  }

  @SpecRef({"6.4", "7.7", "substrates:10.1"})
  @DisplayName("a materialized template's pool resolves one name to one instrument")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templatePoolResolvesOneNameToOneInstrument(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Class< ? > ignoredSign
  ) throws Exception {

    withPool(
      vocabulary,
      emission,
      signs,
      Assertions::assertSame
    );

  }

  @SpecRef({"6.2", "6.5", "7.7"})
  @DisplayName("a materialized template emits what it was given, once per call")
  @ParameterizedTest(name = "{0} via {4}")
  @MethodSource("builtTemplates")
  void templateSignalEmitsWhatItWasGiven(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Class< ? > sign,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      signs,
      construction,
      (instrument, captures) ->
        assertSignalsEmitted(
          vocabulary,
          construction,
          instrument,
          captures,
          sign.getEnumConstants()[0],
          sample(vocabulary, "Dimension")
        )
    );

  }

  @SpecRef({"9", "7.7"})
  @DisplayName("a materialized template's signal rejects an absent dimension")
  @ParameterizedTest(name = "{0} via {4}")
  @MethodSource("builtTemplates")
  void templateSignalRejectsAbsentDimension(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Class< ? > sign,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      signs,
      construction,
      (instrument, ignored) ->
        assertAbsenceRejected(
          operation(Signaler.class),
          instrument,
          sign.getEnumConstants()[0],
          null
        )
    );

  }

  @SpecRef({"9", "7.7"})
  @DisplayName("a materialized template's signal rejects an absent sign")
  @ParameterizedTest(name = "{0} via {4}")
  @MethodSource("builtTemplates")
  void templateSignalRejectsAbsentSign(
    final Class< ? > vocabulary,
    final Class< ? > emission,
    final SignSet< ? > signs,
    final Class< ? > ignoredSign,
    final Construction construction
  ) throws Exception {

    withInstrument(
      vocabulary,
      emission,
      signs,
      construction,
      (instrument, ignored) ->
        assertAbsenceRejected(
          operation(Signaler.class),
          instrument,
          null,
          sample(vocabulary, "Dimension")
        )
    );

  }

  /// The two ways `10.3` says an instrument comes into being.
  ///
  /// Both are required surface, and a suite that exercised one would pass an implementation whose
  /// factories disagreed — a pooled instrument that cached an endpoint of its own and never emitted
  /// through the conduit it was handed, say, or an `of` that validated an argument its pooled twin
  /// dereferenced. Every obligation below that is a property of *an instrument* rather than of the
  /// pool is therefore run once per construction.

  private enum Construction {

    /// `Vocabulary.of(pipe)` — a fresh instrument over a pipe drawn from the conduit.
    OF,

    /// `Vocabulary.pool(conduit).get(name)` — the pooled instrument for that name.
    POOL

  }

}
