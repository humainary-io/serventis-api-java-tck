// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.flow;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.flow.Valves.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.flow.Valves.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Valves] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class ValvesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("api.ratelimit");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Valve valve;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    valve =
      conduit.pool(Valves::of)
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
  void testAdaptiveControlPattern() {

    // Simulate adaptive control: expand, then flow, then contract
    valve.expand();
    valve.pass();
    valve.pass();
    valve.deny();
    valve.contract();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(EXPAND, signs.getFirst());
    assertEquals(PASS, signs.get(1));
    assertEquals(PASS, signs.get(2));
    assertEquals(DENY, signs.get(3));
    assertEquals(CONTRACT, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAllSigns() {

    valve.pass();
    valve.deny();
    valve.expand();
    valve.contract();
    valve.drop();
    valve.drain();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(PASS, signs.getFirst());
    assertEquals(DENY, signs.get(1));
    assertEquals(EXPAND, signs.get(2));
    assertEquals(CONTRACT, signs.get(3));
    assertEquals(DROP, signs.get(4));
    assertEquals(DRAIN, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testContract() {

    valve.contract();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CONTRACT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeny() {

    valve.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DENY, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDrain() {

    valve.drain();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DRAIN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpand() {

    valve.expand();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EXPAND, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFlowPattern() {

    // Simulate normal flow with some denials
    valve.pass();
    valve.pass();
    valve.deny();
    valve.pass();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());

    final var passCount =
      signs
        .stream()
        .filter(s -> s==PASS)
        .count();

    final var denyCount =
      signs
        .stream()
        .filter(s -> s==DENY)
        .count();

    assertEquals(3, passCount);
    assertEquals(1, denyCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testOverloadRecoveryPattern() {

    // Simulate overload and recovery
    valve.drop();
    valve.drop();
    valve.contract();
    valve.drain();
    valve.drain();
    valve.expand();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(DROP, signs.get(0));
    assertEquals(DROP, signs.get(1));
    assertEquals(CONTRACT, signs.get(2));
    assertEquals(DRAIN, signs.get(3));
    assertEquals(DRAIN, signs.get(4));
    assertEquals(EXPAND, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPass() {

    valve.pass();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PASS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSaturationPattern() {

    // Simulate saturation: many denials, no expansion
    valve.deny();
    valve.deny();
    valve.deny();
    valve.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());

    final var denyCount =
      signs
        .stream()
        .filter(s -> s==DENY)
        .count();

    assertEquals(4, denyCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testShed() {

    valve.drop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DROP, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    valve.sign(PASS);
    valve.sign(DENY);
    valve.sign(EXPAND);
    valve.sign(CONTRACT);
    valve.sign(DROP);
    valve.sign(DRAIN);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(PASS, signs.get(0));
    assertEquals(DENY, signs.get(1));
    assertEquals(EXPAND, signs.get(2));
    assertEquals(CONTRACT, signs.get(3));
    assertEquals(DROP, signs.get(4));
    assertEquals(DRAIN, signs.get(5));

  }

  @SpecRef({"4.1", "registry:valves"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, PASS.ordinal());
    assertEquals(1, DENY.ordinal());
    assertEquals(2, EXPAND.ordinal());
    assertEquals(3, CONTRACT.ordinal());
    assertEquals(4, DROP.ordinal());
    assertEquals(5, DRAIN.ordinal());

  }

  @SpecRef({"4.5", "registry:valves"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(6, values.length);
    assertEquals(PASS, values[0]);
    assertEquals(DENY, values[1]);
    assertEquals(EXPAND, values[2]);
    assertEquals(CONTRACT, values[3]);
    assertEquals(DROP, values[4]);
    assertEquals(DRAIN, values[5]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    valve.pass();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(PASS, capture.emission());

  }

}
