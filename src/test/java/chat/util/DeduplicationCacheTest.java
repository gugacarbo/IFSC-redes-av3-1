package chat.util;

import chat.model.ChatMessage;
import chat.model.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationCacheTest {

    private DeduplicationCache cache;

    @BeforeEach
    void setUp() {
        cache = new DeduplicationCache();
    }

    @AfterEach
    void tearDown() {
        cache.shutdown();
    }

    @Test
    void testIsDuplicate_NewMessage_ReturnsFalse() {
        ChatMessage message = new ChatMessage("user1", "Hello", MessageType.CHAT);
        
        assertFalse(cache.isDuplicate(message));
    }

    @Test
    void testAdd_ThenIsDuplicate_ReturnsTrue() {
        ChatMessage message = new ChatMessage("user1", "Hello", MessageType.CHAT);
        
        cache.add(message);
        
        assertTrue(cache.isDuplicate(message));
    }

    @Test
    void testIsDuplicate_DifferentMessages_ReturnsFalse() {
        ChatMessage message1 = new ChatMessage("user1", "Hello", MessageType.CHAT);
        ChatMessage message2 = new ChatMessage("user2", "World", MessageType.CHAT);
        
        cache.add(message1);
        
        assertFalse(cache.isDuplicate(message2));
    }

    @Test
    void testDuplicateDetection_SameMsgId_ReturnsTrue() {
        String fixedMsgId = "724a5229-f733-4a9f-a6e4-83ccee271768";
        ChatMessage message = new ChatMessage("25/03/2026", "10:00:00", "user1", "Hello", MessageType.CHAT, fixedMsgId);
        
        cache.add(message);
        
        ChatMessage duplicateMessage = new ChatMessage("25/03/2026", "10:00:00", "user1", "Hello", MessageType.CHAT, fixedMsgId);
        assertEquals(fixedMsgId, duplicateMessage.getMsgId());
        assertTrue(cache.isDuplicate(duplicateMessage));
    }

    @Test
    void testClear_RemovesAllEntries() {
        ChatMessage message1 = new ChatMessage("user1", "Hello", MessageType.CHAT);
        ChatMessage message2 = new ChatMessage("user2", "World", MessageType.CHAT);
        
        cache.add(message1);
        cache.add(message2);
        
        cache.clear();
        
        assertFalse(cache.isDuplicate(message1));
        assertFalse(cache.isDuplicate(message2));
        assertEquals(0, cache.size());
    }

    @Test
    void testSize_TracksCorrectly() {
        assertEquals(0, cache.size());
        
        ChatMessage message1 = new ChatMessage("user1", "Hello", MessageType.CHAT);
        cache.add(message1);
        assertEquals(1, cache.size());
        
        ChatMessage message2 = new ChatMessage("user2", "World", MessageType.CHAT);
        cache.add(message2);
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void testIsDuplicate_EmptyCache_ReturnsFalse() {
        ChatMessage message = new ChatMessage("user1", "Hello", MessageType.CHAT);
        
        assertFalse(cache.isDuplicate(message));
        assertEquals(0, cache.size());
    }
}
