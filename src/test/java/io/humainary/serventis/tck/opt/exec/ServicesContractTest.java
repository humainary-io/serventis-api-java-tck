// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Services.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.exec.Services.Dimension.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// The test class for the [Service] interface.
/// @author William David Louth
/// @since 1.0
@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
@SuppressWarnings("DataFlowIssue")  // deliberate null-rejection coverage
final class ServicesContractTest {

  private static final Cortex cortex = cortex();
  private static final Name NAME = cortex.name("service.1");
  private Circuit circuit;
  private CaptureBuffer< Signal > captures;
  private Service service;

  private void assertSignal(
    final Signal signal
  ) {

    assertEquals(
      1L, captures
        .drain()
        .filter(capture -> capture.emission().equals(signal))
        .filter(capture -> capture.subject().name()==NAME)
        .count()
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

    service =
      conduit.pool(Services::of)
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

  // Individual tests for each convenience method

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCall() {

    service.call(CALLER);

    assertSignal(
      new Signal(Sign.CALL, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCalled() {

    service.call(CALLEE);

    assertSignal(
      new Signal(Sign.CALL, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDelay() {

    service.delay(CALLER);

    assertSignal(
      new Signal(Sign.DELAY, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDelayed() {

    service.delay(CALLEE);

    assertSignal(
      new Signal(Sign.DELAY, CALLEE)
    );

  }

  /// Tests that [Dimension] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Dimension] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:services"})
  @Test
  void testDimensionEnumOrdinals() {

    assertEquals(0, Dimension.CALLER.ordinal());
    assertEquals(1, Dimension.CALLEE.ordinal());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDiscard() {

    service.discard(CALLER);

    assertSignal(
      new Signal(Sign.DISCARD, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDiscarded() {

    service.discard(CALLEE);

    assertSignal(
      new Signal(Sign.DISCARD, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDisconnect() {

    service.disconnect(CALLER);

    assertSignal(
      new Signal(Sign.DISCONNECT, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDisconnected() {

    service.disconnect(CALLEE);

    assertSignal(
      new Signal(Sign.DISCONNECT, CALLEE)
    );

  }

  @Test
  void testDispatchFunctionNull() {

    assertThrows(
      NullPointerException.class,
      () -> service.dispatch(CALLER, (Fn< String, java.lang.Exception >) null)
    );

  }

  @Test
  void testDispatchOperationNull() {

    assertThrows(
      NullPointerException.class,
      () -> service.dispatch(CALLER, (Op< java.lang.Exception >) null)
    );

  }

  @Test
  void testDispatchWithFunctionFailure() {

    assertThrows(
      RuntimeException.class,
      () -> service.dispatch(
        CALLER,
        () -> {
          throw new RuntimeException("test");
        }
      )
    );

    final var emitted = captures.drain().toList();

    assertEquals(2, emitted.size());
    assertEquals(new Signal(Sign.CALL, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.FAIL, CALLER), emitted.get(1).emission());

  }

  @Test
  void testDispatchWithFunctionSuccess() {

    final var result = service.dispatch(CALLER, () -> "dispatched");

    assertEquals("dispatched", result);

    final var emitted = captures.drain().toList();

    assertEquals(2, emitted.size());
    assertEquals(new Signal(Sign.CALL, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.SUCCESS, CALLER), emitted.get(1).emission());

  }

  @Test
  void testDispatchWithOperationFailure() {

    assertThrows(
      RuntimeException.class,
      () -> service.dispatch(
        CALLER,
        () -> {
          throw new RuntimeException("test");
        }
      )
    );

    final var emitted = captures.drain().toList();

    assertEquals(2, emitted.size());
    assertEquals(new Signal(Sign.CALL, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.FAIL, CALLER), emitted.get(1).emission());

  }

  @Test
  void testDispatchWithOperationSuccess() {

    service.dispatch(CALLER, () -> {
    });

    final var emitted = captures.drain().toList();

    assertEquals(2, emitted.size());
    assertEquals(new Signal(Sign.CALL, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.SUCCESS, CALLER), emitted.get(1).emission());

  }

  @Test
  void testExecuteFunctionNull() {

    assertThrows(
      NullPointerException.class,
      () -> service.execute(CALLER, (Fn< String, java.lang.Exception >) null)
    );

  }

  @Test
  void testExecuteOperationNull() {

    assertThrows(
      NullPointerException.class,
      () -> service.execute(CALLER, (Op< java.lang.Exception >) null)
    );

  }

  @Test
  void testExecuteWithFunctionFailure() {

    assertThrows(
      RuntimeException.class,
      () -> service.execute(
        CALLER,
        () -> {
          throw new RuntimeException("test");
        }
      )
    );

    final var emitted = captures.drain().toList();

    assertEquals(3, emitted.size());
    assertEquals(new Signal(Sign.START, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.FAIL, CALLER), emitted.get(1).emission());
    assertEquals(new Signal(Sign.STOP, CALLER), emitted.get(2).emission());

  }

  @Test
  void testExecuteWithFunctionSuccess() {

    final var result = service.execute(CALLER, () -> "success");

    assertEquals("success", result);

    final var emitted = captures.drain().toList();

    assertEquals(3, emitted.size());
    assertEquals(new Signal(Sign.START, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.SUCCESS, CALLER), emitted.get(1).emission());
    assertEquals(new Signal(Sign.STOP, CALLER), emitted.get(2).emission());

  }

  @Test
  void testExecuteWithOperationFailure() {

    assertThrows(
      RuntimeException.class,
      () -> service.execute(
        CALLER,
        () -> {
          throw new RuntimeException("test");
        }
      )
    );

    final var emitted = captures.drain().toList();

    assertEquals(3, emitted.size());
    assertEquals(new Signal(Sign.START, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.FAIL, CALLER), emitted.get(1).emission());
    assertEquals(new Signal(Sign.STOP, CALLER), emitted.get(2).emission());

  }

  @Test
  void testExecuteWithOperationSuccess() {

    service.execute(CALLER, () -> {
    });

    final var emitted = captures.drain().toList();

    assertEquals(3, emitted.size());
    assertEquals(new Signal(Sign.START, CALLER), emitted.get(0).emission());
    assertEquals(new Signal(Sign.SUCCESS, CALLER), emitted.get(1).emission());
    assertEquals(new Signal(Sign.STOP, CALLER), emitted.get(2).emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpire() {

    service.expire(CALLER);

    assertSignal(
      new Signal(Sign.EXPIRE, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpired() {

    service.expire(CALLEE);

    assertSignal(
      new Signal(Sign.EXPIRE, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFail() {

    service.fail(CALLER);

    assertSignal(
      new Signal(Sign.FAIL, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testFailed() {

    service.fail(CALLEE);

    assertSignal(
      new Signal(Sign.FAIL, CALLEE)
    );

  }

  @Test
  void testFnOfCasting() {

    final Fn< String, RuntimeException > fn = () -> "test";

    final var casted = Fn.of(fn);

    assertEquals(fn, casted);

  }

  @Test
  void testFnOfNull() {

    assertThrows(
      NullPointerException.class,
      () -> Fn.of(null)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleEmissions() {

    service.start(CALLER);
    service.call(CALLER);
    service.success(CALLER);
    service.stop(CALLER);

    assertEquals(
      4L,
      captures
        .drain()
        .count()
    );

  }

  @Test
  void testOpFromFn() throws java.lang.Exception {

    final Fn< String, java.lang.Exception > fn = () -> "result";

    final Op< java.lang.Exception > op = Op.of(fn);

    op.exec();

  }

  @Test
  void testOpFromFnNull() {

    assertThrows(
      NullPointerException.class,
      () -> Op.of((Fn< String, java.lang.Exception >) null)
    );

  }

  @Test
  void testOpOfCasting() {

    final Op< RuntimeException > operation = () -> {
    };

    final var casted = Op.of(operation);

    assertEquals(operation, casted);

  }

  @Test
  void testOpOfNull() {

    assertThrows(
      NullPointerException.class,
      () -> Op.of((Op< java.lang.Exception >) null)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRecourse() {

    service.recourse(CALLER);

    assertSignal(
      new Signal(Sign.RECOURSE, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRecoursed() {

    service.recourse(CALLEE);

    assertSignal(
      new Signal(Sign.RECOURSE, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRedirect() {

    service.redirect(CALLER);

    assertSignal(
      new Signal(Sign.REDIRECT, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRedirected() {

    service.redirect(CALLEE);

    assertSignal(
      new Signal(Sign.REDIRECT, CALLEE)
    );

  }

  // Test all signals via emit

  @SpecRef({"6.3", "6.5"})
  @Test
  void testReject() {

    service.reject(CALLER);

    assertSignal(
      new Signal(Sign.REJECT, CALLER)
    );

  }

  // Test execute with function

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRejected() {

    service.reject(CALLEE);

    assertSignal(
      new Signal(Sign.REJECT, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testResume() {

    service.resume(CALLER);

    assertSignal(
      new Signal(Sign.RESUME, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testResumed() {

    service.resume(CALLEE);

    assertSignal(
      new Signal(Sign.RESUME, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRetried() {

    service.retry(CALLEE);

    assertSignal(
      new Signal(Sign.RETRY, CALLEE)
    );

  }

  // Test dispatch with function

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRetry() {

    service.retry(CALLER);

    assertSignal(
      new Signal(Sign.RETRY, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSchedule() {

    service.schedule(CALLER);

    assertSignal(
      new Signal(Sign.SCHEDULE, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testScheduled() {

    service.schedule(CALLEE);

    assertSignal(
      new Signal(Sign.SCHEDULE, CALLEE)
    );

  }

  /// Tests that [Sign] enum ordinals remain stable for compatibility.
  ///
  /// This test ensures that the ordinal values of [Sign] enum constants
  /// do not change, which is critical for serialization and external integrations.

  @SpecRef({"4.1", "registry:services"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, Sign.START.ordinal());
    assertEquals(1, Sign.STOP.ordinal());
    assertEquals(2, Sign.CALL.ordinal());
    assertEquals(3, Sign.SUCCESS.ordinal());
    assertEquals(4, Sign.FAIL.ordinal());
    assertEquals(5, Sign.RECOURSE.ordinal());
    assertEquals(6, Sign.REDIRECT.ordinal());
    assertEquals(7, Sign.EXPIRE.ordinal());
    assertEquals(8, Sign.RETRY.ordinal());
    assertEquals(9, Sign.REJECT.ordinal());
    assertEquals(10, Sign.DISCARD.ordinal());
    assertEquals(11, Sign.DELAY.ordinal());
    assertEquals(12, Sign.SCHEDULE.ordinal());
    assertEquals(13, Sign.SUSPEND.ordinal());
    assertEquals(14, Sign.RESUME.ordinal());
    assertEquals(15, Sign.DISCONNECT.ordinal());

  }

  @SpecRef("6.2")
  @Test
  void testSignal() {

    // Test direct signal() method for all sign and dimension combinations
    service.signal(Sign.START, CALLER);
    service.signal(Sign.START, CALLEE);
    service.signal(Sign.STOP, CALLER);
    service.signal(Sign.STOP, CALLEE);
    service.signal(Sign.CALL, CALLER);
    service.signal(Sign.CALL, CALLEE);
    service.signal(Sign.SUCCESS, CALLER);
    service.signal(Sign.SUCCESS, CALLEE);
    service.signal(Sign.FAIL, CALLER);
    service.signal(Sign.FAIL, CALLEE);
    service.signal(Sign.RECOURSE, CALLER);
    service.signal(Sign.RECOURSE, CALLEE);
    service.signal(Sign.REDIRECT, CALLER);
    service.signal(Sign.REDIRECT, CALLEE);
    service.signal(Sign.EXPIRE, CALLER);
    service.signal(Sign.EXPIRE, CALLEE);
    service.signal(Sign.RETRY, CALLER);
    service.signal(Sign.RETRY, CALLEE);
    service.signal(Sign.REJECT, CALLER);
    service.signal(Sign.REJECT, CALLEE);
    service.signal(Sign.DISCARD, CALLER);
    service.signal(Sign.DISCARD, CALLEE);
    service.signal(Sign.DELAY, CALLER);
    service.signal(Sign.DELAY, CALLEE);
    service.signal(Sign.SCHEDULE, CALLER);
    service.signal(Sign.SCHEDULE, CALLEE);
    service.signal(Sign.SUSPEND, CALLER);
    service.signal(Sign.SUSPEND, CALLEE);
    service.signal(Sign.RESUME, CALLER);
    service.signal(Sign.RESUME, CALLEE);
    service.signal(Sign.DISCONNECT, CALLER);
    service.signal(Sign.DISCONNECT, CALLEE);

    final var signals =
      captures
        .drainEmissions()
        .toList();

    assertEquals(32, signals.size());
    assertEquals(new Signal(Sign.START, CALLER), signals.get(0));
    assertEquals(new Signal(Sign.START, CALLEE), signals.get(1));
    assertEquals(new Signal(Sign.STOP, CALLER), signals.get(2));
    assertEquals(new Signal(Sign.STOP, CALLEE), signals.get(3));
    assertEquals(new Signal(Sign.CALL, CALLER), signals.get(4));
    assertEquals(new Signal(Sign.CALL, CALLEE), signals.get(5));
    assertEquals(new Signal(Sign.SUCCESS, CALLER), signals.get(6));
    assertEquals(new Signal(Sign.SUCCESS, CALLEE), signals.get(7));
    assertEquals(new Signal(Sign.FAIL, CALLER), signals.get(8));
    assertEquals(new Signal(Sign.FAIL, CALLEE), signals.get(9));
    assertEquals(new Signal(Sign.RECOURSE, CALLER), signals.get(10));
    assertEquals(new Signal(Sign.RECOURSE, CALLEE), signals.get(11));
    assertEquals(new Signal(Sign.REDIRECT, CALLER), signals.get(12));
    assertEquals(new Signal(Sign.REDIRECT, CALLEE), signals.get(13));
    assertEquals(new Signal(Sign.EXPIRE, CALLER), signals.get(14));
    assertEquals(new Signal(Sign.EXPIRE, CALLEE), signals.get(15));
    assertEquals(new Signal(Sign.RETRY, CALLER), signals.get(16));
    assertEquals(new Signal(Sign.RETRY, CALLEE), signals.get(17));
    assertEquals(new Signal(Sign.REJECT, CALLER), signals.get(18));
    assertEquals(new Signal(Sign.REJECT, CALLEE), signals.get(19));
    assertEquals(new Signal(Sign.DISCARD, CALLER), signals.get(20));
    assertEquals(new Signal(Sign.DISCARD, CALLEE), signals.get(21));
    assertEquals(new Signal(Sign.DELAY, CALLER), signals.get(22));
    assertEquals(new Signal(Sign.DELAY, CALLEE), signals.get(23));
    assertEquals(new Signal(Sign.SCHEDULE, CALLER), signals.get(24));
    assertEquals(new Signal(Sign.SCHEDULE, CALLEE), signals.get(25));
    assertEquals(new Signal(Sign.SUSPEND, CALLER), signals.get(26));
    assertEquals(new Signal(Sign.SUSPEND, CALLEE), signals.get(27));
    assertEquals(new Signal(Sign.RESUME, CALLER), signals.get(28));
    assertEquals(new Signal(Sign.RESUME, CALLEE), signals.get(29));
    assertEquals(new Signal(Sign.DISCONNECT, CALLER), signals.get(30));
    assertEquals(new Signal(Sign.DISCONNECT, CALLEE), signals.get(31));

  }

  // Multiple emissions test

  @SpecRef("4.4")
  @Test
  void testSignalAccessors() {

    final var START = new Signal(Sign.START, CALLER);
    final var STARTED = new Signal(Sign.START, CALLEE);
    final var SUCCESS = new Signal(Sign.SUCCESS, CALLER);
    final var SUCCEEDED = new Signal(Sign.SUCCESS, CALLEE);

    assertEquals(Sign.START, START.sign());
    assertEquals(Dimension.CALLER, START.dimension());

    assertEquals(Sign.START, STARTED.sign());
    assertEquals(Dimension.CALLEE, STARTED.dimension());

    assertEquals(Sign.SUCCESS, SUCCESS.sign());
    assertEquals(Dimension.CALLER, SUCCESS.dimension());

    assertEquals(Sign.SUCCESS, SUCCEEDED.sign());
    assertEquals(Dimension.CALLEE, SUCCEEDED.dimension());

  }

  // Subject association test

  /// A signal is a sign together with a dimension, and reports back the pair it was made
  /// from. Which signs and dimensions exist, and in what order, is pinned once for every
  /// vocabulary by the consolidated tables in `SymbolContractTest` — repeating it per domain
  /// left the same fact asserted in two places and cited in neither completely.

  @SpecRef("4.4")
  @Test
  void testSignalComposition() {

    final var signal = new Signal(Sign.START, Dimension.CALLER);
    assertEquals(Sign.START, signal.sign());
    assertEquals(Dimension.CALLER, signal.dimension());

  }

  @SpecRef({"4.4", "4.6"})
  @Test
  void testSignalDimensionMappings() {

    // Verify all Sign×Dimension combinations can be constructed
    for (final var sign : Sign.values()) {

      for (final var dimension : Dimension.values()) {

        final var signal = new Signal(sign, dimension);

        assertEquals(
          sign,
          signal.sign()
        );

        assertEquals(
          dimension,
          signal.dimension()
        );

      }

    }

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStart() {

    service.start(CALLER);

    assertSignal(
      new Signal(Sign.START, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStarted() {

    service.start(CALLEE);

    assertSignal(
      new Signal(Sign.START, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStop() {

    service.stop(CALLER);

    assertSignal(
      new Signal(Sign.STOP, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStopped() {

    service.stop(CALLEE);

    assertSignal(
      new Signal(Sign.STOP, CALLEE)
    );

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAssociation() {

    service.start(CALLER);

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
  void testSucceeded() {

    service.success(CALLEE);

    assertSignal(
      new Signal(Sign.SUCCESS, CALLEE)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuccess() {

    service.success(CALLER);

    assertSignal(
      new Signal(Sign.SUCCESS, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspend() {

    service.suspend(CALLER);

    assertSignal(
      new Signal(Sign.SUSPEND, CALLER)
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSuspended() {

    service.suspend(CALLEE);

    assertSignal(
      new Signal(Sign.SUSPEND, CALLEE)
    );

  }

}
