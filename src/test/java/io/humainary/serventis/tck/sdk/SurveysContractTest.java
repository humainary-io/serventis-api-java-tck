// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Surveys.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.serventis.sdk.Surveys.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class SurveysContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("cluster.health");

  private Circuit circuit;
  private CaptureBuffer< Signal< Statuses.Sign > > captures;
  private Survey< Statuses.Sign > survey;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      (Conduit< Signal< Statuses.Sign > >) (Conduit< ? >)
        circuit.conduit(
          Signal.class
        );

    survey =
      Surveys.pool(Statuses.SIGNS, conduit)
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

  // ========== INDIVIDUAL DIMENSION TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testAgreementProgression() {

    // Simulate agreement improving over time
    survey.signal(DEGRADED, DIVIDED);    // Initially split
    survey.signal(DEGRADED, MAJORITY);   // Converging to majority
    survey.signal(DEGRADED, UNANIMOUS);  // Full consensus

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(DEGRADED, signals.get(0).sign());
    assertEquals(DIVIDED, signals.get(0).dimension());
    assertEquals(DEGRADED, signals.get(1).sign());
    assertEquals(MAJORITY, signals.get(1).dimension());
    assertEquals(DEGRADED, signals.get(2).sign());
    assertEquals(UNANIMOUS, signals.get(2).dimension());

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testClusterHealthScenario() {

    // Realistic scenario: 5-node cluster monitoring shared resource
    // Time 0: All nodes report STABLE
    survey.signal(STABLE, UNANIMOUS);

    // Time 1: 3 nodes see DEGRADED, 2 still see STABLE
    survey.signal(DEGRADED, MAJORITY);

    // Time 2: 4 nodes see DEGRADED, 1 sees DEFECTIVE
    survey.signal(DEGRADED, MAJORITY);

    // Time 3: All nodes agree on DEGRADED
    survey.signal(DEGRADED, UNANIMOUS);

    // Time 4: Recovery - split between DEGRADED and CONVERGING
    survey.signal(CONVERGING, DIVIDED);

    // Time 5: Most see CONVERGING
    survey.signal(CONVERGING, MAJORITY);

    // Time 6: All agree STABLE again
    survey.signal(STABLE, UNANIMOUS);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signals.size());

    // Verify the progression
    assertEquals(STABLE, signals.get(0).sign());
    assertEquals(UNANIMOUS, signals.get(0).dimension());

    assertEquals(DEGRADED, signals.get(3).sign());
    assertEquals(UNANIMOUS, signals.get(3).dimension());

    assertEquals(STABLE, signals.get(6).sign());
    assertEquals(UNANIMOUS, signals.get(6).dimension());

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testCollectiveStatusChanges() {

    // Simulate collective assessment changing over time
    survey.signal(STABLE, UNANIMOUS);     // All agree stable
    survey.signal(DIVERGING, MAJORITY);   // Most see diverging
    survey.signal(DEGRADED, UNANIMOUS);   // All agree degraded

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(STABLE, signals.get(0).sign());
    assertEquals(UNANIMOUS, signals.get(0).dimension());
    assertEquals(DIVERGING, signals.get(1).sign());
    assertEquals(MAJORITY, signals.get(1).dimension());
    assertEquals(DEGRADED, signals.get(2).sign());
    assertEquals(UNANIMOUS, signals.get(2).dimension());

  }

  // ========== ENUM STABILITY TESTS ==========

  @SpecRef({"4.1", "7.8"})
  @Test
  void testDimensionEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    // Spectrum: DIVIDED → MAJORITY → UNANIMOUS (increasing agreement)
    assertEquals(0, DIVIDED.ordinal());
    assertEquals(1, MAJORITY.ordinal());
    assertEquals(2, UNANIMOUS.ordinal());

  }

  @SpecRef({"4.5", "7.8"})
  @Test
  void testDimensionEnumValues() {

    final var values = Dimension.values();

    assertEquals(3, values.length);

  }

  // ========== PATTERN TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testDivided() {

    survey.signal(DEGRADED, DIVIDED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DEGRADED, signals.getFirst().sign());
    assertEquals(DIVIDED, signals.getFirst().dimension());

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testDividedOutcomes() {

    // Simulate divided assessments across different signs
    survey.signal(STABLE, DIVIDED);
    survey.signal(DEGRADED, DIVIDED);
    survey.signal(DIVERGING, DIVIDED);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    // All divided - no clear consensus on any sign
    for (final var signal : signals) {
      assertEquals(DIVIDED, signal.dimension());
    }

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testMajority() {

    survey.signal(STABLE, MAJORITY);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(STABLE, signals.getFirst().sign());
    assertEquals(MAJORITY, signals.getFirst().dimension());

  }

  // ========== SIGNAL CACHING TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testMultipleSubjects() {

    final var name1 = CORTEX.name("cluster.region1.health");
    final var name2 = CORTEX.name("cluster.region2.health");

    final var conduit =
      (Conduit< Signal< Statuses.Sign > >) (Conduit< ? >)
        circuit.conduit(Signal.class);
    final var surveys = Surveys.pool(Statuses.SIGNS, conduit);
    final var survey1 = surveys.get(name1);
    final var survey2 = surveys.get(name2);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    survey1.signal(STABLE, UNANIMOUS);
    survey2.signal(DEGRADED, MAJORITY);
    survey1.signal(DIVERGING, DIVIDED);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(3, allSignals.size());
    assertEquals(name1, allSignals.get(0).subject().name());
    assertEquals(name2, allSignals.get(1).subject().name());
    assertEquals(name1, allSignals.get(2).subject().name());

  }

  @Test
  void testSignalCaching() {

    // Emit same signal twice
    survey.signal(DEGRADED, MAJORITY);
    survey.signal(DEGRADED, MAJORITY);

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

    // Test caching for multiple sign/dimension combinations
    final var dimensions = Dimension.values();

    // First pass - emit for DEGRADED
    for (final var dimension : dimensions) {
      survey.signal(DEGRADED, dimension);
    }

    final var firstPass =
      captures
        .drainEmissions()
        .toList();

    // Second pass - emit same signals
    for (final var dimension : dimensions) {
      survey.signal(DEGRADED, dimension);
    }

    final var secondPass =
      captures
        .drainEmissions()
        .toList();

    // Verify all instances are cached (same reference)
    for (var i = 0; i < dimensions.length; i++) {
      assertSame(
        firstPass.get(i),
        secondPass.get(i),
        "Signal should be cached for DEGRADED × " + dimensions[i]
      );
    }

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    survey.signal(STABLE, UNANIMOUS);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(STABLE, capture.emission().sign());
    assertEquals(UNANIMOUS, capture.emission().dimension());

  }

  // ========== GENERIC SIGN TYPE TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testUnanimous() {

    survey.signal(DOWN, UNANIMOUS);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(DOWN, signals.getFirst().sign());
    assertEquals(UNANIMOUS, signals.getFirst().dimension());

  }

  // ========== CLUSTER SCENARIO TESTS ==========

  @SpecRef({"6.2", "6.5", "7.7"})
  @Test
  void testWithDifferentSignType() {

    // Test that Surveys works with different Sign types (like Trends)
    final var trendsConduit =
      (Conduit< Signal< Trends.Sign > >) (Conduit< ? >)
        circuit.conduit(
          Signal.class
        );

    final var trendsSurvey =
      Surveys.pool(Trends.SIGNS, trendsConduit)
        .get(CORTEX.name("cluster.trends"));

    final var trendsCaptures =
      CaptureBuffer.of(circuit, trendsConduit);

    trendsSurvey.signal(
      Trends.Sign.STABLE,
      UNANIMOUS
    );

    trendsSurvey.signal(
      Trends.Sign.DRIFT,
      MAJORITY
    );

    trendsSurvey.signal(
      Trends.Sign.SPIKE,
      DIVIDED
    );

    final var signals =
      trendsCaptures
        .drainEmissions()
        .toList();
    trendsCaptures.close();

    assertEquals(3, signals.size());
    assertEquals(Trends.Sign.STABLE, signals.get(0).sign());
    assertEquals(UNANIMOUS, signals.get(0).dimension());
    assertEquals(Trends.Sign.DRIFT, signals.get(1).sign());
    assertEquals(MAJORITY, signals.get(1).dimension());
    assertEquals(Trends.Sign.SPIKE, signals.get(2).sign());
    assertEquals(DIVIDED, signals.get(2).dimension());

  }

}
