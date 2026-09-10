// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk.meta;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.pool.*;
import io.humainary.serventis.sdk.meta.*;
import io.humainary.serventis.sdk.meta.Cycles.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.pool.Resources.Sign.*;
import static io.humainary.serventis.sdk.meta.Cycles.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class CyclesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("resource.cycles");

  private Circuit circuit;
  private CaptureBuffer< Signal< Resources.Sign > > captures;
  private Cycle< Resources.Sign > cycle;

  @Test
  void flowDetectsCyclePattern() {

    final Conduit< Signal< Resources.Sign > > conduit =
      (Conduit< Signal< Resources.Sign > >) (Conduit< ? >)
        circuit.conduit(Signal.class);

    final var in =
      conduit.pool(Cycles.flow(Resources.SIGNS))
        .get(CORTEX.name("resource.cycles.flow"));

    final var captures =
      CaptureBuffer.of(circuit, conduit);

    // Stream: GRANT, GRANT, DENY, GRANT
    in.emit(GRANT);
    in.emit(GRANT);
    in.emit(DENY);
    in.emit(GRANT);

    final var signals =
      captures.drainEmissions().toList();
    captures.close();

    assertEquals(4, signals.size());
    assertEquals(GRANT, signals.get(0).sign());
    assertEquals(SINGLE, signals.get(0).dimension());
    assertEquals(GRANT, signals.get(1).sign());
    assertEquals(REPEAT, signals.get(1).dimension());
    assertEquals(DENY, signals.get(2).sign());
    assertEquals(SINGLE, signals.get(2).dimension());
    assertEquals(GRANT, signals.get(3).sign());
    assertEquals(RETURN, signals.get(3).dimension());

  }

  @Test
  void flowIsPerSubject() {

    final Conduit< Signal< Resources.Sign > > conduit =
      (Conduit< Signal< Resources.Sign > >) (Conduit< ? >)
        circuit.conduit(Signal.class);

    final var pool = conduit.pool(Cycles.flow(Resources.SIGNS));
    final var name1 = CORTEX.name("resource.a.cycles");
    final var name2 = CORTEX.name("resource.b.cycles");
    final var in1 = pool.get(name1);
    final var in2 = pool.get(name2);
    final var captures = CaptureBuffer.of(circuit, conduit);

    in1.emit(GRANT);   // subject a: SINGLE
    in2.emit(GRANT);   // subject b: SINGLE (independent history)
    in1.emit(GRANT);   // subject a: REPEAT

    final var captured = captures.drain().toList();
    captures.close();

    assertEquals(3, captured.size());
    assertEquals(name1, captured.get(0).subject().name());
    assertEquals(SINGLE, captured.get(0).emission().dimension());
    assertEquals(name2, captured.get(1).subject().name());
    assertEquals(SINGLE, captured.get(1).emission().dimension());
    assertEquals(name1, captured.get(2).subject().name());
    assertEquals(REPEAT, captured.get(2).emission().dimension());

  }

  // ========== INDIVIDUAL DIMENSION TESTS ==========

  @Test
  void flowRejectsNullArguments() {

    assertThrows(
      NullPointerException.class,
      () -> Cycles.< Resources.Sign > flow(null)
    );

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      (Conduit< Signal< Resources.Sign > >) (Conduit< ? >)
        circuit.conduit(
          Signal.class
        );

    cycle =
      Cycles.pool(Resources.SIGNS, conduit)
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

  // ========== PATTERN TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testConsecutiveRepeats() {

    // Simulate: DENY, DENY, DENY, DENY
    cycle.signal(DENY, SINGLE);
    cycle.signal(DENY, REPEAT);
    cycle.signal(DENY, REPEAT);
    cycle.signal(DENY, REPEAT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(DENY, signals.get(0).sign());
    assertEquals(SINGLE, signals.get(0).dimension());
    assertEquals(DENY, signals.get(1).sign());
    assertEquals(REPEAT, signals.get(1).dimension());
    assertEquals(DENY, signals.get(2).sign());
    assertEquals(REPEAT, signals.get(2).dimension());
    assertEquals(DENY, signals.get(3).sign());
    assertEquals(REPEAT, signals.get(3).dimension());

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testCyclePattern() {

    // Simulate: GRANT, GRANT, DENY, GRANT
    cycle.signal(GRANT, SINGLE);   // First GRANT
    cycle.signal(GRANT, REPEAT);   // GRANT immediately after GRANT
    cycle.signal(DENY, SINGLE);    // First DENY
    cycle.signal(GRANT, RETURN);   // GRANT returns after DENY

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(GRANT, signals.get(0).sign());
    assertEquals(SINGLE, signals.get(0).dimension());
    assertEquals(GRANT, signals.get(1).sign());
    assertEquals(REPEAT, signals.get(1).dimension());
    assertEquals(DENY, signals.get(2).sign());
    assertEquals(SINGLE, signals.get(2).dimension());
    assertEquals(GRANT, signals.get(3).sign());
    assertEquals(RETURN, signals.get(3).dimension());

  }

  @SpecRef({"4.1", "7.9"})
  @Test
  void testDimensionEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, SINGLE.ordinal());
    assertEquals(1, REPEAT.ordinal());
    assertEquals(2, RETURN.ordinal());

  }

  // ========== ENUM STABILITY TESTS ==========

  @SpecRef({"4.5", "7.9"})
  @Test
  void testDimensionEnumValues() {

    final var values = Dimension.values();

    assertEquals(3, values.length);

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testMultipleReturns() {

    // Simulate: GRANT, DENY, GRANT, TIMEOUT, GRANT
    cycle.signal(GRANT, SINGLE);
    cycle.signal(DENY, SINGLE);
    cycle.signal(GRANT, RETURN);
    cycle.signal(TIMEOUT, SINGLE);
    cycle.signal(GRANT, RETURN);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signals.size());
    assertEquals(GRANT, signals.get(0).sign());
    assertEquals(SINGLE, signals.get(0).dimension());
    assertEquals(DENY, signals.get(1).sign());
    assertEquals(SINGLE, signals.get(1).dimension());
    assertEquals(GRANT, signals.get(2).sign());
    assertEquals(RETURN, signals.get(2).dimension());
    assertEquals(TIMEOUT, signals.get(3).sign());
    assertEquals(SINGLE, signals.get(3).dimension());
    assertEquals(GRANT, signals.get(4).sign());
    assertEquals(RETURN, signals.get(4).dimension());

  }

  // ========== SIGNAL CACHING TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testMultipleSubjects() {

    final var name1 = CORTEX.name("resource.pool1.cycles");
    final var name2 = CORTEX.name("resource.pool2.cycles");

    final var conduit =
      (Conduit< Signal< Resources.Sign > >) (Conduit< ? >)
        circuit.conduit(Signal.class);
    final var cycles = Cycles.pool(Resources.SIGNS, conduit);
    final var cycle1 = cycles.get(name1);
    final var cycle2 = cycles.get(name2);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    cycle1.signal(GRANT, SINGLE);
    cycle2.signal(DENY, SINGLE);
    cycle1.signal(GRANT, REPEAT);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(3, allSignals.size());
    assertEquals(name1, allSignals.get(0).subject().name());
    assertEquals(name2, allSignals.get(1).subject().name());
    assertEquals(name1, allSignals.get(2).subject().name());

  }

  @SpecRef({"6.2", "6.5"})
  @Test
  void testRepeat() {

    cycle.signal(GRANT, REPEAT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(GRANT, signals.getFirst().sign());
    assertEquals(REPEAT, signals.getFirst().dimension());

  }

  // ========== SUBJECT TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testReturn() {

    cycle.signal(GRANT, RETURN);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(GRANT, signals.getFirst().sign());
    assertEquals(RETURN, signals.getFirst().dimension());

  }

  @Test
  void testSignalCaching() {

    // Emit same signal twice
    cycle.signal(GRANT, SINGLE);
    cycle.signal(GRANT, SINGLE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signals.size());

    // Verify same Signal instance is reused (cached)
    assertSame(signals.get(0), signals.get(1));

  }

  // ========== GENERIC SIGN TYPE TESTS ==========

  @Test
  void testSignalCachingAllCombinations() {

    // Test caching for multiple sign/dimension combinations
    final var dimensions = Dimension.values();

    // First pass - emit for GRANT
    for (final var dimension : dimensions) {
      cycle.signal(GRANT, dimension);
    }

    final var firstPass =
      captures
        .drainEmissions()
        .toList();

    // Second pass - emit same signals
    for (final var dimension : dimensions) {
      cycle.signal(GRANT, dimension);
    }

    final var secondPass =
      captures
        .drainEmissions()
        .toList();

    // Verify all instances are cached (same reference)
    for (var i = 0; i < dimensions.length; i++) {
      assertSame(
        firstPass.get(i),
        secondPass.get(i),
        "Signal should be cached for GRANT × " + dimensions[i]
      );
    }

  }

  // ========== FLOW (canonical detector) TESTS ==========

  @SpecRef({"6.2", "6.5"})
  @Test
  void testSingle() {

    cycle.signal(GRANT, SINGLE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(GRANT, signals.getFirst().sign());
    assertEquals(SINGLE, signals.getFirst().dimension());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    cycle.signal(GRANT, SINGLE);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(GRANT, capture.emission().sign());
    assertEquals(SINGLE, capture.emission().dimension());

  }

  @SpecRef({"6.2", "6.5", "7.7"})
  @Test
  void testWithDifferentSignType() {

    // Test that Cycles works with different Sign types
    final var taskConduit =
      (Conduit< Signal< Tasks.Sign > >) (Conduit< ? >)
        circuit.conduit(
          Signal.class
        );

    final var taskCycle =
      Cycles.pool(Tasks.SIGNS, taskConduit)
        .get(CORTEX.name("task.cycles"));

    final var taskCaptures =
      CaptureBuffer.of(circuit, taskConduit);

    taskCycle.signal(
      Tasks.Sign.SUBMIT,
      SINGLE
    );

    taskCycle.signal(
      Tasks.Sign.COMPLETE,
      SINGLE
    );

    taskCycle.signal(
      Tasks.Sign.SUBMIT,
      RETURN
    );

    final var signals =
      taskCaptures
        .drainEmissions()
        .toList();
    taskCaptures.close();

    assertEquals(3, signals.size());
    assertEquals(
      Tasks.Sign.SUBMIT,
      signals.get(0).sign()
    );
    assertEquals(
      Tasks.Sign.COMPLETE,
      signals.get(1).sign()
    );
    assertEquals(
      Tasks.Sign.SUBMIT,
      signals.get(2).sign()
    );

  }

}
