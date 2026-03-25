package chat.controller;

import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.model.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ChatControllerImplTest {

    private ChatControllerImpl controller;
    private List<String> receivedMessages;
    private List<String> sentMessages;
    private List<String> systemMessages;
    private List<Peer> peerJoins;
    private List<Peer> peerLeaves;

    @BeforeEach
    void setUp() {
        controller = new ChatControllerImpl();
        receivedMessages = new CopyOnWriteArrayList<>();
        sentMessages = new CopyOnWriteArrayList<>();
        systemMessages = new CopyOnWriteArrayList<>();
        peerJoins = new CopyOnWriteArrayList<>();
        peerLeaves = new CopyOnWriteArrayList<>();

        controller.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                receivedMessages.add(message.getContent());
            }

            @Override
            public void onMessageSent(ChatMessage message) {
                sentMessages.add(message.getContent());
            }

            @Override
            public void onSystemMessage(String message) {
                systemMessages.add(message);
            }
        });

        controller.addPeerListener(new PeerListener() {
            @Override
            public void onPeerJoined(Peer peer) {
                peerJoins.add(peer);
            }

            @Override
            public void onPeerLeft(Peer peer) {
                peerLeaves.add(peer);
            }

            @Override
            public void onPeerUpdated(Peer peer) {
            }

            @Override
            public void onPeerListChanged(List<Peer> peers) {
            }
        });
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Test
    void testControllerInitialization() {
        assertNotNull(controller);
        assertFalse(controller.isConnected());
    }

    @Test
    void testAddMessageListener() {
        AtomicBoolean called = new AtomicBoolean(false);
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                called.set(true);
            }

            @Override
            public void onMessageSent(ChatMessage message) {
            }

            @Override
            public void onSystemMessage(String message) {
            }
        };

        controller.addMessageListener(listener);
        controller.removeMessageListener(listener);
    }

    @Test
    void testAddPeerListener() {
        AtomicBoolean called = new AtomicBoolean(false);
        PeerListener listener = new PeerListener() {
            @Override
            public void onPeerJoined(Peer peer) {
                called.set(true);
            }

            @Override
            public void onPeerLeft(Peer peer) {
            }

            @Override
            public void onPeerUpdated(Peer peer) {
            }

            @Override
            public void onPeerListChanged(List<Peer> peers) {
            }
        };

        controller.addPeerListener(listener);
        controller.removePeerListener(listener);
    }

    @Test
    void testSendMessageWhenDisconnected() throws InterruptedException {
        controller.sendMessage("Test message");
        
        Thread.sleep(100);
        
        assertTrue(systemMessages.stream().anyMatch(m -> m.contains("Não conectado")));
    }

    @Test
    void testSendEmptyMessage() {
        controller.sendMessage("");
        controller.sendMessage("   ");
        
        assertTrue(sentMessages.isEmpty());
    }

    @Test
    void testHandleChatMessage() throws Exception {
        ChatMessage message = new ChatMessage("TestUser", "Hello world", MessageType.CHAT);
        message.setAddress(InetAddress.getByName("127.0.0.1"));
        message.setPort(5000);
        
        controller.handleChatMessage(message);
        
        assertTrue(receivedMessages.contains("Hello world"));
    }

    @Test
    void testDisconnectWhenAlreadyDisconnected() {
        controller.disconnect();
        
        assertFalse(controller.isConnected());
    }

    @Test
    void testControllerImplementsChatControllerInterface() {
        assertTrue(controller instanceof ChatController);
    }

    @Test
    void testMultipleListeners() {
        List<Integer> callCounts = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            final int count = i;
            controller.addMessageListener(new MessageListener() {
                @Override
                public void onMessageReceived(ChatMessage message) {
                    callCounts.add(count);
                }

                @Override
                public void onMessageSent(ChatMessage message) {
                }

                @Override
                public void onSystemMessage(String message) {
                }
            });
        }
        
        ChatMessage message = new ChatMessage("User", "Test", MessageType.CHAT);
        controller.handleChatMessage(message);
        
        assertEquals(3, callCounts.size());
    }
}
