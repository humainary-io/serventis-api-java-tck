// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.data;

import io.humainary.serventis.opt.data.*;
import io.humainary.serventis.opt.data.Queues.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.data.Queues.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Queues] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class QueuesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("worker.queue");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Queue queue;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    queue =
      conduit.pool(Queues::of)
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

    queue.enqueue();
    queue.overflow();
    queue.dequeue();
    queue.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(ENQUEUE, signs.getFirst());
    assertEquals(OVERFLOW, signs.get(1));
    assertEquals(DEQUEUE, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDequeue() {

    queue.dequeue();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DEQUEUE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testEnqueue() {

    queue.enqueue();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ENQUEUE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverflow() {

    queue.overflow();

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

    queue.enqueue();
    queue.enqueue();
    queue.overflow();
    queue.overflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(ENQUEUE, signs.getFirst());
    assertEquals(ENQUEUE, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(OVERFLOW, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testQueueLifecycle() {

    queue.enqueue();
    queue.enqueue();
    queue.dequeue();
    queue.enqueue();
    queue.dequeue();
    queue.dequeue();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(ENQUEUE, signs.getFirst());
    assertEquals(ENQUEUE, signs.get(1));
    assertEquals(DEQUEUE, signs.get(2));
    assertEquals(ENQUEUE, signs.get(3));
    assertEquals(DEQUEUE, signs.get(4));
    assertEquals(DEQUEUE, signs.get(5));

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    queue.sign(ENQUEUE);
    queue.sign(DEQUEUE);
    queue.sign(OVERFLOW);
    queue.sign(UNDERFLOW);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(ENQUEUE, signs.get(0));
    assertEquals(DEQUEUE, signs.get(1));
    assertEquals(OVERFLOW, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

  @SpecRef({"4.1", "registry:queues"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, ENQUEUE.ordinal());
    assertEquals(1, DEQUEUE.ordinal());
    assertEquals(2, OVERFLOW.ordinal());
    assertEquals(3, UNDERFLOW.ordinal());

  }

  @SpecRef({"4.5", "registry:queues"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(4, values.length);
    assertEquals(ENQUEUE, values[0]);
    assertEquals(DEQUEUE, values[1]);
    assertEquals(OVERFLOW, values[2]);
    assertEquals(UNDERFLOW, values[3]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    queue.enqueue();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(ENQUEUE, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUnderflow() {

    queue.underflow();

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

    queue.dequeue();
    queue.dequeue();
    queue.underflow();
    queue.underflow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(DEQUEUE, signs.getFirst());
    assertEquals(DEQUEUE, signs.get(1));
    assertEquals(UNDERFLOW, signs.get(2));
    assertEquals(UNDERFLOW, signs.get(3));

  }

}
