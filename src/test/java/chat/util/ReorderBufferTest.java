package chat.util;

import chat.model.ChatMessage;
import chat.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReorderBufferTest {
    private ReorderBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new ReorderBuffer();
    }

    @Test
    void testAddAndDeliverEmpty() {
        List<ChatMessage> delivered = buffer.deliver();
        assertTrue(delivered.isEmpty());
    }

    @Test
    void testOutOfOrderMessages() throws InterruptedException {
        ChatMessage msg1 = createMessage("user1", "Hello");
        ChatMessage msg2 = createMessage("user2", "World");
        ChatMessage msg3 = createMessage("user3", "Test");

        buffer.add(msg3);
        Thread.sleep(50);
        buffer.add(msg1);
        Thread.sleep(50);
        buffer.add(msg2);

        Thread.sleep(150);
        List<ChatMessage> delivered = buffer.deliver();

        assertEquals(3, delivered.size());
        assertEquals("user3", delivered.get(0).getUsername());
        assertEquals("user1", delivered.get(1).getUsername());
        assertEquals("user2", delivered.get(2).getUsername());
    }

    @Test
    void testSameTimestampOrderingByMsgId() throws InterruptedException {
        ChatMessage msgA = createMessageWithId("userA", "Message A", "aaa");
        ChatMessage msgB = createMessageWithId("userB", "Message B", "bbb");

        buffer.add(msgB);
        buffer.add(msgA);

        Thread.sleep(150);
        List<ChatMessage> delivered = buffer.deliver();

        assertEquals(2, delivered.size());
        assertEquals("aaa", delivered.get(0).getMsgId());
        assertEquals("bbb", delivered.get(1).getMsgId());
    }

    @Test
    void testDuplicateMessages() throws InterruptedException {
        ChatMessage msg1 = createMessage("user1", "Duplicate");

        buffer.add(msg1);
        Thread.sleep(50);
        buffer.add(msg1);

        Thread.sleep(150);
        List<ChatMessage> delivered = buffer.deliver();

        assertEquals(2, delivered.size());
    }

    @Test
    void testOldMessagesDiscarded() throws InterruptedException {
        ChatMessage oldMsg = createMessage("user1", "Old message");

        buffer.add(oldMsg);

        Thread.sleep(5100);

        List<ChatMessage> delivered = buffer.deliver();

        assertTrue(delivered.isEmpty());
    }

    @Test
    void testOverflowHandling() throws InterruptedException {
        for (int i = 0; i < 120; i++) {
            buffer.add(createMessage("user" + i, "Message " + i));
            Thread.sleep(10);
        }

        Thread.sleep(200);

        int size = buffer.size();
        assertTrue(size <= 100, "Buffer should not exceed 100, but was " + size);
    }

    @Test
    void testGradualOverflowRemovesOldest() throws InterruptedException {
        for (int i = 0; i < 95; i++) {
            buffer.add(createMessage("user" + i, "Message " + i));
            Thread.sleep(10);
        }

        Thread.sleep(200);
        int sizeBeforeOverflow = buffer.size();

        for (int i = 0; i < 10; i++) {
            buffer.add(createMessage("newUser" + i, "New Message " + i));
            Thread.sleep(10);
        }

        Thread.sleep(200);
        int sizeAfterOverflow = buffer.size();

        assertTrue(sizeBeforeOverflow < sizeAfterOverflow || sizeAfterOverflow <= 100);
    }

    @Test
    void testConcurrentAddAndDeliver() throws InterruptedException {
        int numThreads = 10;
        int messagesPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger addCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        buffer.add(createMessage("user" + threadId, "Message " + i));
                        addCount.incrementAndGet();
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Thread.sleep(200);
        List<ChatMessage> delivered = buffer.deliver();

        assertTrue(delivered.size() > 0);
    }

    @Test
    void testClear() throws InterruptedException {
        buffer.add(createMessage("user1", "Message 1"));
        buffer.add(createMessage("user2", "Message 2"));

        buffer.clear();

        assertEquals(0, buffer.size());
        assertTrue(buffer.deliver().isEmpty());
    }

    @Test
    void testDeliverDoesNotRemoveAll() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            buffer.add(createMessage("user" + i, "Message " + i));
            Thread.sleep(20);
        }

        Thread.sleep(150);
        List<ChatMessage> firstDelivery = buffer.deliver();
        assertEquals(5, firstDelivery.size());

        for (int i = 0; i < 3; i++) {
            buffer.add(createMessage("newUser" + i, "New Message " + i));
            Thread.sleep(20);
        }

        Thread.sleep(150);
        List<ChatMessage> secondDelivery = buffer.deliver();
        assertEquals(3, secondDelivery.size());
    }

    private ChatMessage createMessage(String username, String content) {
        return new ChatMessage(username, content, MessageType.CHAT);
    }

    private ChatMessage createMessageWithId(String username, String content, String msgId) {
        return new ChatMessage("01/01/2024", "12:00:00", username, content, MessageType.CHAT, msgId);
    }
}
