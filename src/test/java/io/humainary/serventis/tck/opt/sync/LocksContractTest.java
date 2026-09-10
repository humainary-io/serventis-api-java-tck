// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.sync;

import io.humainary.serventis.opt.sync.*;
import io.humainary.serventis.opt.sync.Locks.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.sync.Locks.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Locks] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class LocksContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("cache.mutex");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Lock lock;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    lock =
      conduit.pool(Locks::of)
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

    lock.abandon();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ABANDON, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAbandonedLock() {

    lock.acquire();
    lock.grant();
    lock.abandon();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ACQUIRE, signs.get(0));
    assertEquals(GRANT, signs.get(1));
    assertEquals(ABANDON, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAcquire() {

    lock.acquire();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ACQUIRE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAttempt() {

    lock.attempt();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBlockingLockSuccess() {

    lock.acquire();
    lock.grant();
    lock.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ACQUIRE, signs.get(0));
    assertEquals(GRANT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBlockingLockTimeout() {

    lock.acquire();
    lock.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ACQUIRE, signs.get(0));
    assertEquals(TIMEOUT, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCASWithContention() {

    lock.attempt();
    lock.contest();
    lock.contest();
    lock.grant();
    lock.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(CONTEST, signs.get(1));
    assertEquals(CONTEST, signs.get(2));
    assertEquals(GRANT, signs.get(3));
    assertEquals(RELEASE, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testContest() {

    lock.contest();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CONTEST, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeny() {

    lock.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DENY, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDowngrade() {

    lock.downgrade();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DOWNGRADE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailedTryLock() {

    lock.attempt();
    lock.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(DENY, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testGrant() {

    lock.grant();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(GRANT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testLockUpgradeDowngrade() {

    lock.attempt();
    lock.grant();
    lock.upgrade();
    lock.downgrade();
    lock.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(GRANT, signs.get(1));
    assertEquals(UPGRADE, signs.get(2));
    assertEquals(DOWNGRADE, signs.get(3));
    assertEquals(RELEASE, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRelease() {

    lock.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RELEASE, signs.getFirst());

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:locks"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, ATTEMPT.ordinal());
    assertEquals(1, ACQUIRE.ordinal());
    assertEquals(2, GRANT.ordinal());
    assertEquals(3, DENY.ordinal());
    assertEquals(4, TIMEOUT.ordinal());
    assertEquals(5, RELEASE.ordinal());
    assertEquals(6, UPGRADE.ordinal());
    assertEquals(7, DOWNGRADE.ordinal());
    assertEquals(8, CONTEST.ordinal());
    assertEquals(9, ABANDON.ordinal());

  }

  @SpecRef("6.1")
  @Test
  void testSignMethod() {

    // Test direct sign() method
    lock.sign(ATTEMPT);
    lock.sign(GRANT);
    lock.sign(RELEASE);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(GRANT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    lock.attempt();

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
  void testSuccessfulTryLock() {

    lock.attempt();
    lock.grant();
    lock.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(GRANT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTimeout() {

    lock.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(TIMEOUT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUpgrade() {

    lock.upgrade();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(UPGRADE, signs.getFirst());

  }

}
