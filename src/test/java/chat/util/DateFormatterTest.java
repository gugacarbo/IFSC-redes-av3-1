package chat.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DateFormatterTest {

  @Test
  void testFormatDate() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    String formatted = DateFormatter.formatDate(date);
    assertEquals("15/03/2024", formatted);
  }

  @Test
  void testFormatDateWithNull() {
    assertEquals("", DateFormatter.formatDate(null));
  }

  @Test
  void testFormatTime() {
    LocalTime time = LocalTime.of(14, 30, 45);
    String formatted = DateFormatter.formatTime(time);
    assertEquals("14:30:45", formatted);
  }

  @Test
  void testFormatTimeWithNull() {
    assertEquals("", DateFormatter.formatTime(null));
  }

  @Test
  void testFormatDateTime() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
    String formatted = DateFormatter.formatDateTime(dateTime);
    assertEquals("15/03/2024 14:30:45", formatted);
  }

  @Test
  void testFormatDateTimeWithNull() {
    assertEquals("", DateFormatter.formatDateTime(null));
  }

  @Test
  void testParseDate() {
    LocalDate date = DateFormatter.parseDate("15/03/2024");
    assertEquals(LocalDate.of(2024, 3, 15), date);
  }

  @Test
  void testParseDateWithNull() {
    assertNull(DateFormatter.parseDate(null));
  }

  @Test
  void testParseDateWithEmpty() {
    assertNull(DateFormatter.parseDate(""));
  }

  @Test
  void testParseTime() {
    LocalTime time = DateFormatter.parseTime("14:30:45");
    assertEquals(LocalTime.of(14, 30, 45), time);
  }

  @Test
  void testParseTimeWithNull() {
    assertNull(DateFormatter.parseTime(null));
  }

  @Test
  void testParseDateTime() {
    LocalDateTime dateTime = DateFormatter.parseDateTime("15/03/2024 14:30:45");
    assertEquals(LocalDateTime.of(2024, 3, 15, 14, 30, 45), dateTime);
  }

  @Test
  void testParseDateTimeWithNull() {
    assertNull(DateFormatter.parseDateTime(null));
  }

  @Test
  void testRoundTripDate() {
    LocalDate original = LocalDate.of(2024, 3, 15);
    String formatted = DateFormatter.formatDate(original);
    LocalDate parsed = DateFormatter.parseDate(formatted);
    assertEquals(original, parsed);
  }

  @Test
  void testRoundTripTime() {
    LocalTime original = LocalTime.of(14, 30, 45);
    String formatted = DateFormatter.formatTime(original);
    LocalTime parsed = DateFormatter.parseTime(formatted);
    assertEquals(original, parsed);
  }

  @Test
  void testRoundTripDateTime() {
    LocalDateTime original = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
    String formatted = DateFormatter.formatDateTime(original);
    LocalDateTime parsed = DateFormatter.parseDateTime(formatted);
    assertEquals(original, parsed);
  }
}
