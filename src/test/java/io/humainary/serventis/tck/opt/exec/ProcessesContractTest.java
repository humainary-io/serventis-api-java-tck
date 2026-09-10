// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Processes.*;
import io.humainary.serventis.opt.exec.Processes.Process;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.exec.Processes.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Processes] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class ProcessesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("worker.process");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Process process;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    process =
      conduit.pool(Processes::of)
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
  void testCleanProcess() {

    process.spawn();
    process.start();
    process.stop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(STOP, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCrash() {

    process.crash();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(CRASH, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCrashedProcess() {

    process.spawn();
    process.start();
    process.crash();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(CRASH, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCrashedProcessWithRestart() {

    process.spawn();
    process.start();
    process.crash();
    process.restart();
    process.start();
    process.stop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(CRASH, signs.get(2));
    assertEquals(RESTART, signs.get(3));
    assertEquals(START, signs.get(4));
    assertEquals(STOP, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFail() {

    process.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(FAIL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailedProcess() {

    process.spawn();
    process.start();
    process.fail();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(FAIL, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailedProcessWithRestart() {

    process.spawn();
    process.start();
    process.fail();
    process.restart();
    process.start();
    process.stop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(FAIL, signs.get(2));
    assertEquals(RESTART, signs.get(3));
    assertEquals(START, signs.get(4));
    assertEquals(STOP, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testKill() {

    process.kill();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(KILL, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testKilledProcess() {

    process.start();
    process.kill();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(START, signs.get(0));
    assertEquals(KILL, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRestart() {

    process.restart();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RESTART, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testResume() {

    process.resume();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(RESUME, signs.getFirst());

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:processes"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, SPAWN.ordinal());
    assertEquals(1, START.ordinal());
    assertEquals(2, STOP.ordinal());
    assertEquals(3, FAIL.ordinal());
    assertEquals(4, CRASH.ordinal());
    assertEquals(5, KILL.ordinal());
    assertEquals(6, RESTART.ordinal());
    assertEquals(7, SUSPEND.ordinal());
    assertEquals(8, RESUME.ordinal());

  }

  @SpecRef("6.1")
  @Test
  void testSignMethod() {

    // Test direct sign() method
    process.sign(SPAWN);
    process.sign(START);
    process.sign(STOP);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(SPAWN, signs.get(0));
    assertEquals(START, signs.get(1));
    assertEquals(STOP, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSpawn() {

    process.spawn();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SPAWN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStart() {

    process.start();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(START, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStop() {

    process.stop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(STOP, signs.getFirst());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    process.spawn();

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
  void testSuspend() {

    process.suspend();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SUSPEND, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspendedResumedProcess() {

    process.start();
    process.suspend();
    process.resume();
    process.stop();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(START, signs.get(0));
    assertEquals(SUSPEND, signs.get(1));
    assertEquals(RESUME, signs.get(2));
    assertEquals(STOP, signs.get(3));

  }

}
