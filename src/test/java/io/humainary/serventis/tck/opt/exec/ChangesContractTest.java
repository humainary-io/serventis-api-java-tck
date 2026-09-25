// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.exec;

import io.humainary.serventis.opt.exec.*;
import io.humainary.serventis.opt.exec.Changes.*;
import io.humainary.serventis.opt.exec.Changes.Change;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.humainary.serventis.opt.exec.Changes.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Changes] API.
/// @author William David Louth
/// @since 3.6

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md")
final class ChangesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("checkout.deployment");

  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Change change;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    change =
      conduit.pool(Changes::of)
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
  void testConvenienceMethodsEmitTheirSign() {

    change.start();
    change.apply();
    change.fail();
    change.reject();
    change.revert();
    change.progress();

    assertEquals(
      List.of(START, APPLY, FAIL, REJECT, REVERT, PROGRESS),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testEpisodes() {

    change.start();
    change.apply();
    change.start();
    change.revert();
    change.start();
    change.reject();
    change.start();
    change.progress();
    change.progress();
    change.apply();

    assertEquals(
      List.of(START, APPLY, START, REVERT, START, REJECT, START, PROGRESS, PROGRESS, APPLY),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    for (final var sign : Sign.values()) {
      change.sign(sign);
    }

    assertEquals(
      List.of(Sign.values()),
      captures
        .drainEmissions()
        .toList()
    );

  }

  @SpecRef({"4.1", "registry:changes"})
  @Test
  void testSignEnumOrdinals() {

    assertEquals(0, START.ordinal());
    assertEquals(1, APPLY.ordinal());
    assertEquals(2, FAIL.ordinal());
    assertEquals(3, REJECT.ordinal());
    assertEquals(4, REVERT.ordinal());
    assertEquals(5, PROGRESS.ordinal());

  }

  @SpecRef({"4.5", "registry:changes"})
  @Test
  void testSignEnumValues() {

    assertEquals(6, Sign.values().length);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    change.start();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(START, capture.emission());

  }

}
