// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.pool;

import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.pool.Resources.*;
import io.humainary.serventis.opt.pool.Resources.Resource;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Resources.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Resources] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class ResourcesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("db.connections");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Resource resource;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    resource =
      conduit.pool(Resources::of)
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
  void testAcquire() {

    resource.acquire();

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

    resource.attempt();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBlockingAcquisitionSuccess() {

    resource.acquire();
    resource.grant();
    resource.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ACQUIRE, signs.getFirst());
    assertEquals(GRANT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBlockingAcquisitionTimeout() {

    resource.acquire();
    resource.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ACQUIRE, signs.getFirst());
    assertEquals(TIMEOUT, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testContentionPattern() {

    resource.attempt();
    resource.deny();
    resource.attempt();
    resource.deny();
    resource.attempt();
    resource.grant();
    resource.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());
    assertEquals(DENY, signs.get(1));
    assertEquals(ATTEMPT, signs.get(2));
    assertEquals(DENY, signs.get(3));
    assertEquals(ATTEMPT, signs.get(4));
    assertEquals(GRANT, signs.get(5));
    assertEquals(RELEASE, signs.get(6));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeny() {

    resource.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DENY, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testGrant() {

    resource.grant();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(GRANT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleAcquisitions() {

    resource.attempt();
    resource.grant();
    resource.attempt();
    resource.grant();
    resource.release();
    resource.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());
    assertEquals(GRANT, signs.get(1));
    assertEquals(ATTEMPT, signs.get(2));
    assertEquals(GRANT, signs.get(3));
    assertEquals(RELEASE, signs.get(4));
    assertEquals(RELEASE, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNonBlockingAcquisitionFailure() {

    resource.attempt();
    resource.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());
    assertEquals(DENY, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNonBlockingAcquisitionSuccess() {

    resource.attempt();
    resource.grant();
    resource.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(ATTEMPT, signs.getFirst());
    assertEquals(GRANT, signs.get(1));
    assertEquals(RELEASE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRelease() {

    resource.release();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RELEASE, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    resource.sign(ATTEMPT);
    resource.sign(ACQUIRE);
    resource.sign(GRANT);
    resource.sign(DENY);
    resource.sign(TIMEOUT);
    resource.sign(RELEASE);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(ATTEMPT, signs.get(0));
    assertEquals(ACQUIRE, signs.get(1));
    assertEquals(GRANT, signs.get(2));
    assertEquals(DENY, signs.get(3));
    assertEquals(TIMEOUT, signs.get(4));
    assertEquals(RELEASE, signs.get(5));

  }

  @SpecRef({"4.1", "registry:resources"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, ATTEMPT.ordinal());
    assertEquals(1, ACQUIRE.ordinal());
    assertEquals(2, GRANT.ordinal());
    assertEquals(3, DENY.ordinal());
    assertEquals(4, TIMEOUT.ordinal());
    assertEquals(5, RELEASE.ordinal());

  }

  @SpecRef({"4.5", "registry:resources"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(6, values.length);
    assertEquals(ATTEMPT, values[0]);
    assertEquals(ACQUIRE, values[1]);
    assertEquals(GRANT, values[2]);
    assertEquals(DENY, values[3]);
    assertEquals(TIMEOUT, values[4]);
    assertEquals(RELEASE, values[5]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    resource.attempt();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(ATTEMPT, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTimeout() {

    resource.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(TIMEOUT, signs.getFirst());

  }

}
