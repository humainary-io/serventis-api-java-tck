// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.pool;

import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.opt.pool.Exchanges.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Exchanges.Dimension.*;
import static io.humainary.serventis.opt.pool.Exchanges.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Exchanges] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class ExchangesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("trade.orders");

  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Exchange exchange;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    exchange =
      conduit.pool(Exchanges::of)
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

  // ========== SIGN TESTS ==========

  @Test
  void testAllSignalCombinationsCached() {

    // First pass
    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        exchange.signal(sign, dimension);
      }
    }

    final var firstPass =
      captures
        .drainEmissions()
        .toList();

    // Second pass
    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        exchange.signal(sign, dimension);
      }
    }

    final var secondPass =
      captures
        .drainEmissions()
        .toList();

    // Verify caching
    assertEquals(firstPass.size(), secondPass.size());
    for (var i = 0; i < firstPass.size(); i++) {
      assertSame(firstPass.get(i), secondPass.get(i));
    }

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testContract() {

    exchange.contract(PROVIDER);
    exchange.contract(RECEIVER);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertEquals(CONTRACT, signals.get(0).sign());
    assertEquals(PROVIDER, signals.get(0).dimension());
    assertEquals(CONTRACT, signals.get(1).sign());
    assertEquals(RECEIVER, signals.get(1).dimension());

  }

  // ========== PATTERN TESTS ==========

  @SpecRef({"4.1", "registry:exchanges"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, PROVIDER.ordinal());
    assertEquals(1, RECEIVER.ordinal());

  }

  @SpecRef({"4.5", "registry:exchanges"})
  @Test
  void testDimensionEnumValues() {

    assertEquals(2, Dimension.values().length);

  }

  // ========== GENERIC SIGNAL TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExchangerRendezvousPattern() {

    // Java Exchanger pattern: both threads contract, then swap
    // Thread 1
    exchange.contract(PROVIDER);
    // Thread 2
    exchange.contract(PROVIDER);
    // Swap occurs
    exchange.transfer(PROVIDER);
    exchange.transfer(RECEIVER);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());

  }

  // ========== ENUM TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testREAExchangePattern() {

    // REA pattern: both sides contract, then transfer
    exchange.contract(PROVIDER);   // Provider commits to give
    exchange.contract(RECEIVER);   // Receiver commits to take
    exchange.transfer(PROVIDER);   // Provider transfers out
    exchange.transfer(RECEIVER);   // Receiver receives

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(CONTRACT, signals.get(0).sign());
    assertEquals(PROVIDER, signals.get(0).dimension());
    assertEquals(CONTRACT, signals.get(1).sign());
    assertEquals(RECEIVER, signals.get(1).dimension());
    assertEquals(TRANSFER, signals.get(2).sign());
    assertEquals(PROVIDER, signals.get(2).dimension());
    assertEquals(TRANSFER, signals.get(3).sign());
    assertEquals(RECEIVER, signals.get(3).dimension());

  }

  @SpecRef({"4.1", "registry:exchanges"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, CONTRACT.ordinal());
    assertEquals(1, TRANSFER.ordinal());

  }

  @SpecRef({"4.5", "registry:exchanges"})
  @Test
  void testSignEnumValues() {

    assertEquals(2, Sign.values().length);

  }

  @Test
  void testSignalCaching() {

    exchange.contract(PROVIDER);
    exchange.contract(PROVIDER);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertSame(signals.get(0), signals.get(1));

  }

  // ========== SIGNAL CACHING TESTS ==========

  @SpecRef("6.2")
  @Test
  void testSignalMethod() {

    exchange.signal(CONTRACT, PROVIDER);
    exchange.signal(TRANSFER, RECEIVER);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertEquals(CONTRACT, signals.get(0).sign());
    assertEquals(PROVIDER, signals.get(0).dimension());
    assertEquals(TRANSFER, signals.get(1).sign());
    assertEquals(RECEIVER, signals.get(1).dimension());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    exchange.contract(PROVIDER);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(CONTRACT, capture.emission().sign());
    assertEquals(PROVIDER, capture.emission().dimension());

  }

  // ========== SUBJECT TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransfer() {

    exchange.transfer(PROVIDER);
    exchange.transfer(RECEIVER);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertEquals(TRANSFER, signals.get(0).sign());
    assertEquals(PROVIDER, signals.get(0).dimension());
    assertEquals(TRANSFER, signals.get(1).sign());
    assertEquals(RECEIVER, signals.get(1).dimension());

  }

}
