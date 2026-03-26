package chat.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.util.JsonUtils;
import com.google.gson.JsonSyntaxException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MulticastReceiverTest {

  @Test
  void testParseMessageValidJson() {
    ChatMessage originalMsg = new ChatMessage("Alice", "Hello world", MessageType.CHAT);
    String json = originalMsg.toJson();
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length);

    String parsed = MulticastReceiver.parseMessage(packet);
    assertEquals(json, parsed);
  }

  @Test
  void testParseMessageEmptyContent() {
    ChatMessage originalMsg = new ChatMessage("Bob", "", MessageType.CHAT);
    String json = originalMsg.toJson();
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length);

    String parsed = MulticastReceiver.parseMessage(packet);
    assertEquals(json, parsed);
  }

  @Test
  void testParseMessageInvalidJson() {
    String invalidJson = "not a valid json";
    byte[] data = invalidJson.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length);

    String parsed = MulticastReceiver.parseMessage(packet);
    assertEquals(invalidJson, parsed);
  }

  @Test
  void testParseMessageWithSpecialCharacters() {
    String jsonWithSpecial =
        "{\"msgId\":\"test-id\",\"date\":\"25/03/2026\",\"time\":\"10:30:00\",\"username\":\"Test\",\"content\":\"Hello 世界! ñoño\",\"type\":\"CHAT\"}";
    byte[] data = jsonWithSpecial.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length);

    String parsed = MulticastReceiver.parseMessage(packet);
    assertEquals(jsonWithSpecial, parsed);
  }

  @Test
  void testParseMessagePartialBuffer() {
    String fullJson = "{\"msgId\":\"test\",\"content\":\"Hello\"}";
    byte[] paddedData = new byte[1024];
    byte[] jsonBytes = fullJson.getBytes(StandardCharsets.UTF_8);
    System.arraycopy(jsonBytes, 0, paddedData, 0, jsonBytes.length);

    DatagramPacket packet = new DatagramPacket(paddedData, paddedData.length);
    packet.setLength(jsonBytes.length);
    String parsed = MulticastReceiver.parseMessage(packet);

    assertEquals(fullJson, parsed);
  }

  @Test
  void testSelfMessageFiltering() throws Exception {
    ProtocolHandler mockHandler = mock(ProtocolHandler.class);
    InetAddress selfAddress = InetAddress.getLocalHost();

    ChatMessage selfMsg = new ChatMessage("Self", "My message", MessageType.CHAT);
    String json = selfMsg.toJson();
    byte[] data = json.getBytes(StandardCharsets.UTF_8);

    DatagramPacket packet = new DatagramPacket(data, data.length, selfAddress, 5000);

    assertEquals(selfAddress, packet.getAddress());
  }

  @Test
  void testIsSelfMessageWithSameAddress() throws Exception {
    InetAddress selfAddress = InetAddress.getLocalHost();
    byte[] data = new byte[1024];
    DatagramPacket packet = new DatagramPacket(data, data.length, selfAddress, 5000);

    String json = new ChatMessage("Alice", "Test", MessageType.CHAT).toJson();
    packet.setData(json.getBytes(StandardCharsets.UTF_8));
    packet.setLength(json.length());

    assertEquals(selfAddress, packet.getAddress());
  }

  @Test
  void testIsSelfMessageWithDifferentAddress() throws Exception {
    InetAddress selfAddress = InetAddress.getLocalHost();
    InetAddress otherAddress = InetAddress.getByName("127.0.0.1");

    byte[] data = new byte[1024];
    DatagramPacket packet = new DatagramPacket(data, data.length, otherAddress, 5000);

    assertNotEquals(selfAddress, packet.getAddress());
  }

  @Test
  void testProtocolHandlerDelegation() throws Exception {
    ProtocolHandler mockHandler = mock(ProtocolHandler.class);
    InetAddress selfAddress = InetAddress.getLocalHost();

    ChatMessage msg = new ChatMessage("Alice", "Test message", MessageType.CHAT);
    String json = msg.toJson();
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(data, data.length, selfAddress, 5000);

    mockHandler.process(packet);
    verify(mockHandler, times(1)).process(packet);
  }

  @Test
  void testReceiverProcessesPacketFromSameHostAddress() throws Exception {
    MulticastSocket socket = mock(MulticastSocket.class);
    ProtocolHandler protocolHandler = mock(ProtocolHandler.class);
    InetAddress selfAddress = InetAddress.getByName("127.0.0.1");

    ChatMessage message = new ChatMessage("other-user", "hello", MessageType.CHAT);
    byte[] payload = JsonUtils.toWireJson(message).getBytes(StandardCharsets.UTF_8);

    doAnswer(
            invocation -> {
              DatagramPacket packet = invocation.getArgument(0);
              packet.setData(payload);
              packet.setLength(payload.length);
              packet.setAddress(selfAddress);
              packet.setPort(5000);
              return null;
            })
        .doThrow(new SocketException("socket closed"))
        .when(socket)
        .receive(any(DatagramPacket.class));

    MulticastReceiver receiver = new MulticastReceiver(socket, protocolHandler);
    receiver.start();
    receiver.join(1500);

    verify(protocolHandler, times(1)).process(any(DatagramPacket.class));
  }

  @Test
  void testMessageParsingRoundTrip() {
    ChatMessage original =
        new ChatMessage(
            "25/03/2026",
            "10:30:00",
            "TestUser",
            "Test content",
            MessageType.CHAT,
            "test-uuid-123");
    String json = original.toJson();

    ChatMessage parsed = JsonUtils.fromJson(json);

    assertEquals(original.getDate(), parsed.getDate());
    assertEquals(original.getTime(), parsed.getTime());
    assertEquals(original.getUsername(), parsed.getUsername());
    assertEquals(original.getContent(), parsed.getContent());
    assertEquals(original.getType(), parsed.getType());
    assertEquals(original.getMsgId(), parsed.getMsgId());
  }

  @Test
  void testJsonParsingInvalidFormat() {
    String invalidJson = "{invalid json}";
    assertThrows(JsonSyntaxException.class, () -> JsonUtils.fromJson(invalidJson));
  }
}
