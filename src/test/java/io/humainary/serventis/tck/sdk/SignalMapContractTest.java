// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.api.*;
import io.humainary.serventis.sdk.*;
import org.junit.jupiter.api.*;

import java.util.function.*;

import static io.humainary.serventis.tck.sdk.SignalMapContractTest.SourceDimension.*;
import static io.humainary.serventis.tck.sdk.SignalMapContractTest.SourceSign.*;
import static io.humainary.serventis.tck.sdk.SignalMapContractTest.Target.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for the [SignalMap] projection and the [SignalSet] construction path.
/// @author William David Louth
/// @since 3.0

@SuppressWarnings("DataFlowIssue")  // deliberate null-rejection coverage
final class SignalMapContractTest {

  private static final SignalSet< SourceSign, SourceDimension, SourceSignal > SIGNALS =
    SignSet.of(SourceSign.class).signals(
      SymbolSet.of(SourceDimension.class),
      SourceSignal::new
    );

  private static Target ballot(
    final SourceSignal signal
  ) {

    return
      switch (signal.sign()) {
        case CONNECT -> signal.dimension()==CLIENT ? LOW:HIGH;
        case SEND -> signal.dimension()==CLIENT ? HIGH:LOW;
        case RECEIVE -> null;        // abstain — no translation
      };

  }

  @Test
  void functionInvokedOncePerSignal() {

    final var calls = new int[]{0};

    final var map =
      SIGNALS.map(
        signal -> {
          calls[0]++;
          return ballot(signal);
        }
      );

    assertEquals(
      SourceSign.values().length * SourceDimension.values().length,
      calls[0]
    );

    map.apply(
      SIGNALS.get(
        CONNECT,
        CLIENT
      )
    );

    map.get(
      SEND,
      SERVER
    );

    map.get(
      RECEIVE,
      CLIENT
    );

    assertEquals(
      SourceSign.values().length * SourceDimension.values().length,
      calls[0]
    );

  }

  @Test
  void mapsToNonSignValue() {

    final var weight =
      SIGNALS.map(
        signal -> signal.sign()==CONNECT && signal.dimension()==SERVER ? 1.0:0.5
      );

    assertEquals(
      0.5,
      weight.get(
        CONNECT,
        CLIENT
      ),
      0.0
    );

    assertEquals(
      1.0,
      weight.get(
        CONNECT,
        SERVER
      ),
      0.0
    );

  }

  @Test
  void nullArgumentsRejected() {

    assertThrows(
      NullPointerException.class,
      () -> SIGNALS.map(null)
    );

  }

  @Test
  void nullIsAbstain() {

    final var map =
      SIGNALS.map(SignalMapContractTest::ballot);

    assertNull(
      map.get(
        RECEIVE,
        CLIENT
      )
    );

    assertNull(
      map.get(
        RECEIVE,
        SERVER
      )
    );

  }

  @Test
  void ordinalPairLookupIsCorrect() {

    assertEquals(0, CONNECT.ordinal());
    assertEquals(1, SEND.ordinal());
    assertEquals(2, RECEIVE.ordinal());

    assertEquals(0, CLIENT.ordinal());
    assertEquals(1, SERVER.ordinal());

    final var map =
      SIGNALS.map(SignalMapContractTest::ballot);

    assertEquals(
      LOW,
      map.get(
        CONNECT,
        CLIENT
      )
    );

    assertEquals(
      HIGH,
      map.get(
        CONNECT,
        SERVER
      )
    );

    assertEquals(
      HIGH,
      map.get(
        SEND,
        CLIENT
      )
    );

    assertEquals(
      LOW,
      map.get(
        SEND,
        SERVER
      )
    );

  }

  @Test
  void signalSetCreatesMapsFromCapturedSignals() {

    final var signals =
      SignSet.of(SourceSign.class).signals(
        SymbolSet.of(SourceDimension.class),
        SourceSignal::new
      );

    final var map =
      signals.map(SignalMapContractTest::ballot);

    assertEquals(
      LOW,
      map.get(
        CONNECT,
        CLIENT
      )
    );

    assertEquals(
      HIGH,
      map.get(
        CONNECT,
        SERVER
      )
    );

  }

  @Test
  void singleSignalSource() {

    final var signals =
      SignSet.of(SingleSign.class).signals(
        SymbolSet.of(SingleDimension.class),
        SingleSignal::new
      );

    final var map =
      signals.map(signal -> HIGH);

    assertEquals(
      HIGH,
      map.get(
        SingleSign.ONLY,
        SingleDimension.ONLY
      )
    );

  }

  @Test
  void usableAsFunction() {

    final Function< SourceSignal, Target > fn =
      SIGNALS.map(SignalMapContractTest::ballot);

    assertEquals(
      LOW,
      fn.apply(
        SIGNALS.get(
          CONNECT,
          CLIENT
        )
      )
    );

    assertEquals(
      HIGH,
      fn.apply(
        SIGNALS.get(
          CONNECT,
          SERVER
        )
      )
    );

  }

  enum SourceSign
    implements Serventis.Sign {
    CONNECT,
    SEND,
    RECEIVE
  }

  enum SourceDimension
    implements Serventis.Category {
    CLIENT,
    SERVER
  }

  enum Target
    implements Serventis.Sign {
    LOW,
    HIGH
  }

  enum SingleSign
    implements Serventis.Sign {
    ONLY
  }

  enum SingleDimension
    implements Serventis.Category {
    ONLY
  }

  record SingleSignal(
    SingleSign sign,
    SingleDimension dimension
  ) implements Serventis.Signal< SingleSign, SingleDimension > {
  }

  record SourceSignal(
    SourceSign sign,
    SourceDimension dimension
  ) implements Serventis.Signal< SourceSign, SourceDimension > {
  }

}
