// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.tool;

import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.opt.tool.Gauges.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.tool.Gauges.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Gauges] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class GaugesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("connections.active");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Gauge gauge;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    gauge =
      conduit.pool(Gauges::of)
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
  void testBidirectionalOperations() {

    gauge.increment();
    gauge.increment();
    gauge.decrement();
    gauge.increment();
    gauge.decrement();
    gauge.decrement();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(INCREMENT, signs.getFirst());
    assertEquals(INCREMENT, signs.get(1));
    assertEquals(DECREMENT, signs.get(2));
    assertEquals(INCREMENT, signs.get(3));
    assertEquals(DECREMENT, signs.get(4));
    assertEquals(DECREMENT, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBoundaryConditions() {

    gauge.increment();
    gauge.overflow();
    gauge.decrement();
    gauge.underflow();
    gauge.reset();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(INCREMENT, signs.getFirst());
    assertEquals(OVERFLOW, signs.get(1));
    assertEquals(DECREMENT, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));
    assertEquals(RESET, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDecrement() {

    gauge.decrement();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DECREMENT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testIncrement() {

    gauge.increment();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(INCREMENT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverflow() {

    gauge.overflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(OVERFLOW, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReset() {

    gauge.reset();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RESET, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    gauge.sign(INCREMENT);
    gauge.sign(DECREMENT);
    gauge.sign(OVERFLOW);
    gauge.sign(UNDERFLOW);
    gauge.sign(RESET);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(INCREMENT, signs.get(0));
    assertEquals(DECREMENT, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));
    assertEquals(RESET, signs.get(4));

  }

  @SpecRef({"4.1", "registry:gauges"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, INCREMENT.ordinal());
    assertEquals(1, DECREMENT.ordinal());
    assertEquals(2, OVERFLOW.ordinal());
    assertEquals(3, UNDERFLOW.ordinal());
    assertEquals(4, RESET.ordinal());

  }

  @SpecRef({"4.5", "registry:gauges"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(5, values.length);
    assertEquals(INCREMENT, values[0]);
    assertEquals(DECREMENT, values[1]);
    assertEquals(OVERFLOW, values[2]);
    assertEquals(UNDERFLOW, values[3]);
    assertEquals(RESET, values[4]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    gauge.increment();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(INCREMENT, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUnderflow() {

    gauge.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(UNDERFLOW, signs.getFirst());

  }

}
