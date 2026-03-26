package chat.util;

import static org.junit.jupiter.api.Assertions.*;

import chat.model.ChatMessage;
import chat.model.MessageType;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  @Test
  void testToJsonAndFromJson() {
    ChatMessage original = new ChatMessage("Alice", "Hello world", MessageType.CHAT);
    String json = JsonUtils.toJson(original);

    assertNotNull(json);
    assertTrue(json.contains("Alice"));
    assertTrue(json.contains("Hello world"));
    assertTrue(json.contains("CHAT"));

    ChatMessage parsed = JsonUtils.fromJson(json);

    assertEquals(original.getUsername(), parsed.getUsername());
    assertEquals(original.getContent(), parsed.getContent());
    assertEquals(original.getType(), parsed.getType());
    assertEquals(original.getMsgId(), parsed.getMsgId());
    assertEquals(original.getDate(), parsed.getDate());
    assertEquals(original.getTime(), parsed.getTime());
  }

  @Test
  void testFromJsonWithInvalidJson() {
    assertThrows(
        JsonSyntaxException.class,
        () -> {
          JsonUtils.fromJson("invalid json");
        });
  }

  @Test
  void testFromJsonWithEmptyString() {
    ChatMessage result = JsonUtils.fromJson("");
    assertNull(result);
  }

  @Test
  void testRoundTripPreservesMsgId() {
    ChatMessage original = new ChatMessage("Eve", "Secret message", MessageType.CHAT);
    String json = JsonUtils.toJson(original);
    ChatMessage restored = JsonUtils.fromJson(json);

    assertEquals(original.getMsgId(), restored.getMsgId());
  }

  @Test
  void testAllMessageTypesSerialization() {
    for (MessageType type : MessageType.values()) {
      ChatMessage msg = new ChatMessage("User", "Content", type);
      String json = JsonUtils.toJson(msg);
      ChatMessage parsed = JsonUtils.fromJson(json);

      assertEquals(type, parsed.getType(), "Failed for type: " + type);
    }
  }

  @Test
  void testToWireJsonUsesMinimalLayout() {
    ChatMessage msg = new ChatMessage("Alice", "Hello world", MessageType.CHAT);

    String json = JsonUtils.toWireJson(msg);

    assertTrue(json.contains("\"date\""));
    assertTrue(json.contains("\"time\""));
    assertTrue(json.contains("\"username\""));
    assertTrue(json.contains("\"message\""));
    assertFalse(json.contains("type"));
    assertFalse(json.contains("msgId"));
  }
}
