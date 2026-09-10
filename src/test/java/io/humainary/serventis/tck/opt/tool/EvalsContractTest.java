// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.tool;

import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.opt.tool.Evals.*;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.opt.tool.Evals.Dimension.*;
import static io.humainary.serventis.opt.tool.Evals.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for the [Evals] API.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class EvalsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("support.answer");
  private CaptureBuffer< Signal > captures;
  private Circuit circuit;
  private Eval eval;

  @SpecRef({"4.5", "registry:evals"})
  @Test
  void dimensionsAreStable() {

    assertArrayEquals(
      new Dimension[]{
        CORRECTNESS,
        COMPLETENESS,
        GROUNDEDNESS,
        HELPFULNESS,
        ADHERENCE,
        TOOLING,
        ROUTING,
        SAFETY
      },
      Dimension.values()
    );

  }

  /// The generic operation emits the signal it was given — every one of them, unaltered and in
  /// order.
  ///
  /// [#reusesSignals()] beside this covers cardinality and repeated reference identity, which a
  /// table that consistently returned the *wrong* signal would satisfy just as well: the same
  /// wrong instance twice is still the same instance. This reads the components back.

  @SpecRef({"6.2", "6.5"})
  @Test
  void emitsTheSignalItWasGiven() {

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        eval.signal(sign, dimension);
      }
    }

    final var signals =
      captures
        .drainEmissions()
        .iterator();

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {

        assertTrue(
          signals.hasNext(),
          () -> "no signal emitted for " + sign + " x " + dimension
        );

        final var signal =
          signals.next();

        assertEquals(
          sign,
          signal.sign(),
          () -> "signal(" + sign + ", " + dimension + ") emitted the sign " + signal.sign()
        );

        assertEquals(
          dimension,
          signal.dimension(),
          () -> "signal(" + sign + ", " + dimension + ") emitted the dimension " + signal.dimension()
        );

      }
    }

    assertFalse(
      signals.hasNext(),
      "the instrument emitted more signals than it was given"
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void emitsVerdicts() {

    eval.pass(GROUNDEDNESS);
    eval.fail(ROUTING);
    eval.unknown(CORRECTNESS);
    eval.skip(HELPFULNESS);
    eval.error(SAFETY);

    assertEquals(
      java.util.List.of(
        new Signal(PASS, GROUNDEDNESS),
        new Signal(FAIL, ROUTING),
        new Signal(UNKNOWN, CORRECTNESS),
        new Signal(SKIP, HELPFULNESS),
        new Signal(ERROR, SAFETY)
      ),
      captures
        .drainEmissions()
        .toList()
    );

  }

  /// A judge that could not complete says nothing about the evaluated subject: `ERROR` must abstain
  /// from the status reading and must not count as a failure of the subject in the verdict tallies.

  @Test
  void evaluatorFailureDoesNotAccrueToSubject() {

    assertNull(Evals.STATUS.apply(ERROR));
    assertNull(Evals.STATUS.apply(SKIP));
    assertNull(Evals.STATUS.apply(UNKNOWN));

    assertNotEquals(Outcomes.Sign.FAIL, Evals.OUTCOME.apply(ERROR));

  }

  @Test
  void mapsSigns() {

    assertEquals(OUTCOME, Evals.KIND.apply(PASS));
    assertEquals(OUTCOME, Evals.KIND.apply(FAIL));
    assertEquals(OUTCOME, Evals.KIND.apply(UNKNOWN));
    assertEquals(OUTCOME, Evals.KIND.apply(SKIP));
    assertEquals(OUTCOME, Evals.KIND.apply(ERROR));

    assertEquals(Outcomes.Sign.SUCCESS, Evals.OUTCOME.apply(PASS));
    assertEquals(Outcomes.Sign.FAIL, Evals.OUTCOME.apply(FAIL));
    assertEquals(Outcomes.Sign.UNKNOWN, Evals.OUTCOME.apply(UNKNOWN));
    assertEquals(Outcomes.Sign.UNKNOWN, Evals.OUTCOME.apply(ERROR));
    assertNull(Evals.OUTCOME.apply(SKIP));

  }

  @SpecRef("6.4")
  @Test
  void retainsSubject() {

    eval.pass(CORRECTNESS);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(new Signal(PASS, CORRECTNESS), capture.emission());

  }

  @Test
  void reusesSignals() {

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        eval.signal(sign, dimension);
      }
    }

    for (final var sign : Sign.values()) {
      for (final var dimension : Dimension.values()) {
        eval.signal(sign, dimension);
      }
    }

    final var signals =
      captures
        .drainEmissions()
        .toList();

    final var cardinality =
      Sign.values().length * Dimension.values().length;

    assertEquals(cardinality * 2, signals.size());

    for (var index = 0; index < cardinality; index++) {
      assertSame(signals.get(index), signals.get(index + cardinality));
    }

  }

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    eval =
      Evals.pool(conduit)
        .get(NAME);

    captures =
      CaptureBuffer.of(circuit, conduit);

  }

  @SpecRef({"4.5", "registry:evals"})
  @Test
  void signsAreStable() {

    assertArrayEquals(
      new Sign[]{
        PASS,
        FAIL,
        UNKNOWN,
        SKIP,
        ERROR
      },
      Sign.values()
    );

  }

  @AfterEach
  void teardown() {

    captures.close();
    circuit
      .closeAwait();

  }

  /// The criterion set is closed so that an observer can weight criteria against each other, and
  /// [Evals#DIMENSIONS] is published for exactly that. This pins the documented path — crossing
  /// `SIGNS` with `DIMENSIONS` into a signal-keyed map — and proves it discriminates by criterion
  /// where the sign-keyed [Evals#STATUS] cannot, end to end from a real emission.

  @Test
  void weightsCriteriaBySignal() {

    final var weighted =
      Evals.SIGNS
        .signals(Evals.DIMENSIONS, Signal::new)
        .map(
          signal -> switch (signal.sign()) {
            case PASS -> Statuses.Sign.STABLE;
            case FAIL -> switch (signal.dimension()) {
              case SAFETY, CORRECTNESS -> Statuses.Sign.DEFECTIVE;
              default -> Statuses.Sign.DEGRADED;
            };
            case UNKNOWN, SKIP, ERROR -> null;
          }
        );

    // the canonical map is sign-keyed: every criterion reads the same
    assertEquals(Statuses.Sign.DEGRADED, Evals.STATUS.apply(FAIL));

    // the weighted map separates them
    assertEquals(Statuses.Sign.DEFECTIVE, weighted.get(FAIL, SAFETY));
    assertEquals(Statuses.Sign.DEGRADED, weighted.get(FAIL, COMPLETENESS));

    // and agrees with the canonical reading wherever no weighting applies
    assertEquals(Statuses.Sign.STABLE, weighted.get(PASS, SAFETY));
    assertNull(weighted.get(ERROR, SAFETY));

    // end to end: the map reads an emitted signal, not just a constructed one
    eval.fail(SAFETY);

    assertEquals(
      Statuses.Sign.DEFECTIVE,
      weighted.apply(
        captures
          .drainEmissions()
          .findFirst()
          .orElseThrow()
      )
    );

  }

}
