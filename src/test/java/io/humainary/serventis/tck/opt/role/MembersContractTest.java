// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.role;

import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.role.Members.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.*;

import static io.humainary.serventis.opt.role.Members.Dimension.*;
import static io.humainary.serventis.opt.role.Members.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Members] API.
/// @author William David Louth
/// @since 3.6

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md")
final class MembersContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("cluster.node3");

  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Member member;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    member =
      conduit.pool(Members::of)
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

    methods.put(JOIN, member::join);
    methods.put(LEAVE, member::leave);
    methods.put(SUSPECT, member::suspect);
    methods.put(REFUTE, member::refute);
    methods.put(EVICT, member::evict);
    methods.put(ELECT, member::elect);
    methods.put(RESIGN, member::resign);

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

  @SpecRef({"4.1", "registry:members"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, SELF.ordinal());
    assertEquals(1, PEER.ordinal());

  }

  @SpecRef({"4.5", "registry:members"})
  @Test
  void testDimensionEnumValues() {

    assertEquals(2, Dimension.values().length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspicionEndsInEviction() {

    member.join(PEER);
    member.suspect(PEER);
    member.evict(PEER);
    member.evict(SELF);

    assertEquals(
      List.of(
        new Signal(JOIN, PEER),
        new Signal(SUSPECT, PEER),
        new Signal(EVICT, PEER),
        new Signal(EVICT, SELF)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef({"4.1", "registry:members"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, JOIN.ordinal());
    assertEquals(1, LEAVE.ordinal());
    assertEquals(2, SUSPECT.ordinal());
    assertEquals(3, REFUTE.ordinal());
    assertEquals(4, EVICT.ordinal());
    assertEquals(5, ELECT.ordinal());
    assertEquals(6, RESIGN.ordinal());

  }

  @SpecRef({"4.5", "registry:members"})
  @Test
  void testSignEnumValues() {

    assertEquals(7, Sign.values().length);

  }

  @Test
  void testSignalCaching() {

    member.suspect(PEER);
    member.suspect(PEER);

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

    member.signal(ELECT, SELF);
    member.signal(REFUTE, PEER);

    assertEquals(
      List.of(
        new Signal(ELECT, SELF),
        new Signal(REFUTE, PEER)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    member.suspect(PEER);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(SUSPECT, PEER), capture.emission());

  }

}
