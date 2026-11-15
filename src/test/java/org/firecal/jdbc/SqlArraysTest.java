package org.firecal.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Array;
import org.junit.jupiter.api.Test;

class SqlArraysTest {

  @Test
  void readsAnArrayOfMixedNumberTypesAsLongs() throws Exception {
    Array array = mock(Array.class);
    when(array.getArray()).thenReturn(new Object[] {1L, 2, (short) 3, 4_000_000_000L});
    assertThat(SqlArrays.toLongs(array)).containsExactly(1, 2, 3, 4_000_000_000L);
  }

  @Test
  void anEmptyArrayIsAnEmptyArray() throws Exception {
    Array array = mock(Array.class);
    when(array.getArray()).thenReturn(new Object[0]);
    assertThat(SqlArrays.toLongs(array)).isEmpty();
  }
}
