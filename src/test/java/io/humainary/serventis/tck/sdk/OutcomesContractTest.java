// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Outcomes.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Outcomes.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Outcomes] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.15.1/SPEC.md")
final class OutcomesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("payment.outcomes");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Outcome outcome;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    outcome =
      Outcomes.pool(conduit)
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
  void testFail() {

    outcome.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FAIL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMixedOutcomes() {

    outcome.success();
    outcome.success();
    outcome.fail();
    outcome.success();
    outcome.fail();
    outcome.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(SUCCESS, signs.get(0));
    assertEquals(SUCCESS, signs.get(1));
    assertEquals(FAIL, signs.get(2));
    assertEquals(SUCCESS, signs.get(3));
    assertEquals(FAIL, signs.get(4));
    assertEquals(FAIL, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleOutcomeInstruments() {

    final var paymentName = CORTEX.name("payment.outcomes");
    final var orderName = CORTEX.name("order.outcomes");

    final var conduit = circuit.conduit(Sign.class);
    final var outcomes = Outcomes.pool(conduit);
    final var paymentOutcome = outcomes.get(paymentName);
    final var orderOutcome = outcomes.get(orderName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    paymentOutcome.success();
    orderOutcome.fail();
    paymentOutcome.fail();

    final var allCaptures = signCaptures.drain().toList();
    signCaptures.close();

    assertEquals(3, allCaptures.size());

    assertEquals(paymentName, allCaptures.get(0).subject().name());
    assertEquals(SUCCESS, allCaptures.get(0).emission());

    assertEquals(orderName, allCaptures.get(1).subject().name());
    assertEquals(FAIL, allCaptures.get(1).emission());

    assertEquals(paymentName, allCaptures.get(2).subject().name());
    assertEquals(FAIL, allCaptures.get(2).emission());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    outcome.sign(SUCCESS);
    outcome.sign(FAIL);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(SUCCESS, signs.get(0));
    assertEquals(FAIL, signs.get(1));

  }

  @SpecRef({"4.1", "7.4"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, SUCCESS.ordinal());
    assertEquals(1, FAIL.ordinal());
    assertEquals(2, UNKNOWN.ordinal());

  }

  @SpecRef({"4.5", "7.4"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(3, values.length);
    assertEquals(SUCCESS, values[0]);
    assertEquals(FAIL, values[1]);
    assertEquals(UNKNOWN, values[2]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    outcome.success();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(SUCCESS, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccess() {

    outcome.success();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SUCCESS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccessFailPattern() {

    outcome.success();
    outcome.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(SUCCESS, signs.getFirst());
    assertEquals(FAIL, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testUnknown() {

    outcome.unknown();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(UNKNOWN, signs.getFirst());

  }

  /// `UNKNOWN` is a member of the published outcome sign set, and is distinguishable from the
  /// absent value: an indeterminate verdict is a reading, not an abstention.
  ///
  /// Membership is read through `Outcomes.SIGNS` rather than the enum's own `values()`, which
  /// would pass whatever the published set held. A map over the published set returns, at
  /// `UNKNOWN`'s index, the member the set captured there.
  @SpecRef("10.4")
  @Test
  void unknownIsAMemberAndDistinctFromAbsent() {

    final var members =
      Outcomes.SIGNS.map(sign -> sign);

    assertSame(UNKNOWN, members.apply(UNKNOWN), "UNKNOWN is in the published outcome sign set");

    outcome.unknown();

    assertEquals(
      java.util.List.of(UNKNOWN),
      captures.drainEmissions().toList(),
      "UNKNOWN is emitted as a present value, not as the absent one"
    );

  }

  /// A consumer reading a translation produces nothing where the translation abstains: a flow
  /// that maps signs through a translation emits for the signs it reads and is silent for the
  /// rest.
  ///
  /// The translation is built here, over the published `Outcomes.SIGNS`, so the test needs none of
  /// the Appendix B property maps and runs on a projection that provides no extensions (§10.4).
  @SpecRef("10.4")
  @Test
  void consumerOfATranslationIsSilentWhereItAbstains() {

    final SignMap< Sign, Sign > decided =
      Outcomes.SIGNS.map(sign -> sign==UNKNOWN ? null : sign);

    final var conduit =
      circuit.conduit(Sign.class);

    final var verdicts =
      CaptureBuffer.of(circuit, conduit);

    final var in =
      conduit.pool(
        CORTEX.flow(Sign.class)
          .map(decided::apply)
      ).get(CORTEX.name("decided.verdicts"));

    in.emit(SUCCESS);   // reads SUCCESS
    in.emit(UNKNOWN);   // abstains
    in.emit(FAIL);      // reads FAIL
    in.emit(UNKNOWN);   // abstains

    assertEquals(
      java.util.List.of(SUCCESS, FAIL),
      verdicts.drainEmissions().toList()
    );

  }

}
