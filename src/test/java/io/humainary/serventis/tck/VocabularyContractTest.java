// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

/// The static surface every vocabulary is required to publish, checked against every vocabulary.
///
/// `10.3` names that surface exactly: a concrete vocabulary publishes `SIGNS`, a qualified one also
/// publishes `DIMENSIONS`, and both factories take the pipe or conduit alone. A template (`7.7`)
/// publishes `DIMENSIONS` and **no** `SIGNS`, and leads both factories with the caller's sign set.
/// `4.5` adds what the published sets must be: finite, and an alphabet of exactly `size` members.
///
/// The domain contract tests reach these fields — they emit through the instruments and read the
/// property maps built over the sets — but none of them inspects the fields as a *surface*. A set
/// published under the wrong name, at the wrong type, or sized out of step with the enum it is
/// built from would leave every one of those tests passing, because each domain test only ever uses
/// the field it was written against. This is the test that reads the declarations.
///
/// It reads them **parameterized**. A raw `SignSet`, a `SignSet` over some other vocabulary's sign,
/// a `pool` returning a raw `Pool` — every one of those satisfies an erasure check while publishing
/// a surface no generic consumer can use, and `10.3` writes the type arguments out. So the
/// assertions here compare whole generic type names, built from the vocabulary's own nested types:
/// what `SIGNS` is a set *of*, what a factory's pipe is a pipe *of*, and what its pool is a pool
/// *of*. The instrument type is read off the `of` factory — `10.3` constrains what a factory hands
/// back, not how many nested types happen to implement a protocol, so counting those would refuse
/// an implementation over a private adapter it says nothing about. Because the type comes from the
/// factory, the factory returning it proves nothing on its own; what is asserted instead is the
/// protocol it fulfils, a [Signer] for an unqualified vocabulary and a [Signaler] for a qualified
/// one.
///
/// Every case is driven from [Vocabularies], which discovers the inventory from the API artifact.
/// A handwritten list here would have the defect this class exists to catch: a vocabulary missing
/// from it is a vocabulary whose surface is never checked, and its absence fails nothing.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class VocabularyContractTest {

  private static final String SIGNS = Vocabularies.SIGNS;
  private static final String DIMENSIONS = Vocabularies.DIMENSIONS;

  /// Asserts a factory's whole generic signature: its parameter types in order, and its return
  /// type.

  private static void assertSignature(
    final Method factory,
    final String owner,
    final List< String > parameters,
    final String returns
  ) {

    assertEquals(
      parameters,
      Stream.of(factory.getGenericParameterTypes())
        .map(Type::getTypeName)
        .toList(),
      () -> owner + "." + factory.getName() + " does not take the required arguments"
    );

    assertEquals(
      returns,
      factory.getGenericReturnType().getTypeName(),
      () -> owner + "." + factory.getName() + " does not return the required type"
    );

  }

  /// The canonical constructor of a `Signal` record, for building the sign x dimension product.

  private static Constructor< ? > canonical(
    final Class< ? > signal
  ) throws Exception {

    return
      signal.getDeclaredConstructor(
        Stream.of(signal.getRecordComponents())
          .map(RecordComponent::getType)
          .toArray(Class< ? >[]::new)
      );

  }

  /// The concrete vocabularies: those publishing a sign set of their own.

  private static Stream< Arguments > concrete() {

    return
      Vocabularies.concrete()
        .stream()
        .map(vocabulary -> arguments(vocabulary.getSimpleName(), vocabulary));

  }

  /// What a published dimension set actually holds.
  ///
  /// A [SymbolSet] publishes only its size — `10.3` requires nothing more of one, and `4.5` leaves
  /// the shape of a visiting operation to the projection. The dimension axis is therefore observed
  /// through the one construct that crosses it with something: the signal space of `4.6`, built by
  /// [SignSet#signals] from both published sets. Each cell is created from the member the *set*
  /// holds at that index, so reading a cell back reports the set's dimension rather than the one
  /// asked for.

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List< Object > crossed(
    final SignSet< ? > signs,
    final SymbolSet< ? > dimensions,
    final Class< ? > signal,
    final Class< ? > sign,
    final Class< ? > dimension
  ) throws Exception {

    final var constructor =
      canonical(signal);

    final var space =
      ((SignSet) signs).signals(
        dimensions,
        (s, d) -> {
          try {
            return constructor.newInstance(s, d);
          } catch (final ReflectiveOperationException failure) {
            return fail("could not build a " + signal.getSimpleName(), failure);
          }
        }
      );

    final var first =
      sign.getEnumConstants()[0];

    return
      Stream.of(dimension.getEnumConstants())
        .map(
          constant ->
            (Object)
              ((Signal< ?, ? >) space.get((Enum) first, (Enum) constant))
                .dimension()
        )
        .toList();

  }

  /// The declared constants of an enum, in declaration order.
  ///
  /// The constants themselves, not their names. A name is not a symbol: `8.4` makes symbols that
  /// share a lexeme across sets distinct symbols, so two alphabets can agree letter for letter and
  /// still be different alphabets. Enum equality is identity and no constant of one enum equals a
  /// constant of another, so comparing constants is what tells those apart.

  private static List< Object > members(
    final Class< ? > type
  ) {

    return
      Stream.of(type.getEnumConstants())
        .map(constant -> (Object) constant)
        .toList();

  }

  /// `Raw<A, B>` as the reflection layer renders it, for comparison against a declared type.

  private static String parameterized(
    final Class< ? > raw,
    final String... arguments
  ) {

    return
      parameterized(
        raw.getTypeName(),
        arguments
      );

  }

  /// The same, over a raw type already reduced to its name.

  private static String parameterized(
    final String raw,
    final String... arguments
  ) {

    return
      raw
        + Stream.of(arguments)
        .collect(
          Collectors.joining(", ", "<", ">")
        );

  }

  /// What a published sign set actually holds, read out of the set rather than off the enum.
  ///
  /// `4.5` requires a projection to publish its sets so an alphabet can be worked over without
  /// hard-coded member lists, and recommends it expose some means of visiting the members. In this
  /// projection that means is [SignSet#map(java.util.function.Function)], which walks the set and
  /// stores each projection at the index of the member it came from. Mapping each member to
  /// *itself* and then looking the enum's own constants up therefore hands back the constants the
  /// **set** holds at those indices — the same objects where the set is built over this enum, and
  /// another alphabet's constants where it is not, however alike the two read.
  /// The map is the instrument of the observation, not the thing observed.

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List< Object > projected(
    final SignSet< ? > signs,
    final Class< ? > sign
  ) {

    final var held =
      ((SignSet) signs).map(
        member -> member
      );

    return
      Stream.of(sign.getEnumConstants())
        .map(constant -> (Object) held.apply((Enum) constant))
        .toList();

  }

  /// Every vocabulary and template the artifact publishes.

  private static Stream< Arguments > published() {

    return
      Vocabularies.published()
        .stream()
        .map(vocabulary -> arguments(vocabulary.getSimpleName(), vocabulary));

  }

  /// Asserts that a published set is a public static final field declared at exactly `type`, and
  /// returns its value.

  private static Object publishedSet(
    final Class< ? > vocabulary,
    final String name,
    final String type
  ) throws Exception {

    final var declared =
      Vocabularies.field(vocabulary, name);

    assertNotNull(
      declared,
      () -> vocabulary.getSimpleName() + " publishes no " + name
    );

    final var modifiers =
      declared.getModifiers();

    assertTrue(
      Modifier.isPublic(modifiers)
        && Modifier.isStatic(modifiers)
        && Modifier.isFinal(modifiers),
      () -> vocabulary.getSimpleName() + "." + name + " must be public static final"
    );

    assertEquals(
      type,
      declared.getGenericType().getTypeName(),
      () -> vocabulary.getSimpleName() + "." + name + " is declared over the wrong type; a set "
        + "published raw, or over another vocabulary's symbols, is unusable to a consumer written "
        + "generically over an alphabet"
    );

    final var value =
      declared.get(null);

    assertNotNull(
      value,
      () -> vocabulary.getSimpleName() + "." + name + " must be populated"
    );

    return value;

  }

  /// The name of the single sign type variable a template factory declares, having asserted that it
  /// declares exactly one and bounds it as the specification requires of a sign type.

  private static String signVariable(
    final Method factory,
    final String owner
  ) {

    final var variables =
      factory.getTypeParameters();

    assertEquals(
      1,
      variables.length,
      () -> owner + "." + factory.getName() + " must be generic in exactly the caller's sign type"
    );

    final var variable =
      variables[0];

    assertEquals(
      List.of(
        parameterized(Enum.class, variable.getName()),
        Sign.class.getTypeName()
      ),
      Stream.of(variable.getBounds())
        .map(Type::getTypeName)
        .toList(),
      () -> owner + "." + factory.getName() + " does not bound " + variable.getName()
        + " to an enum sign type, so it would accept a sign set that is not an alphabet"
    );

    return
      variable.getName();

  }

  /// The member count of a published set, whichever set type it is.

  private static int size(
    final Object set
  ) throws Exception {

    return
      (int)
        set.getClass()
          .getMethod("size")
          .invoke(set);

  }

  /// The vocabulary templates: those taking the caller's sign set at materialization.

  private static Stream< Arguments > templates() {

    return
      Vocabularies.templates()
        .stream()
        .map(vocabulary -> arguments(vocabulary.getSimpleName(), vocabulary));

  }

  @SpecRef({"6.1", "6.2", "10.3"})
  @DisplayName("a concrete vocabulary constructs over the pipe and the conduit alone")
  @ParameterizedTest(name = "{0}")
  @MethodSource("concrete")
  void concreteFactoriesTakeTheEndpointAlone(
    final String name,
    final Class< ? > vocabulary
  ) {

    final var emission =
      Vocabularies.emission(vocabulary)
        .getTypeName();

    final var returned =
      Vocabularies.instrument(vocabulary);

    final var instrument =
      returned.getTypeName();

    // The instrument is read off the factory, so the factory returning it says nothing on its own.
    // What the surface actually claims is the protocol: an unqualified vocabulary hands back a
    // Signer, a qualified one a Signaler.

    final var protocol =
      Vocabularies.qualified(vocabulary)
        ? Signaler.class
        :Signer.class;

    assertTrue(
      protocol.isAssignableFrom(returned),
      () -> name + ".of returns " + returned.getSimpleName() + ", which is not a "
        + protocol.getSimpleName() + "; a " + (Vocabularies.qualified(vocabulary) ? "qualified":"unqualified")
        + " vocabulary emits through " + protocol.getSimpleName() + " (6.1, 6.2)"
    );

    assertSignature(
      Vocabularies.of(vocabulary),
      name,
      List.of(
        parameterized(Pipe.class, "? super " + emission)
      ),
      instrument
    );

    assertSignature(
      Vocabularies.pool(vocabulary),
      name,
      List.of(
        parameterized(Conduit.class, emission)
      ),
      parameterized(Pool.class, instrument)
    );

  }

  @SpecRef({"4.1", "4.5", "10.3"})
  @DisplayName("a concrete vocabulary publishes SIGNS as a sign set over its whole sign enum")
  @ParameterizedTest(name = "{0}")
  @MethodSource("concrete")
  void concretePublishesSigns(
    final String name,
    final Class< ? > vocabulary
  ) throws Exception {

    final var sign =
      Vocabularies.nested(vocabulary, "Sign");

    assertNotNull(
      sign,
      () -> name + " declares no Sign"
    );

    final var signs =
      (SignSet< ? >)
        publishedSet(
          vocabulary,
          SIGNS,
          parameterized(SignSet.class, sign.getTypeName())
        );

    assertEquals(
      sign.getEnumConstants().length,
      signs.size(),
      () -> name + ".SIGNS is not the size of " + name + ".Sign"
    );

    assertEquals(
      members(sign),
      projected(signs, sign),
      () -> name + ".SIGNS does not hold " + name + ".Sign; matching cardinality is not "
        + "membership, and a set built over a different alphabet of the same size indexes every "
        + "sign to the wrong slot while satisfying every count"
    );

  }

  @SpecRef({"4.1", "4.5", "10.3"})
  @DisplayName("DIMENSIONS is published exactly where a dimension enum is, over the whole of it")
  @ParameterizedTest(name = "{0}")
  @MethodSource("published")
  void dimensionsAccompanyTheDimensionEnum(
    final String name,
    final Class< ? > vocabulary
  ) throws Exception {

    final var dimension =
      Vocabularies.nested(vocabulary, "Dimension");

    if (dimension==null) {

      assertNull(
        Vocabularies.field(vocabulary, DIMENSIONS),
        () -> name + " publishes a DIMENSIONS without declaring a Dimension"
      );

      return;

    }

    final var dimensions =
      (SymbolSet< ? >)
        publishedSet(
          vocabulary,
          DIMENSIONS,
          parameterized(SymbolSet.class, dimension.getTypeName())
        );

    assertEquals(
      dimension.getEnumConstants().length,
      dimensions.size(),
      () -> name + ".DIMENSIONS is not the size of " + name + ".Dimension"
    );

    // The dimension axis is observed through the signal space, which is the only construct that
    // reads a SymbolSet's members. A template publishes no sign set, so the caller's stands in.

    final var signs =
      Vocabularies.template(vocabulary)
        ? Statuses.SIGNS
        :(SignSet< ? >) Vocabularies.field(vocabulary, SIGNS).get(null);

    final var signSource =
      Vocabularies.template(vocabulary)
        ? Statuses.Sign.class
        :Vocabularies.nested(vocabulary, "Sign");

    assertEquals(
      members(dimension),
      crossed(
        signs,
        dimensions,
        Vocabularies.nested(vocabulary, "Signal"),
        signSource,
        dimension
      ),
      () -> name + ".DIMENSIONS does not hold " + name + ".Dimension; matching cardinality is not "
        + "membership, and a set built over a different alphabet of the same size qualifies every "
        + "signal with the wrong dimension while satisfying every count"
    );

  }

  /// A vocabulary is discovered by its `pool` factory, so one publishing the sets of a vocabulary
  /// and no factory would not be discovered at all — and would escape every test in this suite
  /// driven from the inventory, silently. This is the check that the discriminator misses nothing.
  ///
  /// The independent inventory is built from the **published symbol sets**, because those are what
  /// `10.3` puts in a vocabulary's static surface: `SIGNS` for a vocabulary, `DIMENSIONS` for a
  /// qualified one or a template. Every type publishing either is claiming to be a vocabulary, and
  /// must therefore also publish both factories.
  ///
  /// Not from `Sign` enums. `10.3` constrains types that *are* vocabularies; it does not make every
  /// owner of an enum implementing `Sign` into one, so an auxiliary or non-published alphabet would
  /// be refused for existing — the same Java-shape constraint the classification above deliberately
  /// avoids. A sign inventory could not find a template missing its factories either, since a
  /// template has no signs of its own.

  @SpecRef("10.3")
  @DisplayName("every published symbol set belongs to a vocabulary with both factories")
  @Test
  void everyPublishedSetBelongsToAVocabulary() {

    final var claiming =
      Vocabularies.classes()
        .stream()
        .filter(candidate -> Modifier.isPublic(candidate.getModifiers()))
        .filter(
          candidate ->
            Vocabularies.publishedField(candidate, SIGNS)!=null
              || Vocabularies.publishedField(candidate, DIMENSIONS)!=null
        )
        .sorted(Comparator.comparing(Class::getName))
        .toList();

    assertEquals(
      Vocabularies.published(),
      claiming,
      "a type publishes a symbol set but no pool factory, so nothing driven from the vocabulary "
        + "inventory covers it"
    );

    for (final var vocabulary : claiming) {

      assertDoesNotThrow(
        () -> Vocabularies.of(vocabulary),
        () -> vocabulary.getSimpleName() + " publishes a symbol set but no required of factory"
      );

      assertDoesNotThrow(
        () -> Vocabularies.pool(vocabulary),
        () -> vocabulary.getSimpleName() + " publishes a symbol set but no required pool factory"
      );

    }

  }

  /// A template declares no `Sign` enum of its own.
  ///
  /// A Java-projection convention, not a portable requirement, so it carries no `@SpecRef`. What
  /// `7.7` and `10.3` require of a template is that it publish no sign set, and that is what
  /// classifies one here — a nested type happening to be named `Sign` is a shape the specification
  /// says nothing about, and refusing an implementation for having one would refuse it over a
  /// helper.

  @DisplayName("a template declares no Sign of its own")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templateDeclaresNoSign(
    final String name,
    final Class< ? > vocabulary
  ) {

    assertNull(
      Vocabularies.nested(vocabulary, "Sign"),
      () -> name + " declares a Sign enum, which a template has no use for: the caller supplies "
        + "the sign set at materialization, and two materializations are two different vocabularies"
    );

  }

  @SpecRef({"6.4", "7.7", "10.3"})
  @DisplayName("a template leads both factories with the caller's sign set")
  @ParameterizedTest(name = "{0}")
  @MethodSource("templates")
  void templateFactoriesLeadWithTheSignSet(
    final String name,
    final Class< ? > vocabulary
  ) {

    final var signal =
      Vocabularies.emission(vocabulary)
        .getTypeName();

    final var returned =
      Vocabularies.instrument(vocabulary);

    final var instrument =
      returned.getTypeName();

    assertTrue(
      Signaler.class.isAssignableFrom(returned),
      () -> name + ".of returns " + returned.getSimpleName()
        + ", which is not a Signaler; a template fixes a dimension set, so what it materializes "
        + "emits qualified signs (6.2, 7.7)"
    );

    // Each factory is generic in its own right, so each is read in terms of the variable it
    // declares. Requiring the two to spell it the same would fail a rename that changes no
    // contract: the shape is what is specified, not the letter it is written with.

    final var of =
      Vocabularies.of(vocabulary);

    final var ofSign =
      signVariable(of, name);

    assertSignature(
      of,
      name,
      List.of(
        parameterized(SignSet.class, ofSign),
        parameterized(Pipe.class, "? super " + parameterized(signal, ofSign))
      ),
      parameterized(instrument, ofSign)
    );

    final var pool =
      Vocabularies.pool(vocabulary);

    final var poolSign =
      signVariable(pool, name);

    assertSignature(
      pool,
      name,
      List.of(
        parameterized(SignSet.class, poolSign),
        parameterized(Conduit.class, parameterized(signal, poolSign))
      ),
      parameterized(Pool.class, parameterized(instrument, poolSign))
    );

  }

}
