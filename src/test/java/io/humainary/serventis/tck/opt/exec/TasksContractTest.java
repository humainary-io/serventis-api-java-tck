// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Tasks.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.exec.Tasks.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Tasks] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class TasksContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("worker.task");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Task task;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    task =
      conduit.pool(Tasks::of)
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
  void testCancel() {

    task.cancel();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CANCEL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCancelledTask() {

    task.submit();
    task.schedule();
    task.cancel();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(CANCEL, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testComplete() {

    task.complete();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(COMPLETE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFail() {

    task.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FAIL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailedTaskLifecycle() {

    task.submit();
    task.schedule();
    task.start();
    task.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(START, signs.get(2));
    assertEquals(FAIL, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testProgress() {

    task.progress();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(PROGRESS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReject() {

    task.reject();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REJECT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRejectedTask() {

    task.submit();
    task.reject();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(REJECT, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testResume() {

    task.resume();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RESUME, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSchedule() {

    task.schedule();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SCHEDULE, signs.getFirst());

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:tasks"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, SUBMIT.ordinal());
    assertEquals(1, REJECT.ordinal());
    assertEquals(2, SCHEDULE.ordinal());
    assertEquals(3, START.ordinal());
    assertEquals(4, PROGRESS.ordinal());
    assertEquals(5, SUSPEND.ordinal());
    assertEquals(6, RESUME.ordinal());
    assertEquals(7, COMPLETE.ordinal());
    assertEquals(8, FAIL.ordinal());
    assertEquals(9, CANCEL.ordinal());
    assertEquals(10, TIMEOUT.ordinal());

  }

  @SpecRef("6.1")
  @Test
  void testSignMethod() {

    // Test direct sign() method
    task.sign(SUBMIT);
    task.sign(COMPLETE);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(COMPLETE, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStart() {

    task.start();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(START, signs.getFirst());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    task.submit();

    assertEquals(
      NAME,
      captures
        .drain()
        .map(Capture::subject)
        .map(Subject::name)
        .findFirst()
        .orElseThrow()
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSubmit() {

    task.submit();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SUBMIT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccessfulTaskLifecycle() {

    task.submit();
    task.schedule();
    task.start();
    task.complete();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(START, signs.get(2));
    assertEquals(COMPLETE, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspend() {

    task.suspend();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SUSPEND, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspendedResumedTask() {

    task.submit();
    task.schedule();
    task.start();
    task.suspend();
    task.resume();
    task.complete();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(START, signs.get(2));
    assertEquals(SUSPEND, signs.get(3));
    assertEquals(RESUME, signs.get(4));
    assertEquals(COMPLETE, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTaskWithProgress() {

    task.submit();
    task.schedule();
    task.start();
    task.progress();
    task.progress();
    task.complete();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(START, signs.get(2));
    assertEquals(PROGRESS, signs.get(3));
    assertEquals(PROGRESS, signs.get(4));
    assertEquals(COMPLETE, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTimedOutTask() {

    task.submit();
    task.schedule();
    task.start();
    task.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(SUBMIT, signs.get(0));
    assertEquals(SCHEDULE, signs.get(1));
    assertEquals(START, signs.get(2));
    assertEquals(TIMEOUT, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testTimeout() {

    task.timeout();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(TIMEOUT, signs.getFirst());

  }

}
