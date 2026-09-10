// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Timers.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.exec.Timers.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Timer] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class TimersContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("api.latency");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Timer timer;

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

    timer =
      conduit.pool(Timers::of)
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

  @SpecRef({"4.1", "registry:timers"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, DEADLINE.ordinal());
    assertEquals(1, THRESHOLD.ordinal());

  }

  @SpecRef({"4.5", "registry:timers"})
  @Test
  void testDimensionEnumValues() {

    final var values = Dimension.values();

    assertEquals(2, values.length);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMeetDeadline() {

    emit(new Signal(Sign.MEET, DEADLINE), () -> timer.meet(DEADLINE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMeetThreshold() {

    emit(new Signal(Sign.MEET, THRESHOLD), () -> timer.meet(THRESHOLD));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMissDeadline() {

    emit(new Signal(Sign.MISS, DEADLINE), () -> timer.miss(DEADLINE));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMissThreshold() {

    emit(new Signal(Sign.MISS, THRESHOLD), () -> timer.miss(THRESHOLD));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    timer.meet(THRESHOLD);
    timer.meet(THRESHOLD);
    timer.miss(THRESHOLD);
    timer.meet(DEADLINE);

    assertEquals(
      4L,
      captures
        .drain()
        .count()
    );

  }

  @SpecRef({"4.1", "registry:timers"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, Sign.MEET.ordinal());
    assertEquals(1, Sign.MISS.ordinal());

  }

  @SpecRef({"4.5", "registry:timers"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(2, values.length);

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    timer.signal(Sign.MEET, DEADLINE);
    timer.signal(Sign.MEET, THRESHOLD);
    timer.signal(Sign.MISS, DEADLINE);
    timer.signal(Sign.MISS, THRESHOLD);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(new Signal(Sign.MEET, DEADLINE), signals.get(0));
    assertEquals(new Signal(Sign.MEET, THRESHOLD), signals.get(1));
    assertEquals(new Signal(Sign.MISS, DEADLINE), signals.get(2));
    assertEquals(new Signal(Sign.MISS, THRESHOLD), signals.get(3));

  }

  @SpecRef("4.4")
  @Test
  void testSignalAccessors() {

    final var meetDeadline = new Signal(Sign.MEET, DEADLINE);
    assertEquals(Sign.MEET, meetDeadline.sign());
    assertEquals(DEADLINE, meetDeadline.dimension());

    final var meetThreshold = new Signal(Sign.MEET, THRESHOLD);
    assertEquals(Sign.MEET, meetThreshold.sign());
    assertEquals(THRESHOLD, meetThreshold.dimension());

    final var missDeadline = new Signal(Sign.MISS, DEADLINE);
    assertEquals(Sign.MISS, missDeadline.sign());
    assertEquals(DEADLINE, missDeadline.dimension());

    final var missThreshold = new Signal(Sign.MISS, THRESHOLD);
    assertEquals(Sign.MISS, missThreshold.sign());
    assertEquals(THRESHOLD, missThreshold.dimension());

  }

  @SpecRef({"4.5", "registry:timers"})
  @Test
  void testSignalCoverage() {

    // 2 signs × 2 dimensions = 4 signal combinations
    assertEquals(2, Sign.values().length);
    assertEquals(2, Dimension.values().length);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    timer.meet(THRESHOLD);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(Sign.MEET, THRESHOLD), capture.emission());

  }

}
