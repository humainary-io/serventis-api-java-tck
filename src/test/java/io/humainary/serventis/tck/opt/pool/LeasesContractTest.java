// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.pool;

import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.pool.Leases.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Leases.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Lease] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class LeasesContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("leadership");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Lease lease;

  private void assertSignal(
    final Signal signal
  ) {

    assertEquals(
      1L, captures
        .drain()
        .filter(capture -> capture.subject().name()==NAME)
        .map(Capture::emission)
        .filter(s -> s.equals(signal))
        .count()

    );

  }

  private void emit(
    final Signal signal,
    final Runnable emitter
  ) {

    emitter.run();

    assertSignal(
      signal
    );

  }

  @BeforeEach
  void setup() {

    circuit =
      cortex().circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    lease =
      conduit.pool(Leases::of)
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

    emit(new Signal(Sign.ACQUIRE, LESSEE), () -> lease.acquire(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAcquired() {

    emit(new Signal(Sign.ACQUIRE, LESSOR), () -> lease.acquire(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDenied() {

    emit(new Signal(Sign.DENY, LESSEE), () -> lease.deny(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeny() {

    emit(new Signal(Sign.DENY, LESSOR), () -> lease.deny(LESSOR));

  }

  @SpecRef({"4.1", "registry:leases"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, LESSOR.ordinal());
    assertEquals(1, LESSEE.ordinal());

  }

  @SpecRef({"4.5", "registry:leases"})
  @Test
  void testDimensionEnumValues() {

    final var values = Dimension.values();

    assertEquals(2, values.length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpire() {

    emit(new Signal(Sign.EXPIRE, LESSOR), () -> lease.expire(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpired() {

    emit(new Signal(Sign.EXPIRE, LESSEE), () -> lease.expire(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExtend() {

    emit(new Signal(Sign.EXTEND, LESSOR), () -> lease.extend(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExtended() {

    emit(new Signal(Sign.EXTEND, LESSEE), () -> lease.extend(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testGrant() {

    emit(new Signal(Sign.GRANT, LESSOR), () -> lease.grant(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testGranted() {

    emit(new Signal(Sign.GRANT, LESSEE), () -> lease.grant(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    lease.acquire(LESSEE);
    lease.grant(LESSOR);
    lease.renew(LESSEE);
    lease.extend(LESSOR);

    assertEquals(
      4L,
      captures
        .drain()
        .count()
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProbe() {

    emit(new Signal(Sign.PROBE, LESSOR), () -> lease.probe(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProbed() {

    emit(new Signal(Sign.PROBE, LESSEE), () -> lease.probe(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRelease() {

    emit(new Signal(Sign.RELEASE, LESSEE), () -> lease.release(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReleased() {

    emit(new Signal(Sign.RELEASE, LESSOR), () -> lease.release(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRenew() {

    emit(new Signal(Sign.RENEW, LESSEE), () -> lease.renew(LESSEE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRenewed() {

    emit(new Signal(Sign.RENEW, LESSOR), () -> lease.renew(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRevoke() {

    emit(new Signal(Sign.REVOKE, LESSOR), () -> lease.revoke(LESSOR));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRevoked() {

    emit(new Signal(Sign.REVOKE, LESSEE), () -> lease.revoke(LESSEE));

  }

  @SpecRef({"4.1", "registry:leases"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, Sign.ACQUIRE.ordinal());
    assertEquals(1, Sign.DENY.ordinal());
    assertEquals(2, Sign.EXTEND.ordinal());
    assertEquals(3, Sign.EXPIRE.ordinal());
    assertEquals(4, Sign.GRANT.ordinal());
    assertEquals(5, Sign.PROBE.ordinal());
    assertEquals(6, Sign.RELEASE.ordinal());
    assertEquals(7, Sign.RENEW.ordinal());
    assertEquals(8, Sign.REVOKE.ordinal());

  }

  @SpecRef({"4.5", "registry:leases"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(9, values.length);

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    lease.signal(Sign.ACQUIRE, LESSEE);
    lease.signal(Sign.ACQUIRE, LESSOR);
    lease.signal(Sign.DENY, LESSOR);
    lease.signal(Sign.DENY, LESSEE);
    lease.signal(Sign.EXTEND, LESSOR);
    lease.signal(Sign.EXTEND, LESSEE);
    lease.signal(Sign.EXPIRE, LESSOR);
    lease.signal(Sign.EXPIRE, LESSEE);
    lease.signal(Sign.GRANT, LESSOR);
    lease.signal(Sign.GRANT, LESSEE);
    lease.signal(Sign.PROBE, LESSOR);
    lease.signal(Sign.PROBE, LESSEE);
    lease.signal(Sign.RELEASE, LESSEE);
    lease.signal(Sign.RELEASE, LESSOR);
    lease.signal(Sign.RENEW, LESSEE);
    lease.signal(Sign.RENEW, LESSOR);
    lease.signal(Sign.REVOKE, LESSOR);
    lease.signal(Sign.REVOKE, LESSEE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(18, signals.size());
    assertEquals(new Signal(Sign.ACQUIRE, LESSEE), signals.get(0));
    assertEquals(new Signal(Sign.ACQUIRE, LESSOR), signals.get(1));
    assertEquals(new Signal(Sign.DENY, LESSOR), signals.get(2));
    assertEquals(new Signal(Sign.DENY, LESSEE), signals.get(3));
    assertEquals(new Signal(Sign.EXTEND, LESSOR), signals.get(4));
    assertEquals(new Signal(Sign.EXTEND, LESSEE), signals.get(5));
    assertEquals(new Signal(Sign.EXPIRE, LESSOR), signals.get(6));
    assertEquals(new Signal(Sign.EXPIRE, LESSEE), signals.get(7));
    assertEquals(new Signal(Sign.GRANT, LESSOR), signals.get(8));
    assertEquals(new Signal(Sign.GRANT, LESSEE), signals.get(9));
    assertEquals(new Signal(Sign.PROBE, LESSOR), signals.get(10));
    assertEquals(new Signal(Sign.PROBE, LESSEE), signals.get(11));
    assertEquals(new Signal(Sign.RELEASE, LESSEE), signals.get(12));
    assertEquals(new Signal(Sign.RELEASE, LESSOR), signals.get(13));
    assertEquals(new Signal(Sign.RENEW, LESSEE), signals.get(14));
    assertEquals(new Signal(Sign.RENEW, LESSOR), signals.get(15));
    assertEquals(new Signal(Sign.REVOKE, LESSOR), signals.get(16));
    assertEquals(new Signal(Sign.REVOKE, LESSEE), signals.get(17));

  }

  @SpecRef("4.4")
  @Test
  void testSignalAccessors() {

    final var acquire = new Signal(Sign.ACQUIRE, LESSEE);
    assertEquals(Sign.ACQUIRE, acquire.sign());
    assertEquals(LESSEE, acquire.dimension());

    final var grant = new Signal(Sign.GRANT, LESSOR);
    assertEquals(Sign.GRANT, grant.sign());
    assertEquals(LESSOR, grant.dimension());

    final var renew = new Signal(Sign.RENEW, LESSEE);
    assertEquals(Sign.RENEW, renew.sign());
    assertEquals(LESSEE, renew.dimension());

    final var expire = new Signal(Sign.EXPIRE, LESSOR);
    assertEquals(Sign.EXPIRE, expire.sign());
    assertEquals(LESSOR, expire.dimension());

  }

  @SpecRef({"4.5", "registry:leases"})
  @Test
  void testSignalCoverage() {

    // 9 signs × 2 dimensions = 18 signal combinations
    assertEquals(9, Sign.values().length);
    assertEquals(2, Dimension.values().length);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    lease.acquire(LESSEE);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(Sign.ACQUIRE, LESSEE), capture.emission());

  }

}
