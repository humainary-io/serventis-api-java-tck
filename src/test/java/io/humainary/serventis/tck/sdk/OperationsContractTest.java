// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.sdk;

import io.humainary.serventis.sdk.*;
import io.humainary.serventis.sdk.Operations.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.sdk.Operations.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Operations] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class OperationsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("db.query");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Operation operation;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    operation =
      Operations.pool(conduit)
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
  void testAdvance() {

    operation.advance();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(ADVANCE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBegin() {

    operation.begin();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(BEGIN, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testBeginEndPattern() {

    operation.begin();
    operation.end();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(BEGIN, signs.getFirst());
    assertEquals(END, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testEnd() {

    operation.end();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(END, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMultipleOperationInstruments() {

    final var queryName = CORTEX.name("db.query");
    final var txName = CORTEX.name("db.transaction");

    final var conduit = circuit.conduit(Sign.class);
    final var operations = Operations.pool(conduit);
    final var queryOp = operations.get(queryName);
    final var txOp = operations.get(txName);
    final var signCaptures = CaptureBuffer.of(circuit, conduit);

    txOp.begin();
    queryOp.begin();
    queryOp.end();
    txOp.end();

    final var allCaptures = signCaptures.drain().toList();
    signCaptures.close();

    assertEquals(4, allCaptures.size());

    assertEquals(txName, allCaptures.get(0).subject().name());
    assertEquals(BEGIN, allCaptures.get(0).emission());

    assertEquals(queryName, allCaptures.get(1).subject().name());
    assertEquals(BEGIN, allCaptures.get(1).emission());

    assertEquals(queryName, allCaptures.get(2).subject().name());
    assertEquals(END, allCaptures.get(2).emission());

    assertEquals(txName, allCaptures.get(3).subject().name());
    assertEquals(END, allCaptures.get(3).emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNestedOperations() {

    operation.begin();
    operation.begin();
    operation.end();
    operation.begin();
    operation.end();
    operation.end();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(BEGIN, signs.get(0));
    assertEquals(BEGIN, signs.get(1));
    assertEquals(END, signs.get(2));
    assertEquals(BEGIN, signs.get(3));
    assertEquals(END, signs.get(4));
    assertEquals(END, signs.get(5));

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    operation.sign(BEGIN);
    operation.sign(END);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(BEGIN, signs.get(0));
    assertEquals(END, signs.get(1));

  }

  @SpecRef({"4.1", "7.3"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, BEGIN.ordinal());
    assertEquals(1, ADVANCE.ordinal());
    assertEquals(2, END.ordinal());

  }

  @SpecRef({"4.5", "7.3"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(3, values.length);
    assertEquals(BEGIN, values[0]);
    assertEquals(ADVANCE, values[1]);
    assertEquals(END, values[2]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    operation.begin();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(BEGIN, capture.emission());

  }

}
