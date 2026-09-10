// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.flow;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.flow.Breakers.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.flow.Breakers.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Breakers] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class BreakersContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("api.breaker");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Breaker breaker;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    breaker =
      conduit.pool(Breakers::of)
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
  void testClose() {

    breaker.close();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CLOSE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testHalfOpen() {

    breaker.halfOpen();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(HALF_OPEN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOpen() {

    breaker.open();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(OPEN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProbe() {

    breaker.probe();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PROBE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReset() {

    breaker.reset();

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
    breaker.sign(CLOSE);
    breaker.sign(OPEN);
    breaker.sign(HALF_OPEN);
    breaker.sign(TRIP);
    breaker.sign(PROBE);
    breaker.sign(RESET);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(CLOSE, signs.get(0));
    assertEquals(OPEN, signs.get(1));
    assertEquals(HALF_OPEN, signs.get(2));
    assertEquals(TRIP, signs.get(3));
    assertEquals(PROBE, signs.get(4));
    assertEquals(RESET, signs.get(5));

  }

  @SpecRef({"4.1", "registry:breakers"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, CLOSE.ordinal());
    assertEquals(1, OPEN.ordinal());
    assertEquals(2, HALF_OPEN.ordinal());
    assertEquals(3, TRIP.ordinal());
    assertEquals(4, PROBE.ordinal());
    assertEquals(5, RESET.ordinal());

  }

  @SpecRef({"4.5", "registry:breakers"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(6, values.length);
    assertEquals(CLOSE, values[0]);
    assertEquals(OPEN, values[1]);
    assertEquals(HALF_OPEN, values[2]);
    assertEquals(TRIP, values[3]);
    assertEquals(PROBE, values[4]);
    assertEquals(RESET, values[5]);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStateTransitionPattern() {

    // Simulate typical circuit breaker lifecycle
    breaker.close();     // Normal operation
    breaker.trip();      // Failures detected
    breaker.open();      // Circuit opens
    breaker.halfOpen();  // Testing recovery
    breaker.probe();     // Test request
    breaker.close();     // Recovery successful

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(CLOSE, signs.get(0));
    assertEquals(TRIP, signs.get(1));
    assertEquals(OPEN, signs.get(2));
    assertEquals(HALF_OPEN, signs.get(3));
    assertEquals(PROBE, signs.get(4));
    assertEquals(CLOSE, signs.get(5));

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    breaker.close();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(CLOSE, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTrip() {

    breaker.trip();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(TRIP, signs.getFirst());

  }

}
