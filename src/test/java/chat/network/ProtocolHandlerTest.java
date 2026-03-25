package chat.network;

import chat.controller.ChatController;
import chat.model.ChatMessage;
import chat.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProtocolHandlerTest {

    private ProtocolHandler protocolHandler;

    @Mock
    private ChatController chatController;

    @Mock
    private PeerDiscovery peerDiscovery;

    @Mock
    private LivenessMonitor livenessMonitor;

    @Mock
    private PendingRequestTracker pendingRequestTracker;

    private InetAddress localAddress;
    private InetAddress remoteAddress;

    @BeforeEach
    void setUp() throws Exception {
        protocolHandler = new ProtocolHandler();
        protocolHandler.setChatController(chatController);
        protocolHandler.setPeerDiscovery(peerDiscovery);
        protocolHandler.setLivenessMonitor(livenessMonitor);
        protocolHandler.setPendingRequestTracker(pendingRequestTracker);

        localAddress = InetAddress.getByName("127.0.0.1");
        remoteAddress = InetAddress.getByName("192.168.1.100");
    }

    @Test
    void testChatMessageRouting() throws Exception {
        ChatMessage chatMessage = new ChatMessage(
            "user1", "Hello", MessageType.CHAT
        );
        String json = chatMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
        verify(peerDiscovery, never()).handleJoinMessage(any(ChatMessage.class));
        verify(peerDiscovery, never()).handleLeaveMessage(any(ChatMessage.class));
        verify(livenessMonitor, never()).handlePingMessage(any(ChatMessage.class));
        verify(livenessMonitor, never()).handlePongMessage(any(ChatMessage.class));
        verify(pendingRequestTracker, never()).handleAckMessage(any(ChatMessage.class));
    }

    @Test
    void testJoinMessageRouting() throws Exception {
        ChatMessage joinMessage = new ChatMessage(
            "user1", "", MessageType.JOIN
        );
        String json = joinMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(peerDiscovery, times(1)).handleJoinMessage(any(ChatMessage.class));
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testLeaveMessageRouting() throws Exception {
        ChatMessage leaveMessage = new ChatMessage(
            "user1", "", MessageType.LEAVE
        );
        String json = leaveMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(peerDiscovery, times(1)).handleLeaveMessage(any(ChatMessage.class));
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testPingMessageRouting() throws Exception {
        ChatMessage pingMessage = new ChatMessage(
            "user1", "", MessageType.PING
        );
        String json = pingMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(livenessMonitor, times(1)).handlePingMessage(any(ChatMessage.class));
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testPongMessageRouting() throws Exception {
        ChatMessage pongMessage = new ChatMessage(
            "user1", "", MessageType.PONG
        );
        String json = pongMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(livenessMonitor, times(1)).handlePongMessage(any(ChatMessage.class));
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testAckMessageRouting() throws Exception {
        ChatMessage ackMessage = new ChatMessage(
            "user1", "", MessageType.ACK
        );
        String json = ackMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(pendingRequestTracker, times(1)).handleAckMessage(any(ChatMessage.class));
        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testSelfMessageFiltering() throws Exception {
        ChatMessage chatMessage = new ChatMessage(
            "user1", "Hello", MessageType.CHAT
        );
        String json = chatMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, localAddress, 5000);

        protocolHandler.setOwnCredentials("user1", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testSelfMessageFilteringWithDifferentAddress() throws Exception {
        ChatMessage chatMessage = new ChatMessage(
            "user1", "Hello", MessageType.CHAT
        );
        String json = chatMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user1", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testSelfMessageFilteringWithDifferentPort() throws Exception {
        ChatMessage chatMessage = new ChatMessage(
            "user1", "Hello", MessageType.CHAT
        );
        String json = chatMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, localAddress, 6000);

        protocolHandler.setOwnCredentials("user1", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testInvalidJsonHandling() throws Exception {
        String invalidJson = "{ invalid json }";
        byte[] data = invalidJson.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
        verify(peerDiscovery, never()).handleJoinMessage(any(ChatMessage.class));
        verify(livenessMonitor, never()).handlePingMessage(any(ChatMessage.class));
    }

    @Test
    void testNullMessageHandling() throws Exception {
        String json = "null";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testMessageWithNullType() throws Exception {
        String json = "{\"date\":\"25/03/2026\",\"time\":\"10:00:00\",\"username\":\"user1\",\"message\":\"test\",\"type\":null,\"msgId\":\"uuid-123\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testMessageWithNullUsername() throws Exception {
        String json = "{\"date\":\"25/03/2026\",\"time\":\"10:00:00\",\"username\":null,\"message\":\"test\",\"type\":\"CHAT\",\"msgId\":\"uuid-123\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testMessageWithEmptyMsgId() throws Exception {
        String json = "{\"date\":\"25/03/2026\",\"time\":\"10:00:00\",\"username\":\"user1\",\"message\":\"test\",\"type\":\"CHAT\",\"msgId\":\"\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        protocolHandler.setOwnCredentials("user2", localAddress, 5000);
        protocolHandler.process(packet);

        verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
    }

    @Test
    void testAllMessageTypesProcessed() throws Exception {
        MessageType[] types = {MessageType.CHAT, MessageType.JOIN, MessageType.LEAVE, 
                                MessageType.PING, MessageType.PONG, MessageType.ACK};

        for (MessageType type : types) {
            ChatMessage message = new ChatMessage("user1", "test", type);
            String json = message.toJson();
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

            protocolHandler.setOwnCredentials("user2", localAddress, 5000);
            protocolHandler.process(packet);
        }

        verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
        verify(peerDiscovery, times(1)).handleJoinMessage(any(ChatMessage.class));
        verify(peerDiscovery, times(1)).handleLeaveMessage(any(ChatMessage.class));
        verify(livenessMonitor, times(1)).handlePingMessage(any(ChatMessage.class));
        verify(livenessMonitor, times(1)).handlePongMessage(any(ChatMessage.class));
        verify(pendingRequestTracker, times(1)).handleAckMessage(any(ChatMessage.class));
    }

    @Test
    void testNullHandlerDoesNotThrow() throws Exception {
        ProtocolHandler handlerWithoutHandlers = new ProtocolHandler();
        
        ChatMessage chatMessage = new ChatMessage(
            "user1", "Hello", MessageType.CHAT
        );
        String json = chatMessage.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

        handlerWithoutHandlers.setOwnCredentials("user2", localAddress, 5000);
        handlerWithoutHandlers.process(packet);
    }
}
