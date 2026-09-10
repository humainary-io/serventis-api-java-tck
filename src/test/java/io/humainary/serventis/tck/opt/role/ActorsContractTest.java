// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.role;

import io.humainary.serventis.opt.role.*;
import io.humainary.serventis.opt.role.Actors.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.role.Actors.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Actors] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class ActorsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("user.william");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Actor actor;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    actor =
      conduit.pool(Actors::of)
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

  // ========== INDIVIDUAL SIGN TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAcknowledge() {

    actor.acknowledge();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ACKNOWLEDGE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAffirm() {

    actor.affirm();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(AFFIRM, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAsk() {

    actor.ask();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ASK, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testClarify() {

    actor.clarify();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CLARIFY, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCollaborativeRefinementPattern() {

    // Simulate collaborative refinement
    final var aiName = CORTEX.name("assistant.claude");

    final var conduit = circuit.conduit(Sign.class);
    final var actors = conduit.pool(Actors::of);
    final var human = actors.get(NAME);
    final var ai = actors.get(aiName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    // Human requests design
    human.request();

    // AI explains approach
    ai.explain();

    // Human denies and clarifies different direction
    human.deny();
    human.clarify();

    // AI acknowledges and delivers refined design
    ai.acknowledge();
    ai.deliver();

    // Human acknowledges
    human.acknowledge();

    final var signs =
      signCaptures
        .drainEmissions()
        .toList();
    signCaptures.close();

    assertEquals(7, signs.size());
    assertEquals(REQUEST, signs.get(0));
    assertEquals(EXPLAIN, signs.get(1));
    assertEquals(DENY, signs.get(2));
    assertEquals(CLARIFY, signs.get(3));
    assertEquals(ACKNOWLEDGE, signs.get(4));
    assertEquals(DELIVER, signs.get(5));
    assertEquals(ACKNOWLEDGE, signs.get(6));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCommand() {

    actor.command();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(COMMAND, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCommandAuthority() {

    // Test command-based interaction
    actor.command();
    actor.acknowledge();
    actor.deliver();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(COMMAND, signs.get(0));
    assertEquals(ACKNOWLEDGE, signs.get(1));
    assertEquals(DELIVER, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCommitmentFulfillment() {

    // Test PROMISE → DELIVER arc
    actor.promise();
    actor.deliver();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(PROMISE, signs.get(0));
    assertEquals(DELIVER, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testComplexHumanAIDialogue() {

    // Simulate the full example from Javadoc
    final var aiName = CORTEX.name("assistant.claude");

    final var conduit = circuit.conduit(Sign.class);
    final var actors = conduit.pool(Actors::of);
    final var human = actors.get(NAME);
    final var ai = actors.get(aiName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    // Phase 1: Question-Answer
    human.ask();           // "What about packet networks?"
    ai.explain();          // network semantic spaces
    ai.affirm();           // Routers API would be valuable
    human.acknowledge();

    // Phase 2: Request-Delivery
    human.request();       // write Routers API
    ai.acknowledge();
    ai.promise();          // will deliver
    ai.deliver();          // presents Routers.java
    human.acknowledge();

    // Phase 3: Correction
    human.deny();          // Router not right term
    human.ask();           // is Actor better?
    ai.explain();          // terminology analysis
    ai.affirm();           // Actor is preferable
    human.acknowledge();

    // Phase 4: Command-Revision
    human.command();       // rewrite with Actor
    ai.acknowledge();
    ai.clarify();          // same 11 signs?
    human.acknowledge();
    ai.deliver();          // revised Actors API

    final var signs =
      signCaptures
        .drainEmissions()
        .toList();
    signCaptures.close();

    assertEquals(19, signs.size());

    // Verify key transition points
    assertEquals(ASK, signs.get(0));      // Start Phase 1
    assertEquals(REQUEST, signs.get(4));  // Start Phase 2
    assertEquals(DENY, signs.get(9));     // Start Phase 3
    assertEquals(COMMAND, signs.get(14)); // Start Phase 4
    assertEquals(DELIVER, signs.get(18)); // Final delivery

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCoordinationFlow() {

    // Test coordination sequence: REQUEST → ACKNOWLEDGE → PROMISE → DELIVER
    actor.request();
    actor.acknowledge();
    actor.promise();
    actor.deliver();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(REQUEST, signs.get(0));
    assertEquals(ACKNOWLEDGE, signs.get(1));
    assertEquals(PROMISE, signs.get(2));
    assertEquals(DELIVER, signs.get(3));

  }

  // ========== GENERIC TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCorrectionClarificationPattern() {

    // Simulate correction and clarification
    final var aiName = CORTEX.name("assistant.claude");

    final var conduit = circuit.conduit(Sign.class);
    final var actors = conduit.pool(Actors::of);
    final var human = actors.get(NAME);
    final var ai = actors.get(aiName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    // AI makes assertion
    ai.affirm();

    // Human denies
    human.deny();

    // AI clarifies
    ai.clarify();

    // Human acknowledges
    human.acknowledge();

    final var signs =
      signCaptures
        .drainEmissions()
        .toList();
    signCaptures.close();

    assertEquals(4, signs.size());
    assertEquals(AFFIRM, signs.get(0));
    assertEquals(DENY, signs.get(1));
    assertEquals(CLARIFY, signs.get(2));
    assertEquals(ACKNOWLEDGE, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeliver() {

    actor.deliver();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DELIVER, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDeny() {

    actor.deny();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DENY, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDisagreementResolution() {

    // Test disagreement flow: AFFIRM → DENY → CLARIFY → ACKNOWLEDGE
    actor.affirm();
    actor.deny();
    actor.clarify();
    actor.acknowledge();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(AFFIRM, signs.get(0));
    assertEquals(DENY, signs.get(1));
    assertEquals(CLARIFY, signs.get(2));
    assertEquals(ACKNOWLEDGE, signs.get(3));

  }

  // ========== DIALOGUE PATTERN TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExplain() {

    actor.explain();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EXPLAIN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testInformationExchange() {

    // Test information flow: ASK → REPORT/EXPLAIN/AFFIRM
    actor.ask();
    actor.report();
    actor.explain();
    actor.affirm();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(ASK, signs.get(0));
    assertEquals(REPORT, signs.get(1));
    assertEquals(EXPLAIN, signs.get(2));
    assertEquals(AFFIRM, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPromise() {

    actor.promise();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PROMISE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testQuestionAnswerPattern() {

    // Simulate question-answer dialogue
    final var aiName = CORTEX.name("assistant.claude");

    final var conduit = circuit.conduit(Sign.class);
    final var actors = conduit.pool(Actors::of);
    final var human = actors.get(NAME);
    final var ai = actors.get(aiName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    // Human asks question
    human.ask();

    // AI explains and affirms
    ai.explain();
    ai.affirm();

    // Human acknowledges
    human.acknowledge();

    final var allSigns = signCaptures.drain().toList();
    signCaptures.close();

    assertEquals(4, allSigns.size());

    // Verify sequence
    assertEquals(ASK, allSigns.get(0).emission());
    assertEquals(EXPLAIN, allSigns.get(1).emission());
    assertEquals(AFFIRM, allSigns.get(2).emission());
    assertEquals(ACKNOWLEDGE, allSigns.get(3).emission());

    // Verify actors
    assertEquals(NAME, allSigns.get(0).subject().name());
    assertEquals(aiName, allSigns.get(1).subject().name());
    assertEquals(aiName, allSigns.get(2).subject().name());
    assertEquals(NAME, allSigns.get(3).subject().name());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReport() {

    actor.report();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REPORT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRequest() {

    actor.request();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REQUEST, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRequestDeliveryPattern() {

    // Simulate request-delivery collaboration
    final var aiName = CORTEX.name("assistant.claude");

    final var conduit = circuit.conduit(Sign.class);
    final var actors = conduit.pool(Actors::of);
    final var human = actors.get(NAME);
    final var ai = actors.get(aiName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    // Human requests work
    human.request();

    // AI acknowledges and promises
    ai.acknowledge();
    ai.promise();

    // AI delivers work
    ai.deliver();

    // Human acknowledges
    human.acknowledge();

    final var signs =
      signCaptures
        .drainEmissions()
        .toList();
    signCaptures.close();

    assertEquals(5, signs.size());
    assertEquals(REQUEST, signs.get(0));
    assertEquals(ACKNOWLEDGE, signs.get(1));
    assertEquals(PROMISE, signs.get(2));
    assertEquals(DELIVER, signs.get(3));
    assertEquals(ACKNOWLEDGE, signs.get(4));

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    actor.sign(ASK);
    actor.sign(AFFIRM);
    actor.sign(EXPLAIN);
    actor.sign(REPORT);
    actor.sign(REQUEST);
    actor.sign(COMMAND);
    actor.sign(ACKNOWLEDGE);
    actor.sign(DENY);
    actor.sign(CLARIFY);
    actor.sign(PROMISE);
    actor.sign(DELIVER);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(11, signs.size());
    assertEquals(ASK, signs.get(0));
    assertEquals(AFFIRM, signs.get(1));
    assertEquals(EXPLAIN, signs.get(2));
    assertEquals(REPORT, signs.get(3));
    assertEquals(REQUEST, signs.get(4));
    assertEquals(COMMAND, signs.get(5));
    assertEquals(ACKNOWLEDGE, signs.get(6));
    assertEquals(DENY, signs.get(7));
    assertEquals(CLARIFY, signs.get(8));
    assertEquals(PROMISE, signs.get(9));
    assertEquals(DELIVER, signs.get(10));

  }

  @SpecRef({"4.1", "registry:actors"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, ASK.ordinal());
    assertEquals(1, AFFIRM.ordinal());
    assertEquals(2, EXPLAIN.ordinal());
    assertEquals(3, REPORT.ordinal());
    assertEquals(4, REQUEST.ordinal());
    assertEquals(5, COMMAND.ordinal());
    assertEquals(6, ACKNOWLEDGE.ordinal());
    assertEquals(7, DENY.ordinal());
    assertEquals(8, CLARIFY.ordinal());
    assertEquals(9, PROMISE.ordinal());
    assertEquals(10, DELIVER.ordinal());

  }

  @SpecRef({"4.5", "registry:actors"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(11, values.length);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    actor.ask();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(ASK, capture.emission());

  }

}
