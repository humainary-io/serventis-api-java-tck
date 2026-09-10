// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.data;

import io.humainary.serventis.opt.data.*;
import io.humainary.serventis.opt.data.Stacks.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.data.Stacks.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Stacks] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class StacksContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("parser.states");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Stack stack;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    stack =
      conduit.pool(Stacks::of)
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
  void testBoundaryConditions() {

    stack.push();
    stack.overflow();
    stack.pop();
    stack.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(PUSH, signs.getFirst());
    assertEquals(OVERFLOW, signs.get(1));
    assertEquals(POP, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverflow() {

    stack.overflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(OVERFLOW, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverflowPattern() {

    stack.push();
    stack.push();
    stack.overflow();
    stack.overflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(PUSH, signs.getFirst());
    assertEquals(PUSH, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(OVERFLOW, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPop() {

    stack.pop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(POP, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPush() {

    stack.push();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PUSH, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    stack.sign(PUSH);
    stack.sign(POP);
    stack.sign(OVERFLOW);
    stack.sign(UNDERFLOW);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(PUSH, signs.get(0));
    assertEquals(POP, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

  @SpecRef({"4.1", "registry:stacks"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, PUSH.ordinal());
    assertEquals(1, POP.ordinal());
    assertEquals(2, OVERFLOW.ordinal());
    assertEquals(3, UNDERFLOW.ordinal());

  }

  @SpecRef({"4.5", "registry:stacks"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(4, values.length);
    assertEquals(PUSH, values[0]);
    assertEquals(POP, values[1]);
    assertEquals(OVERFLOW, values[2]);
    assertEquals(UNDERFLOW, values[3]);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStackLifecycle() {

    stack.push();
    stack.push();
    stack.pop();
    stack.push();
    stack.pop();
    stack.pop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(PUSH, signs.getFirst());
    assertEquals(PUSH, signs.get(1));
    assertEquals(POP, signs.get(2));
    assertEquals(PUSH, signs.get(3));
    assertEquals(POP, signs.get(4));
    assertEquals(POP, signs.get(5));

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    stack.push();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(PUSH, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUnderflow() {

    stack.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(UNDERFLOW, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUnderflowPattern() {

    stack.pop();
    stack.pop();
    stack.underflow();
    stack.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(POP, signs.getFirst());
    assertEquals(POP, signs.get(1));
    assertEquals(UNDERFLOW, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

}
