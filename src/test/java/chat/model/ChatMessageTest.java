package chat.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChatMessageTest {

  @Test
  void testChatMessageCreation() {
    ChatMessage msg = new ChatMessage("Alice", "Hello world", MessageType.CHAT);

    assertEquals("Alice", msg.getUsername());
    assertEquals("Hello world", msg.getContent());
    assertEquals(MessageType.CHAT, msg.getType());
    assertNotNull(msg.getDate());
    assertNotNull(msg.getTime());
    assertNotNull(msg.getMsgId());
  }

  @Test
  void testMsgIdIsUnique() {
    ChatMessage msg1 = new ChatMessage("Alice", "Hello", MessageType.CHAT);
    ChatMessage msg2 = new ChatMessage("Alice", "Hello", MessageType.CHAT);

    assertNotEquals(msg1.getMsgId(), msg2.getMsgId());
  }

  @Test
  void testGetUniqueKey() {
    ChatMessage msg = new ChatMessage("Alice", "Hello", MessageType.CHAT);

    assertEquals(msg.getMsgId(), msg.getUniqueKey());
  }

  @Test
  void testMessageTypeEnum() {
    assertEquals(6, MessageType.values().length);
    assertNotNull(MessageType.valueOf("CHAT"));
    assertNotNull(MessageType.valueOf("JOIN"));
    assertNotNull(MessageType.valueOf("LEAVE"));
    assertNotNull(MessageType.valueOf("PING"));
    assertNotNull(MessageType.valueOf("PONG"));
    assertNotNull(MessageType.valueOf("ACK"));
  }
}
