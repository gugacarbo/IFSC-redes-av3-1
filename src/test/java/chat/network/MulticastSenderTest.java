package chat.network;

import chat.model.ChatMessage;
import chat.model.Message;
import chat.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MulticastSenderTest {

    @Mock
    private DatagramSocket socket;

    private InetAddress multicastAddress;
    private MulticastSender sender;

    @BeforeEach
    void setUp() throws Exception {
        multicastAddress = InetAddress.getByName("224.0.0.1");
        sender = new MulticastSender(socket, multicastAddress, 5000);
    }

    @Test
    void testQueueCapacity() {
        BlockingQueue<Message> queue = sender.getQueue();
        assertEquals(1000, queue.remainingCapacity());
    }

    @Test
    void testSendMessageAddsToQueue() {
        Message msg = new ChatMessage("user", "Hello", MessageType.CHAT);
        sender.sendMessage(msg);
        assertEquals(1, sender.getQueue().size());
    }

    @Test
    void testOverflowPolicyDiscardsOldestNonChatFirst() throws Exception {
        for (int i = 0; i < 1000; i++) {
            sender.sendMessage(new ChatMessage("user", "Message " + i, MessageType.JOIN));
        }

        Message chatMsg = new ChatMessage("user", "Chat message", MessageType.CHAT);
        sender.sendMessage(chatMsg);

        assertEquals(1000, sender.getQueue().size());
    }

    @Test
    void testShutdownStopsAddingMessages() {
        sender.shutdown();
        Message msg = new ChatMessage("user", "Hello", MessageType.CHAT);
        sender.sendMessage(msg);
        assertEquals(0, sender.getQueue().size());
    }

    @Test
    void testRunLoopConsumesQueue() throws Exception {
        Message msg = new ChatMessage("user", "Test", MessageType.CHAT);
        sender.sendMessage(msg);

        sender.start();
        sender.join(1000);

        verify(socket, times(1)).send(any(DatagramPacket.class));
    }

    @Test
    void testChatMessageSentOnce() throws Exception {
        Message msg = new ChatMessage("user", "Chat", MessageType.CHAT);
        sender.sendMessage(msg);

        sender.start();
        sender.join(1000);

        verify(socket, times(1)).send(any(DatagramPacket.class));
    }

    @Test
    void testControlMessageRetryLogic() throws Exception {
        Message msg = new ChatMessage("user", "Join", MessageType.JOIN);
        sender.sendMessage(msg);

        doThrow(new RuntimeException("Network error")).doThrow(new RuntimeException("Network error"))
                .doNothing().when(socket).send(any(DatagramPacket.class));

        sender.start();
        sender.join(5000);

        verify(socket, times(3)).send(any(DatagramPacket.class));
    }

    @Test
    void testUtf8Encoding() throws Exception {
        String unicodeMessage = "Olá Mundo 你好 🌍";
        Message msg = new ChatMessage("user", unicodeMessage, MessageType.CHAT);
        sender.sendMessage(msg);

        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);

        sender.start();
        sender.join(1000);

        verify(socket).send(packetCaptor.capture());
        DatagramPacket sentPacket = packetCaptor.getValue();
        String receivedJson = new String(sentPacket.getData(), 0, sentPacket.getLength(), java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(receivedJson.contains(unicodeMessage));
    }

    @Test
    void testGracefulShutdownWithDrain() throws Exception {
        for (int i = 0; i < 5; i++) {
            sender.sendMessage(new ChatMessage("user", "Msg" + i, MessageType.CHAT));
        }

        sender.start();
        Thread.sleep(50);
        sender.shutdown();
        sender.join(2000);

        assertFalse(sender.isRunning());
    }
}
