// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Situations.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.*;

import static io.humainary.serventis.sdk.Situations.Dimension.*;
import static io.humainary.serventis.sdk.Situations.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Situation] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.15.1/SPEC.md")
final class SituationsContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("situation.1");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Situation situation;

  private void assertSignal(
    final Sign sign,
    final Dimension dimension
  ) {

    // Every capture, not just the matching ones: a named operation must emit
    // exactly the one signal it names (SPEC §6.3, §6.5).
    final var emitted = captures.drain().toList();

    assertEquals(1, emitted.size(), "exactly one signal per operation");
    assertSame(sign, emitted.getFirst().emission().sign());
    assertSame(dimension, emitted.getFirst().emission().dimension());
    assertSame(NAME, emitted.getFirst().subject().name());

  }

  @BeforeEach
  void setup() {

    circuit =
      cortex().circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    situation =
      Situations.pool(conduit)
        .get(NAME);

    captures =
      CaptureBuffer.of(circuit, conduit);

  }

  @AfterEach
  void teardown() {

    circuit
      .closeAwait();

  }

  /// Every named operation, with every dimension, emits exactly the signal it
  /// names, in order — so a named operation that ignored its dimension, or
  /// emitted another sign, fails here.
  @SpecRef({"6.3", "6.5"})
  @Test
  void testConvenienceMethodsEmitTheirSign() {

    final Map< Sign, Consumer< Dimension > > methods =
      new EnumMap<>(Sign.class);

    methods.put(NORMAL, situation::normal);
    methods.put(WARNING, situation::warning);
    methods.put(CRITICAL, situation::critical);

    assertEquals(Sign.values().length, methods.size());

    methods.forEach(
      (sign, method) -> {
        for (final var dimension : Dimension.values()) {
          method.accept(dimension);
        }
      }
    );

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(Sign.values().length * Dimension.values().length, signals.size());

    var index = 0;

    for (final var sign : methods.keySet()) {
      for (final var dimension : Dimension.values()) {
        assertEquals(new Signal(sign, dimension), signals.get(index++));
      }
    }

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCritical() {

    situation.critical(CONSTANT);

    assertSignal(
      CRITICAL,
      CONSTANT
    );

  }

  /// Tests that [Dimension] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Dimension] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "7.2"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, Dimension.CONSTANT.ordinal());
    assertEquals(1, Dimension.VARIABLE.ordinal());
    assertEquals(2, Dimension.VOLATILE.ordinal());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    situation.normal(CONSTANT);
    situation.warning(VARIABLE);
    situation.critical(VOLATILE);

    // The whole sequence, not a count: a wrong sign, a swapped dimension or a
    // reordering (§6.5) would keep the count and change this.
    assertEquals(
      java.util.List.of(
        new Signal(Sign.NORMAL, CONSTANT),
        new Signal(Sign.WARNING, VARIABLE),
        new Signal(Sign.CRITICAL, VOLATILE)
      ),
      captures.drainEmissions().toList()
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNormal() {

    situation.normal(CONSTANT);

    assertSignal(
      NORMAL,
      CONSTANT
    );

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "7.2"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, Sign.NORMAL.ordinal());
    assertEquals(1, Sign.WARNING.ordinal());
    assertEquals(2, Sign.CRITICAL.ordinal());

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Every sign and dimension combination, in order
    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        situation.signal(sign, dimension);
      }
    }

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(Sign.values().length * Dimension.values().length, signals.size());

    var i = 0;

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        assertEquals(sign, signals.get(i).sign());
        assertEquals(dimension, signals.get(i).dimension());
        i++;
      }
    }

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    situation.normal(CONSTANT);

    assertEquals(
      NAME,
      captures
        .drain()
        .map(Capture::subject)
        .map(Subject::name)
        .findFirst()
        .orElseThrow()
    );

  }

  /// Tests the variability dimension spectrum from CONSTANT to VOLATILE.

  @SpecRef({"6.3", "6.5"})
  @Test
  void testVariabilityDimensionSpectrum() {

    // Test all three variability dimensions
    situation.critical(CONSTANT);
    situation.critical(VARIABLE);
    situation.critical(VOLATILE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(CONSTANT, signals.get(0).dimension());
    assertEquals(VARIABLE, signals.get(1).dimension());
    assertEquals(VOLATILE, signals.get(2).dimension());

    // All should have CRITICAL sign
    signals.forEach(signal ->
      assertEquals(CRITICAL, signal.sign())
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testWarning() {

    situation.warning(VARIABLE);

    assertSignal(
      WARNING,
      VARIABLE
    );

  }

}
