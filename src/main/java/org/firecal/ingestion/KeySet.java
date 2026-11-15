package org.firecal.ingestion;

import java.util.Arrays;
import org.firecal.domain.Hotspot;

/**
 * Insieme compatto di chiavi di deduplica (satellite, data, ora, lat e lon a 5 decimali) per un
 * singolo file. Indirizzamento aperto su due {@code long} per chiave, senza oggetti: un file da 4
 * milioni di righe usa ~130 MB invece di mezzo gigabyte di {@code HashSet}.
 *
 * <p>La chiave e' esatta (nessuna collisione tollerata): 4 bit di satellite, 16 di giorno e 12 di
 * ora in un {@code long}; latitudine e longitudine (spostate in positivo) nell'altro.
 */
public final class KeySet {

  private static final long EMPTY = -1L;

  private long[] first;
  private long[] second;
  private int size;
  private int mask;

  public KeySet(int expected) {
    int capacity = Integer.highestOneBit(Math.max(16, expected) * 2 - 1) << 1;
    allocate(capacity);
  }

  private void allocate(int capacity) {
    first = new long[capacity];
    second = new long[capacity];
    Arrays.fill(first, EMPTY);
    mask = capacity - 1;
  }

  /**
   * @return {@code true} se la chiave non c'era (e ora c'e'), {@code false} se era un duplicato
   */
  public boolean add(Hotspot hotspot) {
    Hotspot.Key key = hotspot.key();
    long k1 = ((long) (hotspot.satellite().ordinal() + 1) << 28)
        | (hotspot.acqDate().toEpochDay() << 12)
        | hotspot.acqTime();
    long k2 = ((key.latUnits() + 9_000_000L) << 26) | (key.lonUnits() + 18_000_000L);
    return add(k1, k2);
  }

  boolean add(long k1, long k2) {
    if (size * 2 >= first.length) {
      grow();
    }
    int slot = hash(k1, k2) & mask;
    while (first[slot] != EMPTY) {
      if (first[slot] == k1 && second[slot] == k2) {
        return false;
      }
      slot = (slot + 1) & mask;
    }
    first[slot] = k1;
    second[slot] = k2;
    size++;
    return true;
  }

  public int size() {
    return size;
  }

  private void grow() {
    long[] oldFirst = first;
    long[] oldSecond = second;
    allocate(oldFirst.length * 2);
    size = 0;
    for (int i = 0; i < oldFirst.length; i++) {
      if (oldFirst[i] != EMPTY) {
        add(oldFirst[i], oldSecond[i]);
      }
    }
  }

  private static int hash(long k1, long k2) {
    long h = k1 * 0x9E3779B97F4A7C15L ^ (k2 + 0x632BE59BD9B4E019L) * 0xC2B2AE3D27D4EB4FL;
    h ^= h >>> 32;
    return (int) h;
  }
}
