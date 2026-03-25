package chat.network;

import chat.controller.ChatController;
import chat.model.ChatMessage;
import chat.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtocolHandlerTest {

    private ProtocolHandler handler;
    private PendingRequestTracker tracker;
    private ChatController chatController;
    private PeerDiscovery peerDiscovery;
    private LivenessMonitor livenessMonitor;

    @BeforeEach
    void setUp() {
        handler = new ProtocolHandler();
        tracker = new PendingRequestTracker();
        
        chatController = mock(ChatController.class);
        peerDiscovery = mock(PeerDiscovery.class);
        livenessMonitor = mock(LivenessMonitor.class);
        
        handler.setChatController(chatController);
        handler.setPeerDiscovery(peerDiscovery);
        handler.setLivenessMonitor(livenessMonitor);
        handler.setPendingRequestTracker(tracker);
    }

    @Test
    void testProcessChatMessage() throws Exception {
        ChatMessage chatMsg = new ChatMessage("user1", "Hello", MessageType.CHAT);
        String json = chatMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testProcessJoinMessage() throws Exception {
        ChatMessage joinMsg = new ChatMessage("user1", "", MessageType.JOIN);
        String json = joinMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        verify(peerDiscovery, times(1)).handleJoinMessage(any(ChatMessage.class));
    }

    @Test
    void testProcessLeaveMessage() throws Exception {
        ChatMessage leaveMsg = new ChatMessage("user1", "", MessageType.LEAVE);
        String json = leaveMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        verify(peerDiscovery, times(1)).handleLeaveMessage(any(ChatMessage.class));
    }

    @Test
    void testProcessPingMessage() throws Exception {
        ChatMessage pingMsg = new ChatMessage("user1", "", MessageType.PING);
        String json = pingMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        verify(livenessMonitor, times(1)).handlePingMessage(any(ChatMessage.class));
    }

    @Test
    void testProcessPongMessage() throws Exception {
        ChatMessage pingMsg = new ChatMessage("user1", "", MessageType.PING);
        tracker.trackRequest(pingMsg);
        
        ChatMessage pongMsg = new ChatMessage("user2", "", MessageType.PONG, pingMsg.getMsgId());
        String json = pongMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        verify(livenessMonitor, times(1)).handlePongMessage(any(ChatMessage.class));
        assertEquals(0, tracker.getPendingCount());
    }

    @Test
    void testProcessAckMessage() throws Exception {
        ChatMessage joinMsg = new ChatMessage("user1", "", MessageType.JOIN);
        tracker.trackRequest(joinMsg);
        
        assertEquals(1, tracker.getPendingCount());
        
        ChatMessage ackMsg = new ChatMessage("user2", "", MessageType.ACK, joinMsg.getMsgId());
        String json = ackMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        
        handler.process(packet);
        
        assertEquals(0, tracker.getPendingCount());
    }

    @Test
    void testSelfMessageFiltering() throws Exception {
        InetAddress localAddress = InetAddress.getByName("127.0.0.1");
        handler.setOwnCredentials("user1", localAddress, 5000);
        
        ChatMessage chatMsg = new ChatMessage("user1", "Hello", MessageType.CHAT);
        String json = chatMsg.toJson();
        
        DatagramPacket packet = new DatagramPacket(
            json.getBytes(StandardCharsets.UTF_8),
            json.length()
        );
        packet.setAddress(localAddress);
        packet.setPort(5000);
        
        handler.process(packet);
        
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testInvalidJsonHandling() throws Exception {
        String invalidJson = "{invalid json";
        
        DatagramPacket packet = new DatagramPacket(
            invalidJson.getBytes(StandardCharsets.UTF_8),
            invalidJson.length()
        );
        
        handler.process(packet);
        
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testNullMessageHandling() throws Exception {
        String emptyJson = "{}";
        
        DatagramPacket packet = new DatagramPacket(
            emptyJson.getBytes(StandardCharsets.UTF_8),
            emptyJson.length()
        );
        
        handler.process(packet);
        
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }
}
