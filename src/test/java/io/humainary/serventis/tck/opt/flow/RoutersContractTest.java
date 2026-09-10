// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.flow;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.flow.Routers.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.flow.Routers.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Routers] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class RoutersContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("edge01.eth0");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Router router;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    router =
      conduit.pool(Routers::of)
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
  void testCorrupt() {

    router.corrupt();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CORRUPT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDrop() {

    router.drop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DROP, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testForward() {

    router.forward();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FORWARD, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFragment() {

    router.fragment();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FRAGMENT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPacketLifecycle() {

    // Normal packet flow: receive -> route -> forward
    router.receive();
    router.route();
    router.forward();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(RECEIVE, signs.get(0));
    assertEquals(ROUTE, signs.get(1));
    assertEquals(FORWARD, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPacketLifecycleWithFragmentation() {

    // Packet flow with fragmentation: receive -> route -> fragment -> send
    router.receive();
    router.route();
    router.fragment();
    router.send();
    router.send();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(RECEIVE, signs.get(0));
    assertEquals(ROUTE, signs.get(1));
    assertEquals(FRAGMENT, signs.get(2));
    assertEquals(SEND, signs.get(3));
    assertEquals(SEND, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReassemble() {

    router.reassemble();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REASSEMBLE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReceive() {

    router.receive();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RECEIVE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReorder() {

    router.reorder();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REORDER, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRoute() {

    router.route();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ROUTE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSend() {

    router.send();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SEND, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    router.sign(SEND);
    router.sign(RECEIVE);
    router.sign(FORWARD);
    router.sign(ROUTE);
    router.sign(DROP);
    router.sign(FRAGMENT);
    router.sign(REASSEMBLE);
    router.sign(CORRUPT);
    router.sign(REORDER);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(9, signs.size());
    assertEquals(SEND, signs.get(0));
    assertEquals(RECEIVE, signs.get(1));
    assertEquals(FORWARD, signs.get(2));
    assertEquals(ROUTE, signs.get(3));
    assertEquals(DROP, signs.get(4));
    assertEquals(FRAGMENT, signs.get(5));
    assertEquals(REASSEMBLE, signs.get(6));
    assertEquals(CORRUPT, signs.get(7));
    assertEquals(REORDER, signs.get(8));

  }

  @SpecRef({"4.1", "registry:routers"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, SEND.ordinal());
    assertEquals(1, RECEIVE.ordinal());
    assertEquals(2, FORWARD.ordinal());
    assertEquals(3, ROUTE.ordinal());
    assertEquals(4, DROP.ordinal());
    assertEquals(5, FRAGMENT.ordinal());
    assertEquals(6, REASSEMBLE.ordinal());
    assertEquals(7, CORRUPT.ordinal());
    assertEquals(8, REORDER.ordinal());

  }

  @SpecRef({"4.5", "registry:routers"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(9, values.length);
    assertEquals(SEND, values[0]);
    assertEquals(RECEIVE, values[1]);
    assertEquals(FORWARD, values[2]);
    assertEquals(ROUTE, values[3]);
    assertEquals(DROP, values[4]);
    assertEquals(FRAGMENT, values[5]);
    assertEquals(REASSEMBLE, values[6]);
    assertEquals(CORRUPT, values[7]);
    assertEquals(REORDER, values[8]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    router.send();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(SEND, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTrafficWithCongestion() {

    // Simulate traffic with congestion: receive packets, some forwarded, some dropped
    router.receive();
    router.route();
    router.forward();

    router.receive();
    router.route();
    router.forward();

    router.receive();
    router.route();
    router.drop();

    router.receive();
    router.route();
    router.drop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(12, signs.size());

    // Count different sign types
    final var receives = signs.stream().filter(s -> s==RECEIVE).count();
    final var routes = signs.stream().filter(s -> s==ROUTE).count();
    final var forwards = signs.stream().filter(s -> s==FORWARD).count();
    final var drops = signs.stream().filter(s -> s==DROP).count();

    assertEquals(4, receives);
    assertEquals(4, routes);
    assertEquals(2, forwards);
    assertEquals(2, drops);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTrafficWithCorruption() {

    // Simulate receiving corrupted packets
    router.receive();
    router.corrupt();
    router.drop();

    router.receive();
    router.route();
    router.forward();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(RECEIVE, signs.get(0));
    assertEquals(CORRUPT, signs.get(1));
    assertEquals(DROP, signs.get(2));
    assertEquals(RECEIVE, signs.get(3));
    assertEquals(ROUTE, signs.get(4));
    assertEquals(FORWARD, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTrafficWithReordering() {

    // Simulate out-of-order packet arrival
    router.receive();
    router.reorder();
    router.route();
    router.forward();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(RECEIVE, signs.get(0));
    assertEquals(REORDER, signs.get(1));
    assertEquals(ROUTE, signs.get(2));
    assertEquals(FORWARD, signs.get(3));

  }

}
