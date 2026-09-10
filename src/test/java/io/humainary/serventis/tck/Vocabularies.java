// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck;

import io.humainary.serventis.sdk.*;
import io.humainary.substrates.api.Substrates.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

/// The published Serventis surface, read off the API artifact rather than listed by hand.
///
/// Several contract tests need an inventory: of vocabularies, of the qualified ones among them, of
/// the templates, of the `Signal` records. A list maintained by hand is the wrong shape for all of
/// them. A vocabulary added to the API and forgotten in one of those lists is a vocabulary that
/// silently escapes that test — its pool never checked, its absent arguments never rejected, its
/// published sets never inspected — and nothing about the omission fails anything. The artifact is
/// therefore scanned once, here, and the tests are driven from what it actually publishes.
///
/// A **vocabulary** is identified by the static surface the specification requires of one (`10.3`):
/// a public static `pool` factory. That is the discriminator rather than a nested `Sign` enum,
/// because a vocabulary template (`7.7`) publishes no sign set and a `Sign` test would miss it.
///
/// Nothing here asserts anything. It is the inventory the assertions are made over, and it carries
/// no `@SpecDoc` for that reason.
/// @author William David Louth
/// @since 3.0

final class Vocabularies {

  /// The published sign set a concrete vocabulary declares and a template does not.

  static final String SIGNS = "SIGNS";
  /// The published dimension set a qualified vocabulary declares.

  static final String DIMENSIONS = "DIMENSIONS";
  /// The package every published Serventis type lives under.

  private static final String ROOT = "io.humainary.serventis.";
  private static final List< Class< ? > > CLASSES = scan();

  private static final List< Class< ? > > PUBLISHED =
    CLASSES.stream()
      .filter(Vocabularies::publishesPool)
      .toList();

  private Vocabularies() {
  }

  /// The binary name of the class a path under the artifact root holds.

  private static String binaryName(
    final Path relative
  ) {

    final var joined =
      StreamSupport.stream(relative.spliterator(), false)
        .map(Path::toString)
        .collect(joining("."));

    return
      joined.substring(
        0,
        joined.length() - ".class".length()
      );

  }

  /// Every class the API artifact publishes, in name order.

  static List< Class< ? > > classes() {

    return CLASSES;

  }

  /// The concrete vocabularies: those publishing a sign set of their own.

  static List< Class< ? > > concrete() {

    return
      PUBLISHED.stream()
        .filter(vocabulary -> !template(vocabulary))
        .toList();

  }

  /// The type a vocabulary's conduit carries: its `Signal` where it has one, otherwise its `Sign`.

  static Class< ? > emission(
    final Class< ? > vocabulary
  ) {

    final var signal =
      nested(vocabulary, "Signal");

    return
      signal!=null
        ? signal
        :nested(vocabulary, "Sign");

  }

  /// The factory of the given name with the exact signature `10.3` requires of this vocabulary:
  /// the endpoint alone where it publishes a sign set, the caller's sign set ahead of the endpoint
  /// where it is a template.
  ///
  /// One signature, not a choice between the two. Accepting either would make a concrete
  /// vocabulary that also published a `SignSet`-leading convenience overload ambiguous, and
  /// ambiguity here is refused — so a legal addition would fail the suite before the required
  /// factory was ever looked at. Classification decides which signature is required; this only
  /// finds it.
  ///
  /// Matching by signature rather than by name is what leaves room for the overloads. `10.3`
  /// requires *a* factory of the stated shape; it does not make the name exclusive, and a
  /// projection may publish an `of` taking a receptor or a `pool` taking a name beside it.

  private static Method factory(
    final Class< ? > vocabulary,
    final String name,
    final Class< ? > endpoint
  ) {

    final var signature =
      required(vocabulary, endpoint);

    final var matching =
      Stream.of(vocabulary.getDeclaredMethods())
        .filter(method -> method.getName().equals(name))
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> Modifier.isStatic(method.getModifiers()))
        .filter(method -> Arrays.equals(method.getParameterTypes(), signature))
        .toList();

    if (matching.size()!=1) {

      throw new IllegalStateException(
        vocabulary.getName() + " declares " + matching.size() + " public static " + name
          + Arrays.stream(signature)
          .map(Class::getSimpleName)
          .collect(joining(", ", "(", ")"))
          + " factories; the required surface (10.3) is exactly one"
      );

    }

    return
      matching.getFirst();

  }

  /// The declared field of the given name, whatever its modifiers, or `null` where there is none.
  ///
  /// Declared rather than published, because the surface test needs to see a field that is present
  /// but wrong in order to say so.

  static Field field(
    final Class< ? > vocabulary,
    final String name
  ) {

    for (final var declared : vocabulary.getDeclaredFields()) {

      if (declared.getName().equals(name)) {

        return declared;

      }

    }

    return null;

  }

  /// The instrument type a vocabulary publishes: what its `of` factory returns.
  ///
  /// `10.3` constrains the instrument a factory hands back, not how many types in the vocabulary
  /// happen to be assignable to a protocol. Counting the nested types would fail a vocabulary for a
  /// private adapter or a package-private helper implementation — details the specification says
  /// nothing about, and which a projection is free to have. The factory names the one type that is
  /// surface, so the factory is what is read.
  ///
  /// That the returned type *is* a Signer or a Signaler is a claim, not an assumption, and is
  /// asserted where the rest of the surface is.

  static Class< ? > instrument(
    final Class< ? > vocabulary
  ) {

    return
      of(vocabulary)
        .getReturnType();

  }

  /// The classes among the class files under `root`, in name order.

  private static List< Class< ? > > load(
    final Stream< Path > entries,
    final Path root
  ) {

    return
      entries
        .filter(Files::isRegularFile)
        .filter(path -> path.getFileName().toString().endsWith(".class"))
        .map(path -> binaryName(root.relativize(path)))
        .filter(name -> name.startsWith(ROOT))
        .sorted()
        .map(Vocabularies::load)
        .collect(toUnmodifiableList());

  }

  private static Class< ? > load(
    final String name
  ) {

    try {

      return
        Class.forName(
          name,
          false,
          SignSet.class.getClassLoader()
        );

    } catch (final ClassNotFoundException | LinkageError failure) {

      throw new IllegalStateException(
        "could not load " + name + " from the Serventis API artifact",
        failure
      );

    }

  }

  /// A vocabulary's nested type of the given simple name, or `null` where it declares none.

  static Class< ? > nested(
    final Class< ? > vocabulary,
    final String name
  ) {

    for (final var declared : vocabulary.getDeclaredClasses()) {

      if (declared.getSimpleName().equals(name)) {

        return declared;

      }

    }

    return null;

  }

  /// The instrument factory every vocabulary publishes.

  static Method of(
    final Class< ? > vocabulary
  ) {

    return
      factory(vocabulary, "of", Pipe.class);

  }

  /// The pooling factory every vocabulary publishes.

  static Method pool(
    final Class< ? > vocabulary
  ) {

    return
      factory(vocabulary, "pool", Conduit.class);

  }

  /// Every vocabulary and vocabulary template the API artifact publishes, in name order.

  static List< Class< ? > > published() {

    return PUBLISHED;

  }

  /// The public field of the given name, or `null` where the vocabulary publishes none.

  static Field publishedField(
    final Class< ? > vocabulary,
    final String name
  ) {

    final var declared =
      field(vocabulary, name);

    return
      declared!=null && Modifier.isPublic(declared.getModifiers())
        ? declared
        :null;

  }

  /// Whether a class publishes a pooling factory of either required shape.
  ///
  /// Either, here, because this runs before anything is classified — it is what decides that a
  /// class is a vocabulary at all. Selection afterwards takes the one shape the classification
  /// requires.

  private static boolean publishesPool(
    final Class< ? > candidate
  ) {

    return
      Modifier.isPublic(candidate.getModifiers())
        && Stream.of(candidate.getDeclaredMethods())
        .filter(method -> method.getName().equals("pool"))
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> Modifier.isStatic(method.getModifiers()))
        .anyMatch(
          method -> {
            final var parameters = method.getParameterTypes();
            return
              switch (parameters.length) {
                case 1 -> parameters[0]==Conduit.class;
                case 2 -> parameters[0]==SignSet.class && parameters[1]==Conduit.class;
                default -> false;
              };
          }
        );

  }

  /// Whether a vocabulary is qualified — it publishes a dimension set, so its instrument signals.

  static boolean qualified(
    final Class< ? > vocabulary
  ) {

    return
      nested(vocabulary, "Dimension")!=null;

  }

  /// The parameter list `10.3` states for this vocabulary's factory over the given endpoint.

  private static Class< ? >[] required(
    final Class< ? > vocabulary,
    final Class< ? > endpoint
  ) {

    return
      template(vocabulary)
        ? new Class< ? >[]{SignSet.class, endpoint}
        :new Class< ? >[]{endpoint};

  }

  /// Reads the artifact holding the API and loads every class it publishes under [#ROOT].
  ///
  /// The code source is the API jar the TCK resolves, or its class directory in a build that has
  /// not packaged one yet; both are walked the same way. Classes are loaded without initializing
  /// them — the scan reads shapes, it does not run the API.

  private static List< Class< ? > > scan() {

    final var source =
      SignSet.class
        .getProtectionDomain()
        .getCodeSource();

    if (source==null) {

      throw new IllegalStateException(
        "the Serventis API has no code source to scan; the TCK reads its inventory from the artifact"
      );

    }

    try {

      final var location =
        Path.of(
          source
            .getLocation()
            .toURI()
        );

      if (Files.isDirectory(location)) {

        try (final var entries = Files.walk(location)) {

          return
            load(entries, location);

        }

      }

      try (final var artifact = FileSystems.newFileSystem(location)) {

        final var root =
          artifact
            .getRootDirectories()
            .iterator()
            .next();

        try (final var entries = Files.walk(root)) {

          return
            load(entries, root);

        }

      }

    } catch (final Exception failure) {

      throw new IllegalStateException(
        "could not read the Serventis API artifact at " + source.getLocation(),
        failure
      );

    }

  }

  /// Whether a vocabulary is a template — it fixes no sign set of its own, so the caller supplies
  /// one at materialization (`7.7`).
  ///
  /// Read from the absence of the published `SIGNS`, which is the discriminator the specification
  /// itself uses: `7.7` says a template MUST NOT publish a sign set, and `10.3` lists `SIGNS` in
  /// the static surface of a vocabulary and omits it from a template's. Nothing else is a
  /// classifier. A nested type named `Sign` is a Java-shape convention, and rejecting one would
  /// refuse an implementation over a helper the specification says nothing about; a factory cannot
  /// classify either, since which factory shape is *required* is what the classification decides,
  /// and a concrete vocabulary may publish a `SignSet`-leading convenience overload beside its
  /// required one.
  ///
  /// **Published**, specifically — a field that is public. What `10.3` requires is a name a
  /// consumer can reach, and a private helper that happens to be called `SIGNS` is not that: taking
  /// any declared field would classify a conforming template as concrete over an implementation
  /// detail. Whether a public one has the rest of the required shape — static, final, and a
  /// `SignSet` over the vocabulary's own signs — is a defect in a vocabulary rather than a question
  /// about which kind it is, and is reported by the surface test instead.

  static boolean template(
    final Class< ? > vocabulary
  ) {

    return
      publishedField(vocabulary, SIGNS)==null;

  }

  /// The vocabulary templates (`7.7`): those taking the caller's sign set at materialization.

  static List< Class< ? > > templates() {

    return
      PUBLISHED.stream()
        .filter(Vocabularies::template)
        .toList();

  }

}
