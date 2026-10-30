// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.api.*;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for [SymbolSet] — the genus container shared by [Serventis.Sign] and
/// [Serventis.Dimension] symbol enums.
/// @author William David Louth
/// @since 3.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.15.1/SPEC.md")
@SuppressWarnings("DataFlowIssue")  // deliberate null-rejection coverage
final class SymbolSetContractTest {

  @SpecRef("4.5")
  @Test
  void capturesDimensionSetCardinality() {

    // The same container reifies a Dimension enum, not only a Sign enum.
    assertEquals(
      2,
      SymbolSet.of(TestDimension.class).size()
    );

  }

  /// §4.3 asks a projection that cannot make a mixed-kind dimension set unrepresentable to reject
  /// one at set construction, and §10.4 asks a suite to test it. Both kinds are open interfaces in
  /// Java, so such an enum compiles; building its set must fail.
  @SpecRef({"4.3", "10.4"})
  @Test
  void rejectsDimensionDeclaringBothKinds() {

    assertThrows(
      IllegalArgumentException.class,
      () -> SymbolSet.of(BothKinds.class)
    );

  }

  private enum BothKinds
    implements Serventis.Category, Serventis.Spectrum {
    ONLY
  }

  @SpecRef("4.5")
  @Test
  void capturesSignSetCardinality() {

    assertEquals(
      3,
      SymbolSet.of(TestSign.class).size()
    );

  }

  @Test
  void nullSourceRejected() {

    assertThrows(
      NullPointerException.class,
      () -> SymbolSet.of(null)
    );

  }

  enum TestSign
    implements Serventis.Sign {
    A,
    B,
    C
  }

  enum TestDimension
    implements Serventis.Category {
    X,
    Y
  }

}
