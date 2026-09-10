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

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
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
