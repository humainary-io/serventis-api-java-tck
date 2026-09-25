// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.data;

import io.humainary.serventis.opt.data.*;
import io.humainary.serventis.opt.data.Messages.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.*;

import static io.humainary.serventis.opt.data.Messages.Dimension.*;
import static io.humainary.serventis.opt.data.Messages.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Messages] API.
/// @author William David Louth
/// @since 3.6

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md")
final class MessagesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("orders.events");

  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Message message;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    message =
      conduit.pool(Messages::of)
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

    methods.put(PUBLISH, message::publish);
    methods.put(DELIVER, message::deliver);
    methods.put(ACK, message::ack);
    methods.put(NACK, message::nack);
    methods.put(REDELIVER, message::redeliver);
    methods.put(EXHAUST, message::exhaust);
    methods.put(EXPIRE, message::expire);

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

  @SpecRef({"4.1", "registry:messages"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, PRODUCER.ordinal());
    assertEquals(1, BROKER.ordinal());
    assertEquals(2, CONSUMER.ordinal());

  }

  @SpecRef({"4.5", "registry:messages"})
  @Test
  void testDimensionEnumValues() {

    assertEquals(3, Dimension.values().length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRedeliveryUntilExhausted() {

    message.publish(PRODUCER);
    message.ack(PRODUCER);
    message.deliver(BROKER);
    message.nack(CONSUMER);
    message.redeliver(BROKER);
    message.nack(CONSUMER);
    message.exhaust(BROKER);

    assertEquals(
      List.of(
        new Signal(PUBLISH, PRODUCER),
        new Signal(ACK, PRODUCER),
        new Signal(DELIVER, BROKER),
        new Signal(NACK, CONSUMER),
        new Signal(REDELIVER, BROKER),
        new Signal(NACK, CONSUMER),
        new Signal(EXHAUST, BROKER)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef({"4.1", "registry:messages"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, PUBLISH.ordinal());
    assertEquals(1, DELIVER.ordinal());
    assertEquals(2, ACK.ordinal());
    assertEquals(3, NACK.ordinal());
    assertEquals(4, REDELIVER.ordinal());
    assertEquals(5, EXHAUST.ordinal());
    assertEquals(6, EXPIRE.ordinal());

  }

  @SpecRef({"4.5", "registry:messages"})
  @Test
  void testSignEnumValues() {

    assertEquals(7, Sign.values().length);

  }

  @Test
  void testSignalCaching() {

    message.ack(CONSUMER);
    message.ack(CONSUMER);

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

    message.signal(EXPIRE, BROKER);
    message.signal(DELIVER, CONSUMER);

    assertEquals(
      List.of(
        new Signal(EXPIRE, BROKER),
        new Signal(DELIVER, CONSUMER)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    message.publish(PRODUCER);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(PUBLISH, PRODUCER), capture.emission());

  }

}
