// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Statuses.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Statuses.Dimension.*;
import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Statuses] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class StatusesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("service.database");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Status status;

  private void emitSign(
    final Sign sign,
    final Dimension dimension
  ) {

    switch (sign) {
      case CONVERGING -> status.converging(dimension);
      case STABLE -> status.stable(dimension);
      case DIVERGING -> status.diverging(dimension);
      case ERRATIC -> status.erratic(dimension);
      case DEGRADED -> status.degraded(dimension);
      case DEFECTIVE -> status.defective(dimension);
      case DOWN -> status.down(dimension);
    }

  }

  // ========== INDIVIDUAL SIGN TESTS ==========

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    status =
      Statuses.pool(conduit)
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
  void testConfidenceProgression() {

    // Test progression from TENTATIVE → MEASURED → CONFIRMED
    status.degraded(TENTATIVE);
    status.degraded(MEASURED);
    status.degraded(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(DEGRADED, signals.get(0).sign());
    assertEquals(TENTATIVE, signals.get(0).dimension());
    assertEquals(DEGRADED, signals.get(1).sign());
    assertEquals(MEASURED, signals.get(1).dimension());
    assertEquals(DEGRADED, signals.get(2).sign());
    assertEquals(CONFIRMED, signals.get(2).dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testConverging() {

    status.converging(MEASURED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(CONVERGING, signals.getFirst().sign());
    assertEquals(MEASURED, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDefective() {

    status.defective(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DEFECTIVE, signals.getFirst().sign());
    assertEquals(CONFIRMED, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDegradationSequence() {

    // Test operational degradation: STABLE → DIVERGING → DEGRADED → DEFECTIVE → DOWN
    status.stable(CONFIRMED);
    status.diverging(TENTATIVE);
    status.diverging(MEASURED);
    status.degraded(MEASURED);
    status.defective(CONFIRMED);
    status.down(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signals.size());
    assertEquals(STABLE, signals.get(0).sign());
    assertEquals(DIVERGING, signals.get(1).sign());
    assertEquals(DEGRADED, signals.get(3).sign());
    assertEquals(DEFECTIVE, signals.get(4).sign());
    assertEquals(DOWN, signals.get(5).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDegraded() {

    status.degraded(TENTATIVE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DEGRADED, signals.getFirst().sign());
    assertEquals(TENTATIVE, signals.getFirst().dimension());

  }

  @SpecRef({"4.1", "7.1"})
  @Test
  void testDimensionEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, TENTATIVE.ordinal());
    assertEquals(1, MEASURED.ordinal());
    assertEquals(2, CONFIRMED.ordinal());

  }

  // ========== DIMENSION PROGRESSION TESTS ==========

  @SpecRef({"4.5", "7.1"})
  @Test
  void testDimensionEnumValues() {

    final var values = Dimension.values();

    assertEquals(3, values.length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDiverging() {

    status.diverging(MEASURED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DIVERGING, signals.getFirst().sign());
    assertEquals(MEASURED, signals.getFirst().dimension());

  }

  // ========== PATTERN TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDown() {

    status.down(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DOWN, signals.getFirst().sign());
    assertEquals(CONFIRMED, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testErratic() {

    status.erratic(TENTATIVE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(ERRATIC, signals.getFirst().sign());
    assertEquals(TENTATIVE, signals.getFirst().dimension());

  }

  // ========== SIGNAL CACHING TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultiServiceMonitoring() {

    // Simulate statusing multiple services
    final var dbName = CORTEX.name("service.database");
    final var apiName = CORTEX.name("service.api");

    final var conduit = circuit.conduit(Signal.class);
    final var statuses = Statuses.pool(conduit);
    final var dbMonitor = statuses.get(dbName);
    final var apiMonitor = statuses.get(apiName);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    // Database degrades
    dbMonitor.stable(CONFIRMED);
    dbMonitor.degraded(MEASURED);

    // API remains stable
    apiMonitor.stable(CONFIRMED);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(3, allSignals.size());

    // Verify subjects
    assertEquals(dbName, allSignals.get(0).subject().name());
    assertEquals(dbName, allSignals.get(1).subject().name());
    assertEquals(apiName, allSignals.get(2).subject().name());

    // Verify emissions
    assertEquals(STABLE, allSignals.get(0).emission().sign());
    assertEquals(DEGRADED, allSignals.get(1).emission().sign());
    assertEquals(STABLE, allSignals.get(2).emission().sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRecoverySequence() {

    // Test recovery: DEFECTIVE → DEGRADED → CONVERGING → STABLE
    status.defective(CONFIRMED);
    status.degraded(MEASURED);
    status.converging(MEASURED);
    status.stable(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(DEFECTIVE, signals.get(0).sign());
    assertEquals(DEGRADED, signals.get(1).sign());
    assertEquals(CONVERGING, signals.get(2).sign());
    assertEquals(STABLE, signals.get(3).sign());

  }

  // ========== ENUM STABILITY TESTS ==========

  @SpecRef({"4.1", "7.1"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, CONVERGING.ordinal());
    assertEquals(1, STABLE.ordinal());
    assertEquals(2, DIVERGING.ordinal());
    assertEquals(3, ERRATIC.ordinal());
    assertEquals(4, DEGRADED.ordinal());
    assertEquals(5, DEFECTIVE.ordinal());
    assertEquals(6, DOWN.ordinal());

  }

  @SpecRef({"4.5", "7.1"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(7, values.length);

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    status.signal(CONVERGING, TENTATIVE);
    status.signal(CONVERGING, MEASURED);
    status.signal(CONVERGING, CONFIRMED);
    status.signal(STABLE, TENTATIVE);
    status.signal(STABLE, MEASURED);
    status.signal(STABLE, CONFIRMED);
    status.signal(DIVERGING, TENTATIVE);
    status.signal(DIVERGING, MEASURED);
    status.signal(DIVERGING, CONFIRMED);
    status.signal(ERRATIC, TENTATIVE);
    status.signal(ERRATIC, MEASURED);
    status.signal(ERRATIC, CONFIRMED);
    status.signal(DEGRADED, TENTATIVE);
    status.signal(DEGRADED, MEASURED);
    status.signal(DEGRADED, CONFIRMED);
    status.signal(DEFECTIVE, TENTATIVE);
    status.signal(DEFECTIVE, MEASURED);
    status.signal(DEFECTIVE, CONFIRMED);
    status.signal(DOWN, TENTATIVE);
    status.signal(DOWN, MEASURED);
    status.signal(DOWN, CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(21, signals.size());
    assertEquals(new Signal(CONVERGING, TENTATIVE), signals.get(0));
    assertEquals(new Signal(CONVERGING, MEASURED), signals.get(1));
    assertEquals(new Signal(CONVERGING, CONFIRMED), signals.get(2));
    assertEquals(new Signal(STABLE, TENTATIVE), signals.get(3));
    assertEquals(new Signal(STABLE, MEASURED), signals.get(4));
    assertEquals(new Signal(STABLE, CONFIRMED), signals.get(5));
    assertEquals(new Signal(DIVERGING, TENTATIVE), signals.get(6));
    assertEquals(new Signal(DIVERGING, MEASURED), signals.get(7));
    assertEquals(new Signal(DIVERGING, CONFIRMED), signals.get(8));
    assertEquals(new Signal(ERRATIC, TENTATIVE), signals.get(9));
    assertEquals(new Signal(ERRATIC, MEASURED), signals.get(10));
    assertEquals(new Signal(ERRATIC, CONFIRMED), signals.get(11));
    assertEquals(new Signal(DEGRADED, TENTATIVE), signals.get(12));
    assertEquals(new Signal(DEGRADED, MEASURED), signals.get(13));
    assertEquals(new Signal(DEGRADED, CONFIRMED), signals.get(14));
    assertEquals(new Signal(DEFECTIVE, TENTATIVE), signals.get(15));
    assertEquals(new Signal(DEFECTIVE, MEASURED), signals.get(16));
    assertEquals(new Signal(DEFECTIVE, CONFIRMED), signals.get(17));
    assertEquals(new Signal(DOWN, TENTATIVE), signals.get(18));
    assertEquals(new Signal(DOWN, MEASURED), signals.get(19));
    assertEquals(new Signal(DOWN, CONFIRMED), signals.get(20));

  }

  @Test
  void testSignalCaching() {

    // Emit same signal twice
    status.stable(CONFIRMED);
    status.stable(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());

    // Verify same Signal instance is reused (cached)
    assertSame(signals.get(0), signals.get(1));

  }

  // ========== SUBJECT TESTS ==========

  @Test
  void testSignalCachingAllCombinations() {

    // Test that all 21 combinations (7 signs × 3 dimensions) use cached instances
    final var firstPass = new Signal[7][3];
    final var secondPass = new Signal[7][3];

    final var signs = Sign.values();
    final var dimensions = Dimension.values();

    // First pass - collect signals
    for (final var sign : signs) {
      for (final var dimension : dimensions) {
        emitSign(sign, dimension);
      }
    }

    var captured = captures.drain().toList();
    var index = 0;
    for (final var sign : signs) {
      for (final var dimension : dimensions) {
        firstPass[sign.ordinal()][dimension.ordinal()] = captured.get(index++).emission();
      }
    }

    // Second pass - collect signals again
    for (final var sign : signs) {
      for (final var dimension : dimensions) {
        emitSign(sign, dimension);
      }
    }

    captured = captures.drain().toList();
    index = 0;
    for (final var sign : signs) {
      for (final var dimension : dimensions) {
        secondPass[sign.ordinal()][dimension.ordinal()] = captured.get(index++).emission();
      }
    }

    // Verify all instances are cached (same reference)
    for (final var sign : signs) {
      for (final var dimension : dimensions) {
        assertSame(
          firstPass[sign.ordinal()][dimension.ordinal()],
          secondPass[sign.ordinal()][dimension.ordinal()],
          "Signal should be cached for " + sign + " × " + dimension
        );
      }
    }

  }

  // ========== HELPER METHODS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStable() {

    status.stable(CONFIRMED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(CONFIRMED, signals.getFirst().dimension());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    status.stable(CONFIRMED);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(STABLE, capture.emission().sign());
    assertEquals(CONFIRMED, capture.emission().dimension());

  }

}
