// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.flow;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.flow.Flows.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.flow.Flows.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Flows.Flow] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class FlowsContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("flow.1");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Flows.Flow flow;

  private void assertSignal(
    final Signal signal
  ) {

    assertEquals(
      1L, captures
        .drain()
        .filter(capture -> capture.emission().equals(signal))
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

    flow =
      conduit.pool(Flows::of)
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

  // Individual tests for each sign + dimension combination

  /// Tests that [Dimension] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Dimension] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:flows"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, Dimension.INGRESS.ordinal());
    assertEquals(1, Dimension.TRANSIT.ordinal());
    assertEquals(2, Dimension.EGRESS.ordinal());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailEgress() {

    flow.fail(EGRESS);

    assertSignal(
      new Signal(Sign.FAIL, EGRESS)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailIngress() {

    flow.fail(INGRESS);

    assertSignal(
      new Signal(Sign.FAIL, INGRESS)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailTransit() {

    flow.fail(TRANSIT);

    assertSignal(
      new Signal(Sign.FAIL, TRANSIT)
    );

  }

  /// Tests failure at ingress stage (refused)

  @SpecRef({"6.3", "6.5"})
  @Test
  void testIngressFailurePattern() {

    // Simulate flow that is refused entry
    flow.fail(INGRESS);  // refused

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(new Signal(Sign.FAIL, INGRESS), signals.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    flow.success(INGRESS);
    flow.success(TRANSIT);
    flow.success(EGRESS);
    flow.fail(INGRESS);

    assertEquals(
      4L,
      captures
        .drain()
        .count()
    );

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:flows"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, Sign.SUCCESS.ordinal());
    assertEquals(1, Sign.FAIL.ordinal());

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    flow.signal(Sign.SUCCESS, INGRESS);
    flow.signal(Sign.SUCCESS, TRANSIT);
    flow.signal(Sign.SUCCESS, EGRESS);
    flow.signal(Sign.FAIL, INGRESS);
    flow.signal(Sign.FAIL, TRANSIT);
    flow.signal(Sign.FAIL, EGRESS);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signals.size());
    assertEquals(new Signal(Sign.SUCCESS, INGRESS), signals.get(0));
    assertEquals(new Signal(Sign.SUCCESS, TRANSIT), signals.get(1));
    assertEquals(new Signal(Sign.SUCCESS, EGRESS), signals.get(2));
    assertEquals(new Signal(Sign.FAIL, INGRESS), signals.get(3));
    assertEquals(new Signal(Sign.FAIL, TRANSIT), signals.get(4));
    assertEquals(new Signal(Sign.FAIL, EGRESS), signals.get(5));

  }

  @SpecRef("4.4")
  @Test
  void testSignalAccessors() {

    final var SUCCESS_INGRESS = new Signal(Sign.SUCCESS, INGRESS);
    final var SUCCESS_TRANSIT = new Signal(Sign.SUCCESS, TRANSIT);
    final var SUCCESS_EGRESS = new Signal(Sign.SUCCESS, EGRESS);
    final var FAIL_INGRESS = new Signal(Sign.FAIL, INGRESS);
    final var FAIL_TRANSIT = new Signal(Sign.FAIL, TRANSIT);
    final var FAIL_EGRESS = new Signal(Sign.FAIL, EGRESS);

    assertEquals(Sign.SUCCESS, SUCCESS_INGRESS.sign());
    assertEquals(Dimension.INGRESS, SUCCESS_INGRESS.dimension());

    assertEquals(Sign.SUCCESS, SUCCESS_TRANSIT.sign());
    assertEquals(Dimension.TRANSIT, SUCCESS_TRANSIT.dimension());

    assertEquals(Sign.SUCCESS, SUCCESS_EGRESS.sign());
    assertEquals(Dimension.EGRESS, SUCCESS_EGRESS.dimension());

    assertEquals(Sign.FAIL, FAIL_INGRESS.sign());
    assertEquals(Dimension.INGRESS, FAIL_INGRESS.dimension());

    assertEquals(Sign.FAIL, FAIL_TRANSIT.sign());
    assertEquals(Dimension.TRANSIT, FAIL_TRANSIT.dimension());

    assertEquals(Sign.FAIL, FAIL_EGRESS.sign());
    assertEquals(Dimension.EGRESS, FAIL_EGRESS.dimension());

  }

  /// A signal is a sign together with a dimension, and reports back the pair it was made
  /// from. Which signs and dimensions exist, and in what order, is pinned once for every
  /// vocabulary by the consolidated tables in `SymbolContractTest` — repeating it per domain
  /// left the same fact asserted in two places and cited in neither completely.

  @SpecRef("4.4")
  @Test
  void testSignalComposition() {

    final var signal = new Signal(Sign.SUCCESS, Dimension.INGRESS);
    assertEquals(Sign.SUCCESS, signal.sign());
    assertEquals(Dimension.INGRESS, signal.dimension());

  }

  @SpecRef({"4.4", "4.6"})
  @Test
  void testSignalDimensionMappings() {

    // Verify all Sign x Dimension combinations can be constructed
    for (final var sign : Sign.values()) {

      for (final var dimension : Dimension.values()) {

        final var signal = new Signal(sign, dimension);

        assertEquals(
          sign,
          signal.sign()
        );

        assertEquals(
          dimension,
          signal.dimension()
        );

      }

    }

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    flow.success(INGRESS);

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

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccessEgress() {

    flow.success(EGRESS);

    assertSignal(
      new Signal(Sign.SUCCESS, EGRESS)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccessIngress() {

    flow.success(INGRESS);

    assertSignal(
      new Signal(Sign.SUCCESS, INGRESS)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccessTransit() {

    flow.success(TRANSIT);

    assertSignal(
      new Signal(Sign.SUCCESS, TRANSIT)
    );

  }

  /// Tests failure at transit stage (dropped)

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransitFailurePattern() {

    // Simulate flow that enters but fails during processing
    flow.success(INGRESS);  // entered
    flow.fail(TRANSIT);     // dropped

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertEquals(new Signal(Sign.SUCCESS, INGRESS), signals.get(0));
    assertEquals(new Signal(Sign.FAIL, TRANSIT), signals.get(1));

  }

  /// Tests the typical flow pattern: ingress -> transit -> egress

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTypicalFlowPattern() {

    // Simulate successful flow through all stages
    flow.success(INGRESS);  // entered
    flow.success(TRANSIT);  // moved
    flow.success(EGRESS);   // exited

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(new Signal(Sign.SUCCESS, INGRESS), signals.get(0));
    assertEquals(new Signal(Sign.SUCCESS, TRANSIT), signals.get(1));
    assertEquals(new Signal(Sign.SUCCESS, EGRESS), signals.get(2));

  }

}
