// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.tool;

import io.humainary.serventis.opt.tool.*;
import io.humainary.serventis.opt.tool.Logs.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.tool.Logs.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Logs] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class LogsContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("com.acme.PaymentService");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Log log;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    log =
      conduit.pool(Logs::of)
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
  void testAllLevels() {

    log.severe();
    log.warning();
    log.info();
    log.debug();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(SEVERE, signs.getFirst());
    assertEquals(WARNING, signs.get(1));
    assertEquals(INFO, signs.get(2));
    assertEquals(DEBUG, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testDebug() {

    log.debug();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(DEBUG, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testErrorPattern() {

    // Simulate error pattern
    log.info();
    log.warning();
    log.severe();
    log.severe();
    log.severe();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(INFO, signs.getFirst());
    assertEquals(WARNING, signs.get(1));
    assertEquals(SEVERE, signs.get(2));
    assertEquals(SEVERE, signs.get(3));
    assertEquals(SEVERE, signs.get(4));

    // Count severe errors
    final var severeCount =
      signs
        .stream()
        .filter(s -> s==SEVERE)
        .count();

    assertEquals(3, severeCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testInfo() {

    log.info();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(INFO, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testNormalOperationPattern() {

    // Simulate normal operation with occasional warnings
    log.info();
    log.info();
    log.warning();
    log.info();
    log.info();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());

    final var infoCount =
      signs
        .stream()
        .filter(s -> s==INFO)
        .count();

    final var warningCount =
      signs
        .stream()
        .filter(s -> s==WARNING)
        .count();

    assertEquals(4, infoCount);
    assertEquals(1, warningCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testSevere() {

    log.severe();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(SEVERE, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    log.sign(SEVERE);
    log.sign(WARNING);
    log.sign(INFO);
    log.sign(DEBUG);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(SEVERE, signs.get(0));
    assertEquals(WARNING, signs.get(1));
    assertEquals(INFO, signs.get(2));
    assertEquals(DEBUG, signs.get(3));

  }

  @SpecRef({"4.1", "registry:logs"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, SEVERE.ordinal());
    assertEquals(1, WARNING.ordinal());
    assertEquals(2, INFO.ordinal());
    assertEquals(3, DEBUG.ordinal());

  }

  @SpecRef({"4.5", "registry:logs"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(4, values.length);
    assertEquals(SEVERE, values[0]);
    assertEquals(WARNING, values[1]);
    assertEquals(INFO, values[2]);
    assertEquals(DEBUG, values[3]);

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    log.info();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(INFO, capture.emission());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testVerboseLoggingPattern() {

    // Simulate verbose debug logging
    log.debug();
    log.debug();
    log.debug();
    log.debug();
    log.debug();
    log.info();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());

    final var debugCount =
      signs
        .stream()
        .filter(s -> s==DEBUG)
        .count();

    assertEquals(5, debugCount);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testWarning() {

    log.warning();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(WARNING, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testWarningCrescendo() {

    // Simulate warning crescendo pattern (increasing warnings)
    log.info();
    log.warning();
    log.warning();
    log.warning();
    log.severe();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());

    final var warningCount =
      signs
        .stream()
        .filter(s -> s==WARNING)
        .count();

    assertEquals(3, warningCount);

  }

}
