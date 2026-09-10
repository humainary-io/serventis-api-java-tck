// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.tool;

import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.opt.tool.Counters.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.tool.Counters.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Counters] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class CountersContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("requests.total");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Counter counter;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    counter =
      conduit.pool(Counters::of)
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
  void testIncrement() {

    counter.increment();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(INCREMENT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMixedOperations() {

    counter.increment();
    counter.increment();
    counter.overflow();
    counter.reset();
    counter.increment();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(INCREMENT, signs.getFirst());
    assertEquals(INCREMENT, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(RESET, signs.get(3));
    assertEquals(INCREMENT, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverflow() {

    counter.overflow();

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

    counter.reset();

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
    counter.sign(INCREMENT);
    counter.sign(OVERFLOW);
    counter.sign(RESET);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(INCREMENT, signs.get(0));
    assertEquals(OVERFLOW, signs.get(1));
    assertEquals(RESET, signs.get(2));

  }

  @SpecRef({"4.1", "registry:counters"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, INCREMENT.ordinal());
    assertEquals(1, OVERFLOW.ordinal());
    assertEquals(2, RESET.ordinal());

  }

  @SpecRef({"4.5", "registry:counters"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(3, values.length);
    assertEquals(INCREMENT, values[0]);
    assertEquals(OVERFLOW, values[1]);
    assertEquals(RESET, values[2]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    counter.increment();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(INCREMENT, capture.emission());

  }

}
