package chat.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThreadSafeListTest {

  @Test
  void testAdd() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test");
    assertEquals(1, list.size());
    assertEquals("test", list.get(0));
  }

  @Test
  void testRemove() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test");
    assertTrue(list.remove("test"));
    assertTrue(list.isEmpty());
  }

  @Test
  void testGet() {
    ThreadSafeList<Integer> list = new ThreadSafeList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    assertEquals(2, list.get(1));
    assertNull(list.get(10));
    assertNull(list.get(-1));
  }

  @Test
  void testContains() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test");
    assertTrue(list.contains("test"));
    assertFalse(list.contains("other"));
  }

  @Test
  void testClear() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test1");
    list.add("test2");
    list.clear();
    assertTrue(list.isEmpty());
  }

  @Test
  void testToList() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test1");
    list.add("test2");
    List<String> copied = list.toList();
    assertEquals(2, copied.size());
    assertTrue(copied.contains("test1"));
    assertTrue(copied.contains("test2"));
  }

  @Test
  void testAddAll() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    List<String> toAdd = Arrays.asList("a", "b", "c");
    assertTrue(list.addAll(toAdd));
    assertEquals(3, list.size());
  }

  @Test
  void testIterator() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("test");
    int count = 0;
    for (String s : list) {
      count++;
      assertEquals("test", s);
    }
    assertEquals(1, count);
  }

  @Test
  void testFilter() {
    ThreadSafeList<Integer> list = new ThreadSafeList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    List<Integer> evens = list.filter(n -> n % 2 == 0);
    assertEquals(2, evens.size());
    assertTrue(evens.contains(2));
    assertTrue(evens.contains(4));
  }

  @Test
  void testFindFirst() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("apple");
    list.add("banana");
    list.add("cherry");
    String result = list.findFirst(s -> s.startsWith("b"));
    assertEquals("banana", result);
  }

  @Test
  void testFindFirstNoMatch() {
    ThreadSafeList<String> list = new ThreadSafeList<>();
    list.add("apple");
    list.add("banana");
    String result = list.findFirst(s -> s.startsWith("z"));
    assertNull(result);
  }

  @Test
  void testRemoveIf() {
    ThreadSafeList<Integer> list = new ThreadSafeList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.removeIf(n -> n > 1);
    assertEquals(1, list.size());
    assertTrue(list.contains(1));
  }

  @Test
  void testConstructorWithInitialData() {
    List<String> initial = Arrays.asList("a", "b", "c");
    ThreadSafeList<String> list = new ThreadSafeList<>(initial);
    assertEquals(3, list.size());
  }
}
