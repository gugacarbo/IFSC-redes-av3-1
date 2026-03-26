package chat.network;

import static org.mockito.Mockito.*;

import chat.controller.ChatController;
import chat.model.ChatMessage;
import chat.model.MessageType;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProtocolHandlerTest {

  private ProtocolHandler protocolHandler;

  @Mock private ChatController chatController;

  private InetAddress localAddress;
  private InetAddress remoteAddress;

  @BeforeEach
  void setUp() throws Exception {
    protocolHandler = new ProtocolHandler();
    protocolHandler.setChatController(chatController);

    localAddress = InetAddress.getByName("127.0.0.1");
    remoteAddress = InetAddress.getByName("192.168.1.100");
  }

  @Test
  void testValidChatMessageIsDelivered() {
    ChatMessage chatMessage = new ChatMessage("user1", "Hello", MessageType.CHAT);
    byte[] data = chatMessage.toJson().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    protocolHandler.setOwnCredentials("user2", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testMessageWithoutTypeStillDelivered() {
    String json =
        "{\"date\":\"25/03/2026\",\"time\":\"10:00:00\",\"username\":\"user1\",\"message\":\"test\"}";
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    protocolHandler.setOwnCredentials("user2", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testSelfMessageFiltering() {
    ChatMessage chatMessage = new ChatMessage("user1", "Hello", MessageType.CHAT);
    byte[] data = chatMessage.toJson().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, localAddress, 5000);

    protocolHandler.setOwnCredentials("user1", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testSameAddressAndPortWithDifferentUsernameMustBeAccepted() {
    ChatMessage chatMessage = new ChatMessage("other-user", "Hello", MessageType.CHAT);
    byte[] data = chatMessage.toJson().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, localAddress, 5000);

    protocolHandler.setOwnCredentials("user1", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, times(1)).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testInvalidJsonIsDiscarded() {
    String invalidJson = "{ invalid json }";
    byte[] data = invalidJson.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    protocolHandler.setOwnCredentials("user2", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testNullMessageIsDiscarded() {
    String json = "null";
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    protocolHandler.setOwnCredentials("user2", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testMessageWithNullUsernameIsDiscarded() {
    String json =
        "{\"date\":\"25/03/2026\",\"time\":\"10:00:00\",\"username\":null,\"message\":\"test\"}";
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    protocolHandler.setOwnCredentials("user2", localAddress, 5000);
    protocolHandler.process(packet);

    verify(chatController, never()).handleChatMessage(any(ChatMessage.class));
  }

  @Test
  void testNullHandlerDoesNotThrow() {
    ProtocolHandler handlerWithoutHandlers = new ProtocolHandler();
    ChatMessage chatMessage = new ChatMessage("user1", "Hello", MessageType.CHAT);
    byte[] data = chatMessage.toJson().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, 5000);

    handlerWithoutHandlers.setOwnCredentials("user2", localAddress, 5000);
    handlerWithoutHandlers.process(packet);
  }
}
