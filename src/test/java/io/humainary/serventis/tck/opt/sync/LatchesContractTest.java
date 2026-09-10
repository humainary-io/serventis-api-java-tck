// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.sync;

import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.sync.Latches.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.sync.Latches.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Latches] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class LatchesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("startup.barrier");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Latch latch;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    latch =
      conduit.pool(Latches::of)
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
  void testAbandon() {

    latch.abandon();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ABANDON, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testArrive() {

    latch.arrive();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ARRIVE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAwait() {

    latch.await();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(AWAIT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCountDownLatchPattern() {

    latch.await();
    latch.await();
    latch.arrive();
    latch.arrive();
    latch.arrive();
    latch.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(AWAIT, signs.get(1));
    assertEquals(ARRIVE, signs.get(2));
    assertEquals(ARRIVE, signs.get(3));
    assertEquals(ARRIVE, signs.get(4));
    assertEquals(RELEASE, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCyclicBarrierMultiplePhases() {

    // Phase 1
    latch.await();
    latch.await();
    latch.release();
    latch.reset();

    // Phase 2
    latch.await();
    latch.await();
    latch.release();
    latch.reset();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(8, signs.size());
    // Phase 1
    assertEquals(AWAIT, signs.get(0));
    assertEquals(AWAIT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));
    assertEquals(RESET, signs.get(3));
    // Phase 2
    assertEquals(AWAIT, signs.get(4));
    assertEquals(AWAIT, signs.get(5));
    assertEquals(RELEASE, signs.get(6));
    assertEquals(RESET, signs.get(7));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCyclicBarrierPattern() {

    latch.await();
    latch.await();
    latch.await();
    latch.release();
    latch.reset();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(AWAIT, signs.get(1));
    assertEquals(AWAIT, signs.get(2));
    assertEquals(RELEASE, signs.get(3));
    assertEquals(RESET, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testParticipantFailure() {

    latch.await();
    latch.await();
    latch.arrive();
    latch.abandon();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(AWAIT, signs.get(1));
    assertEquals(ARRIVE, signs.get(2));
    assertEquals(ABANDON, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRelease() {

    latch.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RELEASE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReset() {

    latch.reset();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RESET, signs.getFirst());

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:latches"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, AWAIT.ordinal());
    assertEquals(1, ARRIVE.ordinal());
    assertEquals(2, RELEASE.ordinal());
    assertEquals(3, TIMEOUT.ordinal());
    assertEquals(4, RESET.ordinal());
    assertEquals(5, ABANDON.ordinal());

  }

  @SpecRef("6.1")
  @Test
  void testSignMethod() {

    // Test direct sign() method
    latch.sign(AWAIT);
    latch.sign(ARRIVE);
    latch.sign(RELEASE);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(ARRIVE, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStartSignalPattern() {

    // Worker threads wait
    latch.await();
    latch.await();
    latch.await();
    // Main thread signals start
    latch.arrive();
    latch.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(AWAIT, signs.get(1));
    assertEquals(AWAIT, signs.get(2));
    assertEquals(ARRIVE, signs.get(3));
    assertEquals(RELEASE, signs.get(4));

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    latch.await();

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
  void testTimeout() {

    latch.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(TIMEOUT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTimeoutWaitPattern() {

    latch.await();
    latch.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(AWAIT, signs.get(0));
    assertEquals(TIMEOUT, signs.get(1));

  }

}
