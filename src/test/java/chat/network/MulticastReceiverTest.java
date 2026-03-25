package chat.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.util.JsonUtils;
import com.google.gson.JsonSyntaxException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

class MulticastReceiverTest {

    @Test
    void testParseMessageValidJson() {
        ChatMessage originalMsg = JsonUtils.createChatMessage("Alice", "Hello world");
        String json = originalMsg.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length);

        String parsed = MulticastReceiver.parseMessage(packet);
        assertEquals(json, parsed);
    }

    @Test
    void testParseMessageEmptyContent() {
        ChatMessage originalMsg = JsonUtils.createJoinMessage("Bob");
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
        String jsonWithSpecial = "{\"msgId\":\"test-id\",\"date\":\"25/03/2026\",\"time\":\"10:30:00\",\"username\":\"Test\",\"content\":\"Hello 世界! ñoño\",\"type\":\"CHAT\"}";
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

        ChatMessage selfMsg = JsonUtils.createChatMessage("Self", "My message");
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

        String json = JsonUtils.createChatMessage("Alice", "Test").toJson();
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

        ChatMessage msg = JsonUtils.createChatMessage("Alice", "Test message");
        String json = msg.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, selfAddress, 5000);

        mockHandler.process(packet);
        verify(mockHandler, times(1)).process(packet);
    }

    @Test
    void testMessageParsingRoundTrip() {
        ChatMessage original = new ChatMessage(
            "25/03/2026", "10:30:00", "TestUser", "Test content", MessageType.CHAT, "test-uuid-123"
        );
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
