// Copyright (c) 2025 William David Louth

package io.humainary.serventis.tck.opt.data;

import io.humainary.serventis.opt.data.*;
import io.humainary.serventis.opt.data.Caches.*;
import io.humainary.specs.api.Specs.*;
import io.humainary.substrates.tck.*;
import org.junit.jupiter.api.*;

import static io.humainary.serventis.opt.data.Caches.Sign.*;
import static io.humainary.substrates.api.Substrates.*;
import static org.junit.jupiter.api.Assertions.*;


/// Tests for the [Caches] API.
/// @author William David Louth
/// @since 1.0

@SpecDoc("https://github.com/humainary-io/serventis-api-spec/blob/3.1.2/SPEC.md")
final class CachesContractTest {

  private static final Cortex CORTEX = cortex();
  private static final Name NAME = CORTEX.name("user.sessions");
  private Circuit circuit;
  private CaptureBuffer< Sign > captures;
  private Cache cache;

  @BeforeEach
  void setup() {

    circuit =
      CORTEX.circuit();

    final var conduit =
      circuit.conduit(
        Sign.class
      );

    cache =
      conduit.pool(Caches::of)
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
  void testCacheHitScenario() {

    cache.lookup();
    cache.hit();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(2, signs.size());
    assertEquals(LOOKUP, signs.getFirst());
    assertEquals(HIT, signs.get(1));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testCacheMissScenario() {

    cache.lookup();
    cache.miss();
    cache.store();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(3, signs.size());
    assertEquals(LOOKUP, signs.getFirst());
    assertEquals(MISS, signs.get(1));
    assertEquals(STORE, signs.get(2));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testEvict() {

    cache.evict();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EVICT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testEvictionPattern() {

    cache.store();
    cache.store();
    cache.evict();
    cache.store();
    cache.evict();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(5, signs.size());
    assertEquals(STORE, signs.getFirst());
    assertEquals(STORE, signs.get(1));
    assertEquals(EVICT, signs.get(2));
    assertEquals(STORE, signs.get(3));
    assertEquals(EVICT, signs.get(4));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpirationPattern() {

    cache.store();
    cache.expire();
    cache.store();
    cache.expire();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(STORE, signs.getFirst());
    assertEquals(EXPIRE, signs.get(1));
    assertEquals(STORE, signs.get(2));
    assertEquals(EXPIRE, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testExpire() {

    cache.expire();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(EXPIRE, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testHit() {

    cache.hit();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(HIT, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testLookup() {

    cache.lookup();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(LOOKUP, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMiss() {

    cache.miss();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(MISS, signs.getFirst());

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMixedCacheOperations() {

    cache.lookup();
    cache.hit();
    cache.lookup();
    cache.miss();
    cache.store();
    cache.lookup();
    cache.hit();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(LOOKUP, signs.getFirst());
    assertEquals(HIT, signs.get(1));
    assertEquals(LOOKUP, signs.get(2));
    assertEquals(MISS, signs.get(3));
    assertEquals(STORE, signs.get(4));
    assertEquals(LOOKUP, signs.get(5));
    assertEquals(HIT, signs.get(6));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testMixedRemovalTypes() {

    cache.store();
    cache.evict();
    cache.store();
    cache.expire();
    cache.store();
    cache.remove();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(6, signs.size());
    assertEquals(STORE, signs.getFirst());
    assertEquals(EVICT, signs.get(1));
    assertEquals(STORE, signs.get(2));
    assertEquals(EXPIRE, signs.get(3));
    assertEquals(STORE, signs.get(4));
    assertEquals(REMOVE, signs.get(5));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRemovalPattern() {

    cache.store();
    cache.remove();
    cache.store();
    cache.remove();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(4, signs.size());
    assertEquals(STORE, signs.getFirst());
    assertEquals(REMOVE, signs.get(1));
    assertEquals(STORE, signs.get(2));
    assertEquals(REMOVE, signs.get(3));

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testRemove() {

    cache.remove();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(REMOVE, signs.getFirst());

  }

  @SpecRef("6.1")
  @Test
  void testSign() {

    // Test direct sign() method for all sign values
    cache.sign(LOOKUP);
    cache.sign(HIT);
    cache.sign(MISS);
    cache.sign(STORE);
    cache.sign(EVICT);
    cache.sign(EXPIRE);
    cache.sign(REMOVE);

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(7, signs.size());
    assertEquals(LOOKUP, signs.get(0));
    assertEquals(HIT, signs.get(1));
    assertEquals(MISS, signs.get(2));
    assertEquals(STORE, signs.get(3));
    assertEquals(EVICT, signs.get(4));
    assertEquals(EXPIRE, signs.get(5));
    assertEquals(REMOVE, signs.get(6));

  }

  @SpecRef({"4.1", "registry:caches"})
  @Test
  void testSignEnumOrdinals() {

    // Ensure ordinals remain stable for compatibility
    assertEquals(0, LOOKUP.ordinal());
    assertEquals(1, HIT.ordinal());
    assertEquals(2, MISS.ordinal());
    assertEquals(3, STORE.ordinal());
    assertEquals(4, EVICT.ordinal());
    assertEquals(5, EXPIRE.ordinal());
    assertEquals(6, REMOVE.ordinal());

  }

  @SpecRef({"4.5", "registry:caches"})
  @Test
  void testSignEnumValues() {

    final var values = Sign.values();

    assertEquals(7, values.length);
    assertEquals(LOOKUP, values[0]);
    assertEquals(HIT, values[1]);
    assertEquals(MISS, values[2]);
    assertEquals(STORE, values[3]);
    assertEquals(EVICT, values[4]);
    assertEquals(EXPIRE, values[5]);
    assertEquals(REMOVE, values[6]);

  }

  @SpecRef({"6.3", "6.5"})
  @Test
  void testStore() {

    cache.store();

    final var signs =
      captures
        .drainEmissions()
        .toList();

    assertEquals(1, signs.size());
    assertEquals(STORE, signs.getFirst());

  }

  @SpecRef("6.4")
  @Test
  void testSubjectAttachment() {

    cache.lookup();

    final var capture =
      captures
        .drain()
        .findFirst()
        .orElseThrow();

    assertEquals(NAME, capture.subject().name());
    assertEquals(LOOKUP, capture.emission());

  }

}
