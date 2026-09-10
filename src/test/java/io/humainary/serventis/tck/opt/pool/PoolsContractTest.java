// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.pool;

import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.pool.Pools.*;
import io.humainary.serventis.opt.pool.Pools.Pool;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Pools.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Pools] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class PoolsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("db.connections");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Pool pool;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    pool =
      conduit.pool(Pools::of)
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
  void testAdaptiveScalingPattern() {

    // Simulate adaptive scaling: grow on demand, shrink when idle
    pool.expand();
    pool.expand();
    pool.borrow();
    pool.borrow();
    pool.reclaim();
    pool.reclaim();
    pool.contract();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(EXPAND, signs.get(0));
    assertEquals(EXPAND, signs.get(1));
    assertEquals(BORROW, signs.get(2));
    assertEquals(BORROW, signs.get(3));
    assertEquals(RECLAIM, signs.get(4));
    assertEquals(RECLAIM, signs.get(5));
    assertEquals(CONTRACT, signs.get(6));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAllSigns() {

    pool.expand();
    pool.contract();
    pool.borrow();
    pool.reclaim();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(EXPAND, signs.getFirst());
    assertEquals(CONTRACT, signs.get(1));
    assertEquals(BORROW, signs.get(2));
    assertEquals(RECLAIM, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBorrow() {

    pool.borrow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(BORROW, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCapacityOscillationPattern() {

    // Simulate capacity oscillation (unstable sizing)
    pool.expand();
    pool.contract();
    pool.expand();
    pool.contract();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());

    final var expandCount =
      signs
        .stream()
        .filter(s -> s==EXPAND)
        .count();

    final var contractCount =
      signs
        .stream()
        .filter(s -> s==CONTRACT)
        .count();

    assertEquals(2, expandCount);
    assertEquals(2, contractCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testContract() {

    pool.contract();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CONTRACT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpand() {

    pool.expand();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EXPAND, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testHealthyUtilizationPattern() {

    // Simulate healthy utilization (balanced borrow/reclaim)
    pool.borrow();
    pool.reclaim();
    pool.borrow();
    pool.reclaim();
    pool.borrow();
    pool.reclaim();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());

    final var borrowCount =
      signs
        .stream()
        .filter(s -> s==BORROW)
        .count();

    final var reclaimCount =
      signs
        .stream()
        .filter(s -> s==RECLAIM)
        .count();

    assertEquals(3, borrowCount);
    assertEquals(3, reclaimCount);

  }

  /// `Pools` names a resource pool, which is not the instrument pool of `6.4`: this drives the
  /// vocabulary's own EXPAND operation and reads the emitted sequence back.

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPoolInitializationPattern() {

    // Simulate pool initialization (multiple grows)
    pool.expand();
    pool.expand();
    pool.expand();
    pool.expand();
    pool.expand();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());

    final var expandCount =
      signs
        .stream()
        .filter(s -> s==EXPAND)
        .count();

    assertEquals(5, expandCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReclaim() {

    pool.reclaim();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RECLAIM, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testResourceLeakPattern() {

    // Simulate resource leak (borrows without reclaims)
    pool.borrow();
    pool.borrow();
    pool.borrow();
    pool.borrow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());

    final var borrowCount =
      signs
        .stream()
        .filter(s -> s==BORROW)
        .count();

    final var reclaimCount =
      signs
        .stream()
        .filter(s -> s==RECLAIM)
        .count();

    assertEquals(4, borrowCount);
    assertEquals(0, reclaimCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSaturationPattern() {

    // Simulate saturation (high borrow rate, capacity limit)
    pool.borrow();
    pool.borrow();
    pool.borrow();
    pool.expand();
    pool.borrow();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(BORROW, signs.get(0));
    assertEquals(BORROW, signs.get(1));
    assertEquals(BORROW, signs.get(2));
    assertEquals(EXPAND, signs.get(3));
    assertEquals(BORROW, signs.get(4));

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    pool.sign(EXPAND);
    pool.sign(CONTRACT);
    pool.sign(BORROW);
    pool.sign(RECLAIM);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(EXPAND, signs.get(0));
    assertEquals(CONTRACT, signs.get(1));
    assertEquals(BORROW, signs.get(2));
    assertEquals(RECLAIM, signs.get(3));

  }

  @SpecRef({"4.1", "registry:pools"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, EXPAND.ordinal());
    assertEquals(1, CONTRACT.ordinal());
    assertEquals(2, BORROW.ordinal());
    assertEquals(3, RECLAIM.ordinal());

  }

  @SpecRef({"4.5", "registry:pools"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(4, values.length);
    assertEquals(EXPAND, values[0]);
    assertEquals(CONTRACT, values[1]);
    assertEquals(BORROW, values[2]);
    assertEquals(RECLAIM, values[3]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    pool.expand();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(EXPAND, capture.emission());

  }

}
