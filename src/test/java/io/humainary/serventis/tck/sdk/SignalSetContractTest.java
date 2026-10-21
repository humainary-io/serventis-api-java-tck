// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.api.*;
import io.humainary.serventis.api.Serventis.*;
import io.humainary.serventis.sdk.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.tck.sdk.SignalSetContractTest.TestDimension.*;
import static io.humainary.serventis.tck.sdk.SignalSetContractTest.TestSign.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for [SignalSet] and its [SignSet]-based construction path.
/// @author William David Louth
/// @since 1.0

final class SignalSetContractTest {

  @Test
  void testAllSignDimensionCombinations() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    // Test all 6 combinations
    final var combinations = new TestSignal[]{
      signalSet.get(CONNECT, CLIENT),
      signalSet.get(CONNECT, SERVER),
      signalSet.get(SEND, CLIENT),
      signalSet.get(SEND, SERVER),
      signalSet.get(RECEIVE, CLIENT),
      signalSet.get(RECEIVE, SERVER)
    };

    // Verify each combination has correct sign and dimension
    assertEquals(CONNECT, combinations[0].sign());
    assertEquals(CLIENT, combinations[0].dimension());

    assertEquals(CONNECT, combinations[1].sign());
    assertEquals(SERVER, combinations[1].dimension());

    assertEquals(SEND, combinations[2].sign());
    assertEquals(CLIENT, combinations[2].dimension());

    assertEquals(SEND, combinations[3].sign());
    assertEquals(SERVER, combinations[3].dimension());

    assertEquals(RECEIVE, combinations[4].sign());
    assertEquals(CLIENT, combinations[4].dimension());

    assertEquals(RECEIVE, combinations[5].sign());
    assertEquals(SERVER, combinations[5].dimension());

    // All should be distinct instances
    for (int i = 0; i < combinations.length; i++) {
      for (int j = i + 1; j < combinations.length; j++) {
        assertNotSame(combinations[i], combinations[j]);
      }
    }

  }

  @Test
  void testConstructorCreatesAllCombinations() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    // 3 signs × 2 dimensions = 6 signals, each holding the pair it is looked up by
    for (final var sign : TestSign.values()) {
      for (final var dimension : TestDimension.values()) {

        final var signal = signalSet.get(sign, dimension);

        assertNotNull(signal, () -> sign + " × " + dimension + " has no signal");
        assertSame(sign, signal.sign());
        assertSame(dimension, signal.dimension());

      }
    }

  }

  @Test
  void testFactoryCalledCorrectNumberOfTimes() {

    final var callCount = new int[]{0};

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        (sign, dimension) -> {
          callCount[0]++;
          return new TestSignal(sign, dimension);
        }
      );

    // Factory should have been called exactly 3 × 2 = 6 times during construction
    assertEquals(6, callCount[0]);

    // Getting signals should NOT call factory (pre-allocated)
    signalSet.get(CONNECT, CLIENT);
    signalSet.get(SEND, SERVER);
    signalSet.get(RECEIVE, CLIENT);

    // Count should still be 6 (no additional calls)
    assertEquals(6, callCount[0]);

  }

  /// A factory that builds a signal for another cell is rejected at construction: lookups find a
  /// cell by its coordinates, so the set would hand back a signal it was not asked for.
  @Test
  void testFactoryReturningMismatchedSignalIsRejected() {

    final var signs = SignSet.of(TestSign.class);
    final var dimensions = SymbolSet.of(TestDimension.class);

    assertThrows(
      IllegalArgumentException.class,
      () -> signs.signals(dimensions, (_, dimension) -> new TestSignal(CONNECT, dimension))
    );

    assertThrows(
      IllegalArgumentException.class,
      () -> signs.signals(dimensions, (sign, _) -> new TestSignal(sign, CLIENT))
    );

  }

  /// `signals` rejects an absent dimension set or factory, as documented, at the call rather than
  /// on first use.
  @Test
  void testSignalsRejectsAbsentArguments() {

    final var signs = SignSet.of(TestSign.class);
    final var dimensions = SymbolSet.of(TestDimension.class);

    assertThrows(
      NullPointerException.class,
      () -> signs.signals(null, TestSignal::new)
    );

    assertThrows(
      NullPointerException.class,
      () -> signs.signals(dimensions, (java.util.function.BiFunction< TestSign, TestDimension, TestSignal >) null)
    );

  }

  /// A factory that returns no signal for a cell is rejected at construction, not later by the
  /// pipe the absent signal would have been emitted through.
  @Test
  void testFactoryReturningNullIsRejected() {

    final var signs = SignSet.of(TestSign.class);
    final var dimensions = SymbolSet.of(TestDimension.class);

    assertThrows(
      NullPointerException.class,
      () -> signs.signals(
        dimensions,
        (sign, dimension) -> sign==SEND ? null:new TestSignal(sign, dimension)
      )
    );

  }

  @Test
  void testGetReturnsCorrectSignal() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal =
      signalSet.get(
        CONNECT,
        CLIENT
      );

    assertNotNull(signal);
    assertEquals(CONNECT, signal.sign());
    assertEquals(CLIENT, signal.dimension());

  }

  @Test
  void testGetReturnsDistinctSignals() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal1 =
      signalSet.get(
        CONNECT,
        CLIENT
      );

    final var signal2 =
      signalSet.get(
        CONNECT,
        SERVER
      );

    final var signal3 =
      signalSet.get(
        SEND,
        CLIENT
      );

    assertNotSame(signal1, signal2);
    assertNotSame(signal1, signal3);
    assertNotSame(signal2, signal3);

  }

  @Test
  void testGetReturnsPreAllocatedSignals() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal1 =
      signalSet.get(
        CONNECT,
        CLIENT
      );

    final var signal2 =
      signalSet.get(
        CONNECT,
        CLIENT
      );

    // Should return same pre-allocated instance (no new allocation)
    assertSame(signal1, signal2);

  }

  @Test
  void testLookupPerformancePattern() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    // Simulate real-world usage pattern: repeated lookups
    for (int i = 0; i < 1000; i++) {

      final var signal1 = signalSet.get(CONNECT, CLIENT);
      final var signal2 = signalSet.get(SEND, SERVER);
      final var signal3 = signalSet.get(RECEIVE, CLIENT);

      assertNotNull(signal1);
      assertNotNull(signal2);
      assertNotNull(signal3);

    }

    // Should still return same pre-allocated instances
    final var finalSignal = signalSet.get(CONNECT, CLIENT);
    final var firstSignal = signalSet.get(CONNECT, CLIENT);
    assertSame(finalSignal, firstSignal);

  }

  @Test
  void testRepeatedGetsSameInstance() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    // Get same signal multiple times
    final var signal1 = signalSet.get(SEND, SERVER);
    final var signal2 = signalSet.get(SEND, SERVER);
    final var signal3 = signalSet.get(SEND, SERVER);
    final var signal4 = signalSet.get(SEND, SERVER);

    // All should be same instance (zero allocation)
    assertSame(signal1, signal2);
    assertSame(signal2, signal3);
    assertSame(signal3, signal4);

  }

  @Test
  void testSignOrdinalsRespected() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    // Verify ordinals are used correctly for lookup
    assertEquals(0, CONNECT.ordinal());
    assertEquals(1, SEND.ordinal());
    assertEquals(2, RECEIVE.ordinal());

    assertEquals(0, CLIENT.ordinal());
    assertEquals(1, SERVER.ordinal());

    // Get signals in different ordinal order
    final var signal_0_0 = signalSet.get(CONNECT, CLIENT);
    final var signal_0_1 = signalSet.get(CONNECT, SERVER);
    final var signal_1_0 = signalSet.get(SEND, CLIENT);
    final var signal_1_1 = signalSet.get(SEND, SERVER);
    final var signal_2_0 = signalSet.get(RECEIVE, CLIENT);
    final var signal_2_1 = signalSet.get(RECEIVE, SERVER);

    // Verify correct sign/dimension pairings
    assertEquals(CONNECT, signal_0_0.sign());
    assertEquals(CLIENT, signal_0_0.dimension());

    assertEquals(CONNECT, signal_0_1.sign());
    assertEquals(SERVER, signal_0_1.dimension());

    assertEquals(SEND, signal_1_0.sign());
    assertEquals(CLIENT, signal_1_0.dimension());

    assertEquals(SEND, signal_1_1.sign());
    assertEquals(SERVER, signal_1_1.dimension());

    assertEquals(RECEIVE, signal_2_0.sign());
    assertEquals(CLIENT, signal_2_0.dimension());

    assertEquals(RECEIVE, signal_2_1.sign());
    assertEquals(SERVER, signal_2_1.dimension());

  }

  @Test
  void testSignSetSignalsConstruction() {

    // Construction goes through SignSet.signals — the captured sign set crossed with the dimension
    // enum. The BiFunction<S,D,T> ties sign, dimension, and signal so the compiler checks consistency.
    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal =
      signalSet.get(
        CONNECT,
        SERVER
      );

    assertNotNull(signal);
    assertEquals(CONNECT, signal.sign());
    assertEquals(SERVER, signal.dimension());

    // Repeated gets return the same pre-allocated instance, as with the three-argument form
    assertSame(
      signal,
      signalSet.get(CONNECT, SERVER)
    );

  }

  @Test
  void testSignalEquality() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal1 = signalSet.get(CONNECT, CLIENT);
    final var signal2 = signalSet.get(CONNECT, CLIENT);

    // Same instance, so should be equal
    assertEquals(signal1, signal2);

    // Records define equals based on components
    final var manualSignal = new TestSignal(CONNECT, CLIENT);
    assertEquals(signal1, manualSignal);

  }

  @Test
  void testSignalHashCode() {

    final var signalSet =
      SignSet.of(TestSign.class).signals(
        SymbolSet.of(TestDimension.class),
        TestSignal::new
      );

    final var signal = signalSet.get(SEND, CLIENT);
    final var manualSignal = new TestSignal(SEND, CLIENT);

    // Records define hashCode based on components
    assertEquals(signal.hashCode(), manualSignal.hashCode());

  }

  @Test
  void testSingleDimensionSignalSet() {

    final var signalSet =
      SignSet.of(SingleSign.class).signals(
        SymbolSet.of(SingleDimension.class),
        SingleSignal::new
      );

    final var signal = signalSet.get(SingleSign.SINGLE, SingleDimension.ONLY);

    assertNotNull(signal);
    assertEquals(SingleSign.SINGLE, signal.sign());
    assertEquals(SingleDimension.ONLY, signal.dimension());

    // Repeated gets should return same instance
    final var signal2 = signalSet.get(SingleSign.SINGLE, SingleDimension.ONLY);
    assertSame(signal, signal2);

  }

  /// Test sign enum with 3 values
  enum TestSign implements Serventis.Sign {
    CONNECT,
    SEND,
    RECEIVE
  }

  /// Test dimension enum with 2 values
  enum TestDimension implements Category {
    CLIENT,
    SERVER
  }

  /// Test dimension enum with single value (edge case)
  enum SingleDimension implements Category {
    ONLY
  }

  /// Test sign enum with single value (edge case)
  enum SingleSign implements Serventis.Sign {
    SINGLE
  }

  /// Single signal record
  record SingleSignal(
    SingleSign sign,
    SingleDimension dimension
  ) implements Serventis.Signal< SingleSign, SingleDimension > {
  }

  /// Test signal record
  record TestSignal(
    TestSign sign,
    TestDimension dimension
  ) implements Serventis.Signal< TestSign, TestDimension > {
  }

}
