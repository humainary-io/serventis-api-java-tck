// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.tool;

import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.opt.tool.Probes.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.tool.Probes.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Probe] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.12.0/SPEC.md")
final class ProbesContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("service.1");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Probe probe;

  private void assertSignal(
    final Signal signal
  ) {

    // Every capture, not just the matching ones: a named operation must emit
    // exactly the one signal it names (SPEC §6.3, §6.5).
    final var emitted = captures.drain().toList();

    assertEquals(1, emitted.size(), "exactly one signal per operation");
    assertEquals(signal, emitted.getFirst().emission());
    assertSame(NAME, emitted.getFirst().subject().name());

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

    probe =
      Probes.pool(conduit)
        .get(NAME);

    captures =
      CaptureBuffer.of(circuit, conduit);

  }

  @AfterEach
  void teardown() {

    circuit
      .closeAwait();

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testConnect() {

    emit(new Signal(Sign.CONNECT, OUTBOUND), () -> probe.connect(OUTBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testConnected() {

    emit(new Signal(Sign.CONNECT, INBOUND), () -> probe.connect(INBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDisconnect() {

    emit(new Signal(Sign.DISCONNECT, OUTBOUND), () -> probe.disconnect(OUTBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDisconnected() {

    emit(new Signal(Sign.DISCONNECT, INBOUND), () -> probe.disconnect(INBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFail() {

    emit(new Signal(Sign.FAIL, OUTBOUND), () -> probe.fail(OUTBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailed() {

    emit(new Signal(Sign.FAIL, INBOUND), () -> probe.fail(INBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    probe.connect(OUTBOUND);
    probe.transfer(OUTBOUND);
    probe.succeed(OUTBOUND);
    probe.disconnect(OUTBOUND);

    // The whole sequence, not a count: a wrong sign, a swapped dimension or a
    // reordering (§6.5) would keep the count and change this.
    assertEquals(
      java.util.List.of(
        new Signal(Sign.CONNECT, OUTBOUND),
        new Signal(Sign.TRANSFER, OUTBOUND),
        new Signal(Sign.SUCCEED, OUTBOUND),
        new Signal(Sign.DISCONNECT, OUTBOUND)
      ),
      captures.drainEmissions().toList()
    );

  }

  @SpecRef({"4.1", "registry:probes"})
  @Test
  void testOrientationEnumOrdinals() {

    assertEquals(0, OUTBOUND.ordinal());
    assertEquals(1, INBOUND.ordinal());

  }

  @SpecRef({"4.5", "registry:probes"})
  @Test
  void testOrientationEnumValues() {

    final var values = Dimension.values();

    assertEquals(2, values.length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProcess() {

    emit(new Signal(Sign.PROCESS, OUTBOUND), () -> probe.process(OUTBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProcessed() {

    emit(new Signal(Sign.PROCESS, INBOUND), () -> probe.process(INBOUND));

  }

  @SpecRef({"4.1", "registry:probes"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, Sign.CONNECT.ordinal());
    assertEquals(1, Sign.DISCONNECT.ordinal());
    assertEquals(2, Sign.TRANSFER.ordinal());
    assertEquals(3, Sign.PROCESS.ordinal());
    assertEquals(4, Sign.SUCCEED.ordinal());
    assertEquals(5, Sign.FAIL.ordinal());

  }

  @SpecRef({"4.5", "registry:probes"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(6, values.length);

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // The direct signal() method for every sign and dimension combination, once each
    final var expected = new java.util.ArrayList< Signal >();

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        probe.signal(sign, dimension);
        expected.add(new Signal(sign, dimension));
      }
    }

    assertEquals(
      expected,
      captures.drainEmissions().toList()
    );

  }

  @SpecRef("4.4")
  @Test
  void testSignalAccessors() {

    final var connect = new Signal(Sign.CONNECT, OUTBOUND);
    assertEquals(Sign.CONNECT, connect.sign());
    assertEquals(OUTBOUND, connect.dimension());

    final var connected = new Signal(Sign.CONNECT, INBOUND);
    assertEquals(Sign.CONNECT, connected.sign());
    assertEquals(INBOUND, connected.dimension());

    final var succeed = new Signal(Sign.SUCCEED, OUTBOUND);
    assertEquals(Sign.SUCCEED, succeed.sign());
    assertEquals(OUTBOUND, succeed.dimension());

    final var failed = new Signal(Sign.FAIL, INBOUND);
    assertEquals(Sign.FAIL, failed.sign());
    assertEquals(INBOUND, failed.dimension());

  }

  @SpecRef({"4.5", "registry:probes"})
  @Test
  void testSignalCoverage() {

    // 6 signs × 2 dimensions = 12 signal combinations
    assertEquals(6, Sign.values().length);
    assertEquals(2, Dimension.values().length);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    probe.connect(OUTBOUND);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(Sign.CONNECT, OUTBOUND), capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSucceed() {

    emit(new Signal(Sign.SUCCEED, OUTBOUND), () -> probe.succeed(OUTBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSucceeded() {

    emit(new Signal(Sign.SUCCEED, INBOUND), () -> probe.succeed(INBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransferInbound() {

    emit(new Signal(Sign.TRANSFER, INBOUND), () -> probe.transfer(INBOUND));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransferOutbound() {

    emit(new Signal(Sign.TRANSFER, OUTBOUND), () -> probe.transfer(OUTBOUND));

  }

}
