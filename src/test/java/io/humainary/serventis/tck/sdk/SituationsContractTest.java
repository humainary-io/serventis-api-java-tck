// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Situations.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Situations.Dimension.*;
import static io.humainary.serventis.sdk.Situations.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Situation] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
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

    assertEquals(
      1L, captures
        .drain()
        .filter(capture -> capture.emission().sign()==sign)
        .filter(capture -> capture.emission().dimension()==dimension)
        .filter(capture -> capture.subject().name()==NAME)
        .count()
    );

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

    captures.close();
    circuit
      .closeAwait();

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

    assertEquals(
      3L,
      captures
        .drain()
        .count()
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

    // Test direct signal() method for all sign and dimension combinations
    situation.signal(NORMAL, CONSTANT);
    situation.signal(WARNING, VARIABLE);
    situation.signal(CRITICAL, VOLATILE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(NORMAL, signals.get(0).sign());
    assertEquals(CONSTANT, signals.get(0).dimension());
    assertEquals(WARNING, signals.get(1).sign());
    assertEquals(VARIABLE, signals.get(1).dimension());
    assertEquals(CRITICAL, signals.get(2).sign());
    assertEquals(VOLATILE, signals.get(2).dimension());

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
