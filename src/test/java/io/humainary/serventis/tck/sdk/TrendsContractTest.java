// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Trends.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Trends.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Trends] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class TrendsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("api.latency");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Trend trend;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    trend =
      Trends.pool(conduit)
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
  void testChaos() {

    trend.chaos();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CHAOS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCycle() {

    trend.cycle();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CYCLE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDrift() {

    trend.drift();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DRIFT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleTrendInstruments() {

    final var latencyName = CORTEX.name("api.latency");
    final var throughputName = CORTEX.name("api.throughput");

    final var conduit = circuit.conduit(Sign.class);
    final var trends = Trends.pool(conduit);
    final var latencyTrend = trends.get(latencyName);
    final var throughputTrend = trends.get(throughputName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    latencyTrend.stable();
    throughputTrend.drift();
    latencyTrend.spike();

    final var allCaptures = signCaptures.drain().toList();
    signCaptures.close();

    assertEquals(3, allCaptures.size());

    assertEquals(latencyName, allCaptures.get(0).subject().name());
    assertEquals(STABLE, allCaptures.get(0).emission());

    assertEquals(throughputName, allCaptures.get(1).subject().name());
    assertEquals(DRIFT, allCaptures.get(1).emission());

    assertEquals(latencyName, allCaptures.get(2).subject().name());
    assertEquals(SPIKE, allCaptures.get(2).emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPatternSequence() {

    // Simulate a process going from stable to problematic
    trend.stable();
    trend.drift();
    trend.spike();
    trend.chaos();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(STABLE, signs.get(0));
    assertEquals(DRIFT, signs.get(1));
    assertEquals(SPIKE, signs.get(2));
    assertEquals(CHAOS, signs.get(3));

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    trend.sign(STABLE);
    trend.sign(DRIFT);
    trend.sign(SPIKE);
    trend.sign(CYCLE);
    trend.sign(CHAOS);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(STABLE, signs.get(0));
    assertEquals(DRIFT, signs.get(1));
    assertEquals(SPIKE, signs.get(2));
    assertEquals(CYCLE, signs.get(3));
    assertEquals(CHAOS, signs.get(4));

  }

  @SpecRef({"4.1", "7.6"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, STABLE.ordinal());
    assertEquals(1, DRIFT.ordinal());
    assertEquals(2, SPIKE.ordinal());
    assertEquals(3, CYCLE.ordinal());
    assertEquals(4, CHAOS.ordinal());

  }

  @SpecRef({"4.5", "7.6"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(5, values.length);
    assertEquals(STABLE, values[0]);
    assertEquals(DRIFT, values[1]);
    assertEquals(SPIKE, values[2]);
    assertEquals(CYCLE, values[3]);
    assertEquals(CHAOS, values[4]);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSpike() {

    trend.spike();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SPIKE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStable() {

    trend.stable();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(STABLE, signs.getFirst());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    trend.stable();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(STABLE, capture.emission());

  }

}
