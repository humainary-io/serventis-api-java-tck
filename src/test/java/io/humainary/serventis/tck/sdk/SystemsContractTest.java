// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Systems.*;
import io.humainary.serventis.sdk.Systems.System;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Systems.Dimension.*;
import static io.humainary.serventis.sdk.Systems.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Systems] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class SystemsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("db.pool");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private System system;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    system =
      Systems.pool(conduit)
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

  // ========== INDIVIDUAL SIGN TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAlarm() {

    system.alarm(FLOW);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(ALARM, signals.getFirst().sign());
    assertEquals(FLOW, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAllDimensions() {

    system.normal(SPACE);
    system.normal(FLOW);
    system.normal(LINK);
    system.normal(TIME);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(SPACE, signals.get(0).dimension());
    assertEquals(FLOW, signals.get(1).dimension());
    assertEquals(LINK, signals.get(2).dimension());
    assertEquals(TIME, signals.get(3).dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAllSignDimensionCombinations() {

    // Test all 16 combinations (4 signs × 4 dimensions)
    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        system.signal(sign, dimension);
      }
    }

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(16, signals.size());

    var index = 0;
    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        assertEquals(new Signal(sign, dimension), signals.get(index++));
      }
    }

  }

  @SpecRef({"4.1", "7.5"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, SPACE.ordinal());
    assertEquals(1, FLOW.ordinal());
    assertEquals(2, LINK.ordinal());
    assertEquals(3, TIME.ordinal());

  }

  // ========== ALL DIMENSIONS TESTS ==========

  @SpecRef({"4.5", "7.5"})
  @Test
  void testDimensionEnumValues() {

    assertEquals(4, Dimension.values().length);

  }

  // ========== SEVERITY PROGRESSION TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFault() {

    system.fault(LINK);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(FAULT, signals.getFirst().sign());
    assertEquals(LINK, signals.getFirst().dimension());

  }

  // ========== SIGNAL METHOD TEST ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testLimit() {

    system.limit(TIME);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(LIMIT, signals.getFirst().sign());
    assertEquals(TIME, signals.getFirst().dimension());

  }

  // ========== ALL COMBINATIONS TEST ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultiSystemMonitoring() {

    final var poolName = CORTEX.name("db.pool");
    final var cacheName = CORTEX.name("cache.redis");

    final var conduit = circuit.conduit(Signal.class);
    final var systems = Systems.pool(conduit);
    final var poolSystem = systems.get(poolName);
    final var cacheSystem = systems.get(cacheName);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    // Pool space is tight, cache link is down
    poolSystem.limit(SPACE);
    cacheSystem.fault(LINK);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(2, allSignals.size());

    // Verify subjects
    assertEquals(poolName, allSignals.get(0).subject().name());
    assertEquals(cacheName, allSignals.get(1).subject().name());

    // Verify emissions
    assertEquals(LIMIT, allSignals.get(0).emission().sign());
    assertEquals(SPACE, allSignals.get(0).emission().dimension());
    assertEquals(FAULT, allSignals.get(1).emission().sign());
    assertEquals(LINK, allSignals.get(1).emission().dimension());

  }

  // ========== SIGNAL CACHING TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNormal() {

    system.normal(SPACE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(NORMAL, signals.getFirst().sign());
    assertEquals(SPACE, signals.getFirst().dimension());

  }

  // ========== ENUM STABILITY TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSeverityProgression() {

    // Test progression: NORMAL → LIMIT → ALARM → FAULT
    system.normal(SPACE);
    system.limit(SPACE);
    system.alarm(SPACE);
    system.fault(SPACE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(NORMAL, signals.get(0).sign());
    assertEquals(LIMIT, signals.get(1).sign());
    assertEquals(ALARM, signals.get(2).sign());
    assertEquals(FAULT, signals.get(3).sign());

  }

  @SpecRef({"4.1", "7.5"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, NORMAL.ordinal());
    assertEquals(1, LIMIT.ordinal());
    assertEquals(2, ALARM.ordinal());
    assertEquals(3, FAULT.ordinal());

  }

  @SpecRef({"4.5", "7.5"})
  @Test
  void testSignEnumValues() {

    assertEquals(4, Sign.values().length);

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    system.signal(ALARM, SPACE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(ALARM, signals.getFirst().sign());
    assertEquals(SPACE, signals.getFirst().dimension());

  }

  // ========== SUBJECT TESTS ==========

  @Test
  void testSignalCaching() {

    // Emit same signal twice
    system.alarm(SPACE);
    system.alarm(SPACE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());

    // Verify same Signal instance is reused (cached)
    assertSame(signals.get(0), signals.get(1));

  }

  // ========== MULTI-SYSTEM TEST ==========

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    system.alarm(FLOW);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(ALARM, capture.emission().sign());
    assertEquals(FLOW, capture.emission().dimension());

  }

}
