// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Transactions.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.exec.Transactions.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Transactions] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class TransactionsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("db.transaction");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Transaction transaction;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Signal.class
      );

    transaction =
      conduit.pool(Transactions::of)
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

  // ========== COORDINATOR SIGNAL TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAbortCoordinator() {

    transaction.abort(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.ABORT, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testAbortParticipant() {

    transaction.abort(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.ABORT, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCommitCoordinator() {

    transaction.commit(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.COMMIT, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCommitParticipant() {

    transaction.commit(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.COMMIT, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCompensateCoordinator() {

    transaction.compensate(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.COMPENSATE, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCompensateParticipant() {

    transaction.compensate(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.COMPENSATE, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testConflictCoordinator() {

    transaction.conflict(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.CONFLICT, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testConflictParticipant() {

    transaction.conflict(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.CONFLICT, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"4.5", "registry:transactions"})
  @Test
  void testDimensionEnumValues() {

    final var dimensions = Dimension.values();

    assertEquals(2, dimensions.length);
    assertEquals(COORDINATOR, dimensions[0]);
    assertEquals(PARTICIPANT, dimensions[1]);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDistributedTransactionMultipleParticipants() {

    // Coordinator perspective with multiple participants
    final var coordinatorName = CORTEX.name("coordinator");
    final var participant1Name = CORTEX.name("participant.1");
    final var participant2Name = CORTEX.name("participant.2");

    final var conduit = circuit.conduit(Signal.class);
    final var transactions = conduit.pool(Transactions::of);
    final var coordinator = transactions.get(coordinatorName);
    final var participant1 = transactions.get(participant1Name);
    final var participant2 = transactions.get(participant2Name);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    // Coordinator starts transaction
    coordinator.start(COORDINATOR);
    participant1.start(PARTICIPANT);
    participant2.start(PARTICIPANT);

    // Coordinator sends prepare
    coordinator.prepare(COORDINATOR);
    participant1.prepare(PARTICIPANT);    // P1 votes yes
    participant2.prepare(PARTICIPANT);    // P2 votes yes

    // Coordinator commits
    coordinator.commit(COORDINATOR);
    participant1.commit(PARTICIPANT);
    participant2.commit(PARTICIPANT);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(9, allSignals.size());

    // Verify coordinator signals
    final var coordinatorSignals =
      allSignals
        .stream()
        .filter(c -> c.subject().name().equals(coordinatorName))
        .map(Capture::emission)
        .toList();

    assertEquals(3, coordinatorSignals.size());
    assertEquals(Sign.START, coordinatorSignals.get(0).sign());
    assertEquals(COORDINATOR, coordinatorSignals.get(0).dimension());
    assertEquals(Sign.PREPARE, coordinatorSignals.get(1).sign());
    assertEquals(Sign.COMMIT, coordinatorSignals.get(2).sign());

    // Verify participant 1 signals
    final var p1Signals =
      allSignals
        .stream()
        .filter(c -> c.subject().name().equals(participant1Name))
        .map(Capture::emission)
        .toList();

    assertEquals(3, p1Signals.size());
    assertEquals(Sign.START, p1Signals.get(0).sign());
    assertEquals(PARTICIPANT, p1Signals.get(0).dimension());
    assertEquals(Sign.PREPARE, p1Signals.get(1).sign());
    assertEquals(Sign.COMMIT, p1Signals.get(2).sign());

    // Verify participant 2 signals
    final var p2Signals =
      allSignals
        .stream()
        .filter(c -> c.subject().name().equals(participant2Name))
        .map(Capture::emission)
        .toList();

    assertEquals(3, p2Signals.size());
    assertEquals(Sign.START, p2Signals.get(0).sign());
    assertEquals(PARTICIPANT, p2Signals.get(0).dimension());
    assertEquals(Sign.PREPARE, p2Signals.get(1).sign());
    assertEquals(Sign.COMMIT, p2Signals.get(2).sign());

  }

  // ========== ENUM TESTS ==========

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDistributedTransactionPartialFailure() {

    // Coordinator with one participant voting no
    final var coordinatorName = CORTEX.name("coordinator");
    final var participant1Name = CORTEX.name("participant.1");
    final var participant2Name = CORTEX.name("participant.2");

    final var conduit = circuit.conduit(Signal.class);
    final var transactions = conduit.pool(Transactions::of);
    final var coordinator = transactions.get(coordinatorName);
    final var participant1 = transactions.get(participant1Name);
    final var participant2 = transactions.get(participant2Name);
    final var signalCaptures = CaptureBuffer.of(circuit, conduit);

    // Coordinator starts transaction
    coordinator.start(COORDINATOR);
    participant1.start(PARTICIPANT);
    participant2.start(PARTICIPANT);

    // Coordinator sends prepare
    coordinator.prepare(COORDINATOR);
    participant1.prepare(PARTICIPANT);      // P1 votes yes
    participant2.conflict(PARTICIPANT);     // P2 has conflict, votes no

    // Coordinator rolls back
    coordinator.rollback(COORDINATOR);
    participant1.rollback(PARTICIPANT);
    participant2.rollback(PARTICIPANT);

    final var allSignals = signalCaptures.drain().toList();
    signalCaptures.close();

    assertEquals(9, allSignals.size());

    // Verify rollback was issued
    final var coordinatorSignals =
      allSignals
        .stream()
        .filter(c -> c.subject().name().equals(coordinatorName))
        .map(Capture::emission)
        .toList();

    assertEquals(3, coordinatorSignals.size());
    assertEquals(Sign.ROLLBACK, coordinatorSignals.get(2).sign());

    // Verify P2 conflicted
    final var p2Signals =
      allSignals
        .stream()
        .filter(c -> c.subject().name().equals(participant2Name))
        .map(Capture::emission)
        .toList();

    assertEquals(Sign.CONFLICT, p2Signals.get(1).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpireCoordinator() {

    transaction.expire(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.EXPIRE, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpireParticipant() {

    transaction.expire(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.EXPIRE, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleTransactionOperations() {

    // Multiple operations in sequence
    transaction.start(COORDINATOR);
    transaction.prepare(COORDINATOR);
    transaction.commit(COORDINATOR);
    transaction.start(COORDINATOR);
    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(Sign.PREPARE, signals.get(1).sign());
    assertEquals(Sign.COMMIT, signals.get(2).sign());
    assertEquals(Sign.START, signals.get(3).sign());
    assertEquals(Sign.ROLLBACK, signals.get(4).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPrepareCoordinator() {

    transaction.prepare(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.PREPARE, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testPrepareParticipant() {

    transaction.prepare(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.PREPARE, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRollbackCoordinator() {

    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.ROLLBACK, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRollbackParticipant() {

    transaction.rollback(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.ROLLBACK, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSagaPattern() {

    // Saga with compensation
    transaction.start(COORDINATOR);
    transaction.compensate(COORDINATOR);
    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(Sign.COMPENSATE, signals.get(1).sign());
    assertEquals(Sign.ROLLBACK, signals.get(2).sign());

  }

  @SpecRef({"4.5", "registry:transactions"})
  @Test
  void testSignEnumValues() {

    final var signs = Sign.values();

    assertEquals(8, signs.length);
    assertEquals(Sign.START, signs[0]);
    assertEquals(Sign.PREPARE, signs[1]);
    assertEquals(Sign.COMMIT, signs[2]);
    assertEquals(Sign.ROLLBACK, signs[3]);
    assertEquals(Sign.ABORT, signs[4]);
    assertEquals(Sign.EXPIRE, signs[5]);
    assertEquals(Sign.CONFLICT, signs[6]);
    assertEquals(Sign.COMPENSATE, signs[7]);

  }

  // ========== TRANSACTION LIFECYCLE PATTERN TESTS ==========

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    transaction.signal(Sign.START, COORDINATOR);
    transaction.signal(Sign.START, PARTICIPANT);
    transaction.signal(Sign.PREPARE, COORDINATOR);
    transaction.signal(Sign.PREPARE, PARTICIPANT);
    transaction.signal(Sign.COMMIT, COORDINATOR);
    transaction.signal(Sign.COMMIT, PARTICIPANT);
    transaction.signal(Sign.ROLLBACK, COORDINATOR);
    transaction.signal(Sign.ROLLBACK, PARTICIPANT);
    transaction.signal(Sign.ABORT, COORDINATOR);
    transaction.signal(Sign.ABORT, PARTICIPANT);
    transaction.signal(Sign.EXPIRE, COORDINATOR);
    transaction.signal(Sign.EXPIRE, PARTICIPANT);
    transaction.signal(Sign.CONFLICT, COORDINATOR);
    transaction.signal(Sign.CONFLICT, PARTICIPANT);
    transaction.signal(Sign.COMPENSATE, COORDINATOR);
    transaction.signal(Sign.COMPENSATE, PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(16, signals.size());
    assertEquals(new Signal(Sign.START, COORDINATOR), signals.get(0));
    assertEquals(new Signal(Sign.START, PARTICIPANT), signals.get(1));
    assertEquals(new Signal(Sign.PREPARE, COORDINATOR), signals.get(2));
    assertEquals(new Signal(Sign.PREPARE, PARTICIPANT), signals.get(3));
    assertEquals(new Signal(Sign.COMMIT, COORDINATOR), signals.get(4));
    assertEquals(new Signal(Sign.COMMIT, PARTICIPANT), signals.get(5));
    assertEquals(new Signal(Sign.ROLLBACK, COORDINATOR), signals.get(6));
    assertEquals(new Signal(Sign.ROLLBACK, PARTICIPANT), signals.get(7));
    assertEquals(new Signal(Sign.ABORT, COORDINATOR), signals.get(8));
    assertEquals(new Signal(Sign.ABORT, PARTICIPANT), signals.get(9));
    assertEquals(new Signal(Sign.EXPIRE, COORDINATOR), signals.get(10));
    assertEquals(new Signal(Sign.EXPIRE, PARTICIPANT), signals.get(11));
    assertEquals(new Signal(Sign.CONFLICT, COORDINATOR), signals.get(12));
    assertEquals(new Signal(Sign.CONFLICT, PARTICIPANT), signals.get(13));
    assertEquals(new Signal(Sign.COMPENSATE, COORDINATOR), signals.get(14));
    assertEquals(new Signal(Sign.COMPENSATE, PARTICIPANT), signals.get(15));

  }

  @SpecRef("4.4")
  @Test
  void testSignalRecord() {

    // Test that Signal is a proper record with sign and dimension
    final var signal = new Signal(Sign.COMMIT, COORDINATOR);

    assertEquals(Sign.COMMIT, signal.sign());
    assertEquals(COORDINATOR, signal.dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStartCoordinator() {

    transaction.start(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.START, signals.getFirst().sign());
    assertEquals(COORDINATOR, signals.getFirst().dimension());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStartParticipant() {

    transaction.start(PARTICIPANT);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signals.size());
    assertEquals(Sign.START, signals.getFirst().sign());
    assertEquals(PARTICIPANT, signals.getFirst().dimension());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    transaction.start(COORDINATOR);

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(Sign.START, capture.emission().sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransactionAbort() {

    // Explicit abort (e.g., deadlock detected)
    transaction.start(COORDINATOR);
    transaction.prepare(COORDINATOR);
    transaction.abort(COORDINATOR);
    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(Sign.PREPARE, signals.get(1).sign());
    assertEquals(Sign.ABORT, signals.get(2).sign());
    assertEquals(Sign.ROLLBACK, signals.get(3).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTransactionExpire() {

    // Transaction with expiration
    transaction.start(COORDINATOR);
    transaction.prepare(COORDINATOR);
    transaction.expire(COORDINATOR);
    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(Sign.PREPARE, signals.get(1).sign());
    assertEquals(Sign.EXPIRE, signals.get(2).sign());
    assertEquals(Sign.ROLLBACK, signals.get(3).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTwoPhaseCommitAbort() {

    // 2PC abort flow (participant votes no via conflict)
    transaction.start(COORDINATOR);
    transaction.prepare(COORDINATOR);
    transaction.conflict(PARTICIPANT);
    transaction.rollback(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(Sign.PREPARE, signals.get(1).sign());
    assertEquals(Sign.CONFLICT, signals.get(2).sign());
    assertEquals(PARTICIPANT, signals.get(2).dimension());
    assertEquals(Sign.ROLLBACK, signals.get(3).sign());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTwoPhaseCommitSuccess() {

    // Standard 2PC success flow from coordinator perspective
    transaction.start(COORDINATOR);
    transaction.prepare(COORDINATOR);
    transaction.commit(COORDINATOR);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signals.size());
    assertEquals(Sign.START, signals.get(0).sign());
    assertEquals(COORDINATOR, signals.get(0).dimension());
    assertEquals(Sign.PREPARE, signals.get(1).sign());
    assertEquals(COORDINATOR, signals.get(1).dimension());
    assertEquals(Sign.COMMIT, signals.get(2).sign());
    assertEquals(COORDINATOR, signals.get(2).dimension());

  }

}
