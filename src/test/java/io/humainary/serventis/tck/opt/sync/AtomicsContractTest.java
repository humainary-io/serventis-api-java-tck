// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.sync;

import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.sync.Atomics.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.sync.Atomics.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Atomics] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class AtomicsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("queue.head.cas");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Atomic atomic;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    atomic =
      conduit.pool(Atomics::of)
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
  void testAttempt() {

    atomic.attempt();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBackoff() {

    atomic.backoff();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(BACKOFF, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCASWithAdaptiveBackoff() {

    atomic.attempt();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.backoff();
    atomic.fail();
    atomic.backoff();
    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(8, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(FAIL, signs.get(1));
    assertEquals(SPIN, signs.get(2));
    assertEquals(FAIL, signs.get(3));
    assertEquals(BACKOFF, signs.get(4));
    assertEquals(FAIL, signs.get(5));
    assertEquals(BACKOFF, signs.get(6));
    assertEquals(SUCCESS, signs.get(7));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCASWithSpinRetry() {

    atomic.attempt();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.spin();
    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(FAIL, signs.get(1));
    assertEquals(SPIN, signs.get(2));
    assertEquals(FAIL, signs.get(3));
    assertEquals(SPIN, signs.get(4));
    assertEquals(SUCCESS, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCASWithYield() {

    atomic.attempt();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.yield();
    atomic.fail();
    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(FAIL, signs.get(1));
    assertEquals(SPIN, signs.get(2));
    assertEquals(FAIL, signs.get(3));
    assertEquals(YIELD, signs.get(4));
    assertEquals(FAIL, signs.get(5));
    assertEquals(SUCCESS, signs.get(6));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExhaust() {

    atomic.exhaust();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EXHAUST, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFail() {

    atomic.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FAIL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPark() {

    atomic.park();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PARK, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRetryExhaustion() {

    atomic.attempt();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.backoff();
    atomic.fail();
    atomic.exhaust();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(FAIL, signs.get(1));
    assertEquals(SPIN, signs.get(2));
    assertEquals(FAIL, signs.get(3));
    assertEquals(BACKOFF, signs.get(4));
    assertEquals(FAIL, signs.get(5));
    assertEquals(EXHAUST, signs.get(6));

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:atomics"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, ATTEMPT.ordinal());
    assertEquals(1, SUCCESS.ordinal());
    assertEquals(2, FAIL.ordinal());
    assertEquals(3, SPIN.ordinal());
    assertEquals(4, YIELD.ordinal());
    assertEquals(5, BACKOFF.ordinal());
    assertEquals(6, PARK.ordinal());
    assertEquals(7, EXHAUST.ordinal());

  }

  @SpecRef("6.1")
  @Test
  void testSignMethod() {

    // Test direct sign() method
    atomic.sign(ATTEMPT);
    atomic.sign(SUCCESS);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(SUCCESS, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSimpleCASSuccess() {

    atomic.attempt();
    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(SUCCESS, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSpin() {

    atomic.spin();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SPIN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSpinToParkEscalation() {

    atomic.attempt();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.spin();
    atomic.fail();
    atomic.park();
    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(8, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(FAIL, signs.get(1));
    assertEquals(SPIN, signs.get(2));
    assertEquals(FAIL, signs.get(3));
    assertEquals(SPIN, signs.get(4));
    assertEquals(FAIL, signs.get(5));
    assertEquals(PARK, signs.get(6));
    assertEquals(SUCCESS, signs.get(7));

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    atomic.attempt();

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
  void testSuccess() {

    atomic.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SUCCESS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testYield() {

    atomic.yield();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(YIELD, signs.getFirst());

  }

}
