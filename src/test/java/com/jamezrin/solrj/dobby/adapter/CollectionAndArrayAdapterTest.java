package com.jamezrin.solrj.dobby.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeToken;

class CollectionAndArrayAdapterTest {

  private Dobby dobby;

  @BeforeEach
  void setUp() {
    dobby = Dobby.builder().build();
  }

  @Nested
  class ListAdapterTests {
    @Test
    void readsListOfStrings() {
      TypeAdapter<List<String>> adapter = dobby.getAdapter(new TypeToken<List<String>>() {});
      List<String> result = adapter.read(List.of("a", "b", "c"));
      assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void readsSingleValueAsList() {
      TypeAdapter<List<String>> adapter = dobby.getAdapter(new TypeToken<List<String>>() {});
      List<String> result = adapter.read("single");
      assertEquals(List.of("single"), result);
    }

    @Test
    void readsListWithTypeCoercion() {
      TypeAdapter<List<Integer>> adapter = dobby.getAdapter(new TypeToken<List<Integer>>() {});
      List<Integer> result = adapter.read(List.of(1L, 2L, 3L));
      assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void writesListOfStrings() {
      TypeAdapter<List<String>> adapter = dobby.getAdapter(new TypeToken<List<String>>() {});
      Object result = adapter.write(List.of("x", "y"));
      assertInstanceOf(List.class, result);
      assertEquals(List.of("x", "y"), result);
    }

    @Test
    void nullReturnsNull() {
      TypeAdapter<List<String>> adapter = dobby.getAdapter(new TypeToken<List<String>>() {});
      assertNull(adapter.read(null));
      assertNull(adapter.write(null));
    }
  }

  @Nested
  class SetAdapterTests {
    @Test
    void readsSetOfStrings() {
      TypeAdapter<Set<String>> adapter = dobby.getAdapter(new TypeToken<Set<String>>() {});
      Set<String> result = adapter.read(List.of("a", "b", "a"));
      assertEquals(Set.of("a", "b"), result);
    }

    @Test
    void writesSetToList() {
      TypeAdapter<Set<String>> adapter = dobby.getAdapter(new TypeToken<Set<String>>() {});
      Object result = adapter.write(Set.of("x", "y"));
      assertInstanceOf(List.class, result);
    }
  }

  @Nested
  class ArrayAdapterTests {
    @Test
    void readsStringArray() {
      TypeAdapter<String[]> adapter = dobby.getAdapter(new TypeToken<String[]>() {});
      String[] result = adapter.read(List.of("a", "b"));
      assertArrayEquals(new String[] {"a", "b"}, result);
    }

    @Test
    void readsIntegerArray() {
      TypeAdapter<Integer[]> adapter = dobby.getAdapter(new TypeToken<Integer[]>() {});
      Integer[] result = adapter.read(List.of(1, 2, 3));
      assertArrayEquals(new Integer[] {1, 2, 3}, result);
    }

    @Test
    void readsSingleValueAsArray() {
      TypeAdapter<String[]> adapter = dobby.getAdapter(new TypeToken<String[]>() {});
      String[] result = adapter.read("solo");
      assertArrayEquals(new String[] {"solo"}, result);
    }

    @Test
    void writesArrayToList() {
      TypeAdapter<String[]> adapter = dobby.getAdapter(new TypeToken<String[]>() {});
      Object result = adapter.write(new String[] {"a", "b"});
      assertInstanceOf(List.class, result);
      assertEquals(List.of("a", "b"), result);
    }

    @Test
    void nullReturnsNull() {
      TypeAdapter<String[]> adapter = dobby.getAdapter(new TypeToken<String[]>() {});
      assertNull(adapter.read(null));
      assertNull(adapter.write(null));
    }
  }

  @Nested
  class MapAdapterTests {
    @Test
    void readsMapOfStringToString() {
      TypeAdapter<Map<String, String>> adapter =
          dobby.getAdapter(new TypeToken<Map<String, String>>() {});
      Map<String, Object> input = new LinkedHashMap<>();
      input.put("key1", "val1");
      input.put("key2", "val2");
      Map<String, String> result = adapter.read(input);
      assertEquals("val1", result.get("key1"));
      assertEquals("val2", result.get("key2"));
    }

    @Test
    void writesMap() {
      TypeAdapter<Map<String, Integer>> adapter =
          dobby.getAdapter(new TypeToken<Map<String, Integer>>() {});
      Map<String, Integer> input = new LinkedHashMap<>();
      input.put("a", 1);
      input.put("b", 2);
      Object result = adapter.write(input);
      assertInstanceOf(Map.class, result);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) result;
      assertEquals(1, map.get("a"));
      assertEquals(2, map.get("b"));
    }
  }

  @Nested
  class OptionalAdapterTests {
    @Test
    void readsNullAsEmpty() {
      TypeAdapter<Optional<String>> adapter =
          dobby.getAdapter(new TypeToken<Optional<String>>() {});
      assertEquals(Optional.empty(), adapter.read(null));
    }

    @Test
    void readsValueAsPresent() {
      TypeAdapter<Optional<String>> adapter =
          dobby.getAdapter(new TypeToken<Optional<String>>() {});
      assertEquals(Optional.of("hello"), adapter.read("hello"));
    }

    @Test
    void writesEmptyAsNull() {
      TypeAdapter<Optional<String>> adapter =
          dobby.getAdapter(new TypeToken<Optional<String>>() {});
      assertNull(adapter.write(Optional.empty()));
    }

    @Test
    void writesPresentValue() {
      TypeAdapter<Optional<String>> adapter =
          dobby.getAdapter(new TypeToken<Optional<String>>() {});
      assertEquals("hello", adapter.write(Optional.of("hello")));
    }

    @Test
    void writesNullOptionalAsNull() {
      TypeAdapter<Optional<String>> adapter =
          dobby.getAdapter(new TypeToken<Optional<String>>() {});
      assertNull(adapter.write(null));
    }
  }
}
