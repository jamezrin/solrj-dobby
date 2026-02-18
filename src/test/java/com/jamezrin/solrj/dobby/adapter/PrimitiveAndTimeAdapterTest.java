package com.jamezrin.solrj.dobby.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;

class PrimitiveAndTimeAdapterTest {

  private Dobby dobby;

  @BeforeEach
  void setUp() {
    dobby = Dobby.builder().build();
  }

  @Nested
  class StringAdapterTests {
    @Test
    void readsString() {
      assertEquals("hello", dobby.getAdapter(String.class).read("hello"));
    }

    @Test
    void readsNonStringAsToString() {
      assertEquals("42", dobby.getAdapter(String.class).read(42));
    }

    @Test
    void nullReturnsNull() {
      assertNull(dobby.getAdapter(String.class).read(null));
    }

    @Test
    void writesPassthrough() {
      assertEquals("abc", dobby.getAdapter(String.class).write("abc"));
    }
  }

  @Nested
  class IntegerAdapterTests {
    @Test
    void readsInt() {
      assertEquals(42, dobby.getAdapter(Integer.class).read(42));
    }

    @Test
    void readsLongToInt() {
      assertEquals(42, dobby.getAdapter(Integer.class).read(42L));
    }

    @Test
    void readsStringToInt() {
      assertEquals(42, dobby.getAdapter(Integer.class).read("42"));
    }

    @Test
    void primitiveIntSameAsWrapper() {
      assertNotNull(dobby.getAdapter(int.class));
      assertEquals(7, dobby.getAdapter(int.class).read(7));
    }
  }

  @Nested
  class LongAdapterTests {
    @Test
    void readsLong() {
      assertEquals(100L, dobby.getAdapter(Long.class).read(100L));
    }

    @Test
    void readsIntToLong() {
      assertEquals(100L, dobby.getAdapter(Long.class).read(100));
    }

    @Test
    void readsStringToLong() {
      assertEquals(100L, dobby.getAdapter(Long.class).read("100"));
    }
  }

  @Nested
  class FloatAdapterTests {
    @Test
    void readsFloat() {
      assertEquals(1.5f, dobby.getAdapter(Float.class).read(1.5f));
    }

    @Test
    void readsDoubleToFloat() {
      assertEquals(1.5f, dobby.getAdapter(Float.class).read(1.5));
    }

    @Test
    void readsStringToFloat() {
      assertEquals(1.5f, dobby.getAdapter(Float.class).read("1.5"));
    }
  }

  @Nested
  class DoubleAdapterTests {
    @Test
    void readsDouble() {
      assertEquals(3.14, dobby.getAdapter(Double.class).read(3.14));
    }

    @Test
    void readsIntToDouble() {
      assertEquals(3.0, dobby.getAdapter(Double.class).read(3));
    }
  }

  @Nested
  class BooleanAdapterTests {
    @Test
    void readsBoolean() {
      assertEquals(true, dobby.getAdapter(Boolean.class).read(true));
    }

    @Test
    void readsStringToBoolean() {
      assertEquals(true, dobby.getAdapter(Boolean.class).read("true"));
    }

    @Test
    void readsNumberToBoolean() {
      assertEquals(true, dobby.getAdapter(Boolean.class).read(1));
      assertEquals(false, dobby.getAdapter(Boolean.class).read(0));
    }
  }

  @Nested
  class ByteArrayAdapterTests {
    @Test
    void readsByteArray() {
      byte[] data = {1, 2, 3};
      assertArrayEquals(data, dobby.getAdapter(byte[].class).read(data));
    }

    @Test
    void readsByteBufferToByteArray() {
      byte[] data = {4, 5, 6};
      ByteBuffer bb = ByteBuffer.wrap(data);
      assertArrayEquals(data, dobby.getAdapter(byte[].class).read(bb));
    }
  }

  @Nested
  class ByteBufferAdapterTests {
    @Test
    void readsByteBuffer() {
      ByteBuffer bb = ByteBuffer.wrap(new byte[] {1, 2});
      assertEquals(bb, dobby.getAdapter(ByteBuffer.class).read(bb));
    }

    @Test
    void readsByteArrayToByteBuffer() {
      byte[] data = {7, 8};
      ByteBuffer result = dobby.getAdapter(ByteBuffer.class).read(data);
      assertArrayEquals(data, result.array());
    }
  }

  @Nested
  class InstantAdapterTests {
    @Test
    void readsFromDate() {
      Date d = new Date(1000L);
      Instant result = dobby.getAdapter(Instant.class).read(d);
      assertEquals(Instant.ofEpochMilli(1000L), result);
    }

    @Test
    void readsFromString() {
      Instant result = dobby.getAdapter(Instant.class).read("2023-01-15T10:30:00Z");
      assertEquals(Instant.parse("2023-01-15T10:30:00Z"), result);
    }

    @Test
    void readsFromEpochMillis() {
      Instant result = dobby.getAdapter(Instant.class).read(5000L);
      assertEquals(Instant.ofEpochMilli(5000L), result);
    }

    @Test
    void writesToDate() {
      Instant instant = Instant.parse("2023-06-01T00:00:00Z");
      Object result = dobby.getAdapter(Instant.class).write(instant);
      assertInstanceOf(Date.class, result);
      assertEquals(instant, ((Date) result).toInstant());
    }
  }

  @Nested
  class LocalDateAdapterTests {
    @Test
    void readsFromDate() {
      Instant instant = LocalDate.of(2023, 6, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
      Date d = Date.from(instant);
      LocalDate result = dobby.getAdapter(LocalDate.class).read(d);
      assertEquals(LocalDate.of(2023, 6, 15), result);
    }

    @Test
    void readsFromString() {
      LocalDate result = dobby.getAdapter(LocalDate.class).read("2023-06-15");
      assertEquals(LocalDate.of(2023, 6, 15), result);
    }

    @Test
    void writesToDate() {
      Object result = dobby.getAdapter(LocalDate.class).write(LocalDate.of(2023, 6, 15));
      assertInstanceOf(Date.class, result);
    }
  }

  @Nested
  class LocalDateTimeAdapterTests {
    @Test
    void readsFromDate() {
      LocalDateTime ldt = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
      Date d = Date.from(ldt.toInstant(ZoneOffset.UTC));
      LocalDateTime result = dobby.getAdapter(LocalDateTime.class).read(d);
      assertEquals(ldt, result);
    }

    @Test
    void readsFromString() {
      LocalDateTime result = dobby.getAdapter(LocalDateTime.class).read("2023-06-15T10:30:00");
      assertEquals(LocalDateTime.of(2023, 6, 15, 10, 30, 0), result);
    }
  }

  @Nested
  class ZonedDateTimeAdapterTests {
    @Test
    void readsFromDate() {
      ZonedDateTime zdt = ZonedDateTime.of(2023, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
      Date d = Date.from(zdt.toInstant());
      ZonedDateTime result = dobby.getAdapter(ZonedDateTime.class).read(d);
      assertEquals(zdt, result);
    }
  }

  @Nested
  class DateAdapterTests {
    @Test
    void readsDate() {
      Date d = new Date();
      assertEquals(d, dobby.getAdapter(Date.class).read(d));
    }

    @Test
    void readsFromString() {
      Date result = dobby.getAdapter(Date.class).read("2023-06-15T00:00:00Z");
      assertNotNull(result);
    }

    @Test
    void readsFromEpochMillis() {
      Date result = dobby.getAdapter(Date.class).read(5000L);
      assertEquals(new Date(5000L), result);
    }

    @Test
    void writesPassthrough() {
      Date d = new Date();
      assertSame(d, dobby.getAdapter(Date.class).write(d));
    }
  }

  @Nested
  class EnumAdapterTests {
    enum Color {
      RED,
      GREEN,
      BLUE
    }

    @Test
    void readsFromName() {
      assertEquals(Color.RED, dobby.getAdapter(Color.class).read("RED"));
    }

    @Test
    void readsFromOrdinal() {
      assertEquals(Color.GREEN, dobby.getAdapter(Color.class).read(1));
    }

    @Test
    void writesToName() {
      assertEquals("BLUE", dobby.getAdapter(Color.class).write(Color.BLUE));
    }

    @Test
    void nullReturnsNull() {
      assertNull(dobby.getAdapter(Color.class).read(null));
      assertNull(dobby.getAdapter(Color.class).write(null));
    }

    @Test
    void invalidNameThrows() {
      assertThrows(DobbyException.class, () -> dobby.getAdapter(Color.class).read("PURPLE"));
    }

    @Test
    void invalidOrdinalThrows() {
      assertThrows(DobbyException.class, () -> dobby.getAdapter(Color.class).read(99));
    }
  }
}
