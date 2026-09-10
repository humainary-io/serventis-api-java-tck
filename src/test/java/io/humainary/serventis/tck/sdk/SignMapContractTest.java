// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.api.*;
import io.humainary.serventis.sdk.*;
import org.junit.jupiter.api.*;

import java.util.function.*;

import static io.humainary.serventis.tck.sdk.SignMapContractTest.Source.*;
import static io.humainary.serventis.tck.sdk.SignMapContractTest.Target.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for the [SignMap] projection and the [SignSet] construction path.
/// @author William David Louth
/// @since 3.0

@SuppressWarnings("DataFlowIssue")  // deliberate null-rejection coverage
final class SignMapContractTest {

  private static final SignSet< Source > SIGNS =
    SignSet.of(Source.class);

  private static Target ballot(
    final Source sign
  ) {

    return
      switch (sign) {
        case A -> LOW;
        case B, C -> HIGH;
        case D -> null;        // abstain — no translation
      };

  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void functionInvokedOncePerConstant() {

    final var calls = new int[]{0};

    final var map =
      SIGNS.map(
        sign -> {
          calls[0]++;
          return ballot(sign);
        }
      );

    // Eager: invoked exactly once per source constant at construction.
    assertEquals(Source.values().length, calls[0]);

    // apply() is a pure array load — no further invocations.
    map.apply(A);
    map.apply(B);
    map.apply(D);

    assertEquals(Source.values().length, calls[0]);

  }

  @Test
  void mapsToNonSignValue() {

    // Generalized (unbounded T): a sign may project to any value — e.g. a vote weight.
    final var weight =
      SIGNS.map(
        sign -> switch (sign) {
          case A -> 1.0;
          case B, C -> 0.5;
          case D -> 0.0;
        }
      );

    assertEquals(1.0, weight.apply(A), 0.0);
    assertEquals(0.5, weight.apply(B), 0.0);
    assertEquals(0.5, weight.apply(C), 0.0);
    assertEquals(0.0, weight.apply(D), 0.0);

  }

  @Test
  void nullArgumentsRejected() {

    assertThrows(
      NullPointerException.class,
      () -> SignSet.of(null)
    );

    assertThrows(
      NullPointerException.class,
      () -> SIGNS.map(null)
    );

  }

  @Test
  void nullIsAbstain() {

    final var map =
      SIGNS.map(SignMapContractTest::ballot);

    assertNull(map.apply(D));

  }

  @Test
  void ordinalLookupIsCorrect() {

    assertEquals(0, A.ordinal());
    assertEquals(1, B.ordinal());
    assertEquals(2, C.ordinal());
    assertEquals(3, D.ordinal());

    final var map =
      SIGNS.map(SignMapContractTest::ballot);

    assertEquals(LOW, map.apply(A));
    assertEquals(HIGH, map.apply(B));
    assertEquals(HIGH, map.apply(C));
    assertNull(map.apply(D));

  }

  @Test
  void signSetCreatesMapsFromCapturedSigns() {

    final var signs =
      SignSet.of(Source.class);

    final var map =
      signs.map(SignMapContractTest::ballot);

    assertEquals(LOW, map.apply(A));
    assertEquals(HIGH, map.apply(B));

  }

  @Test
  void singleConstantSource() {

    final var map =
      SignSet.of(Single.class).map(_ -> HIGH);

    assertEquals(HIGH, map.apply(Single.ONLY));

  }

  @Test
  void translatesEachSign() {

    final var map =
      SIGNS.map(SignMapContractTest::ballot);

    assertEquals(LOW, map.apply(A));
    assertEquals(HIGH, map.apply(B));
    assertEquals(HIGH, map.apply(C));

  }

  @Test
  void usableAsFunction() {

    final Function< Source, Target > fn =
      SIGNS.map(SignMapContractTest::ballot);

    assertEquals(LOW, fn.apply(A));
    assertEquals(HIGH, fn.apply(B));

  }

  /// Source sign enum: A, B, C translate; D abstains.
  enum Source implements Serventis.Sign {
    A,
    B,
    C,
    D
  }

  /// Target sign enum.
  enum Target implements Serventis.Sign {
    LOW,
    HIGH
  }

  /// Single-constant source (edge case).
  enum Single implements Serventis.Sign {
    ONLY
  }

}
