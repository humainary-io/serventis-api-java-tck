// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.flow;

import io.humainary.serventis.opt.flow.*;
import io.humainary.serventis.opt.flow.Guards.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.*;

import static io.humainary.serventis.opt.flow.Guards.Dimension.*;
import static io.humainary.serventis.opt.flow.Guards.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Guards] API.
/// @author William David Louth
/// @since 3.6

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md")
final class GuardsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("api.gateway.auth");

  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Guard guard;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    guard =
      conduit.pool(Guards::of)
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
  void testConvenienceMethodsEmitTheirSign() {

    final Map< Sign, Consumer< Dimension > > methods =
      new EnumMap<>(Sign.class);

    methods.put(ATTEMPT, guard::attempt);
    methods.put(CHALLENGE, guard::challenge);
    methods.put(GRANT, guard::grant);
    methods.put(DENY, guard::deny);

    assertEquals(Sign.values().length, methods.size());

    methods.forEach(
      (sign, method) -> {
        for (final var dimension : Dimension.values()) {
          method.accept(dimension);
        }
      }
    );

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(Sign.values().length * Dimension.values().length, signals.size());

    var index = 0;

    for (final var sign : methods.keySet()) {
      for (final var dimension : Dimension.values()) {
        assertEquals(new Signal(sign, dimension), signals.get(index++));
      }
    }

  }

  @SpecRef({"4.1", "registry:guards"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, IDENTITY.ordinal());
    assertEquals(1, PERMISSION.ordinal());

  }

  @SpecRef({"4.5", "registry:guards"})
  @Test
  void testDimensionEnumValues() {

    assertEquals(2, Dimension.values().length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testLoginThenRefusedPermission() {

    guard.attempt(IDENTITY);
    guard.challenge(IDENTITY);
    guard.grant(IDENTITY);
    guard.attempt(PERMISSION);
    guard.deny(PERMISSION);

    assertEquals(
      List.of(
        new Signal(ATTEMPT, IDENTITY),
        new Signal(CHALLENGE, IDENTITY),
        new Signal(GRANT, IDENTITY),
        new Signal(ATTEMPT, PERMISSION),
        new Signal(DENY, PERMISSION)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef({"4.1", "registry:guards"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, ATTEMPT.ordinal());
    assertEquals(1, CHALLENGE.ordinal());
    assertEquals(2, GRANT.ordinal());
    assertEquals(3, DENY.ordinal());

  }

  @SpecRef({"4.5", "registry:guards"})
  @Test
  void testSignEnumValues() {

    assertEquals(4, Sign.values().length);

  }

  @Test
  void testSignalCaching() {

    guard.deny(IDENTITY);
    guard.deny(IDENTITY);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());
    assertSame(signals.get(0), signals.get(1));

  }

  @SpecRef("6.2")
  @Test
  void testSignalMethod() {

    guard.signal(DENY, IDENTITY);
    guard.signal(GRANT, PERMISSION);

    assertEquals(
      List.of(
        new Signal(DENY, IDENTITY),
        new Signal(GRANT, PERMISSION)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    guard.attempt(IDENTITY);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(ATTEMPT, IDENTITY), capture.emission());

  }

}
