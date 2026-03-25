package chat.network;

import chat.model.ChatMessage;
import chat.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PendingRequestTrackerTest {

    private PendingRequestTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new PendingRequestTracker();
    }

    @Test
    void testTrackJoinRequest() {
        ChatMessage joinMessage = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMessage);
        
        assertEquals(1, tracker.getPendingCount());
        assertTrue(tracker.hasPendingRequest(joinMessage.getMsgId()));
    }

    @Test
    void testTrackPingRequest() {
        ChatMessage pingMessage = new ChatMessage("user1", "", MessageType.PING);
        tracker.trackRequest(pingMessage);
        
        assertEquals(1, tracker.getPendingCount());
        assertTrue(tracker.hasPendingRequest(pingMessage.getMsgId()));
    }

    @Test
    void testChatMessageNotTracked() {
        ChatMessage chatMessage = new ChatMessage("user1", "Hello", MessageType.CHAT);
        tracker.trackRequest(chatMessage);
        
        assertEquals(0, tracker.getPendingCount());
        assertFalse(tracker.hasPendingRequest(chatMessage.getMsgId()));
    }

    @Test
    void testAckMessageRemovesPendingRequest() {
        ChatMessage joinMessage = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMessage);
        
        assertEquals(1, tracker.getPendingCount());
        
        ChatMessage ackMessage = new ChatMessage("user2", "", MessageType.ACK, joinMessage.getMsgId());
        tracker.handleAckMessage(ackMessage);
        
        assertEquals(0, tracker.getPendingCount());
        assertFalse(tracker.hasPendingRequest(joinMessage.getMsgId()));
    }

    @Test
    void testAckWithNullOriginalMsgId() {
        ChatMessage joinMessage = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMessage);
        
        ChatMessage ackMessage = new ChatMessage("user2", "", MessageType.ACK);
        tracker.handleAckMessage(ackMessage);
        
        assertEquals(1, tracker.getPendingCount());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testCleanupExpiredRequests() throws InterruptedException {
        ChatMessage joinMessage = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMessage);
        
        assertEquals(1, tracker.getPendingCount());
        
        Thread.sleep(16000);
        
        assertEquals(0, tracker.getPendingCount());
    }

    @Test
    void testMessageSenderCallback() {
        final int[] sendCount = {0};
        tracker.setMessageSender(msg -> sendCount[0]++);
        
        ChatMessage joinMessage = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMessage);
    }

    @Test
    void testShutdown() {
        tracker.shutdown();
    }

    @Test
    void testMultiplePendingRequests() {
        ChatMessage join1 = new ChatMessage("user1", "", MessageType.JOIN);
        ChatMessage join2 = new ChatMessage("user2", "", MessageType.JOIN);
        ChatMessage ping = new ChatMessage("user1", "", MessageType.PING);
        
        tracker.trackRequest(join1);
        tracker.trackRequest(join2);
        tracker.trackRequest(ping);
        
        assertEquals(3, tracker.getPendingCount());
        
        ChatMessage ack1 = new ChatMessage("user2", "", MessageType.ACK, join1.getMsgId());
        tracker.handleAckMessage(ack1);
        
        assertEquals(2, tracker.getPendingCount());
    }

    @Test
    void testAckForNonExistentRequest() {
        ChatMessage ackMessage = new ChatMessage("user2", "", MessageType.ACK, "non-existent-id");
        tracker.handleAckMessage(ackMessage);
        
        assertEquals(0, tracker.getPendingCount());
    }
}
