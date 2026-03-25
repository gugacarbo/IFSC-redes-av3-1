package chat.network;

import chat.exception.NetworkException;
import chat.model.Message;
import chat.model.MessageType;
import chat.util.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MulticastSender extends Thread {
    private static final int MAX_QUEUE_CAPACITY = 1000;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final long MAX_BACKOFF_MS = 1000;

    private final BlockingQueue<Message> messageQueue;
    private final DatagramSocket socket;
    private final InetAddress multicastAddress;
    private final int port;
    private final AtomicBoolean running;

    public MulticastSender(DatagramSocket socket, InetAddress multicastAddress, int port) {
        this.socket = socket;
        this.multicastAddress = multicastAddress;
        this.port = port;
        this.messageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
        this.running = new AtomicBoolean(true);
    }

    public void sendMessage(Message msg) {
        if (!running.get()) {
            return;
        }

        while (messageQueue.size() >= MAX_QUEUE_CAPACITY) {
            Message oldest = messageQueue.peek();
            if (oldest != null && oldest.getType() != MessageType.CHAT) {
                messageQueue.poll();
            } else {
                messageQueue.poll();
                break;
            }
        }

        if (running.get()) {
            messageQueue.offer(msg);
        }
    }

    @Override
    public void run() {
        while (running.get() || !messageQueue.isEmpty()) {
            try {
                Message msg = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    sendWithRetry(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                drainQueue();
                break;
            }
        }
    }

    private void sendWithRetry(Message msg) {
        boolean isControlMessage = isControlMessage(msg.getType());
        int attempts = isControlMessage ? MAX_RETRIES : 1;

        for (int i = 0; i < attempts; i++) {
            try {
                byte[] data = msg.toJson().getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, multicastAddress, port);
                socket.send(packet);
                return;
            } catch (Exception e) {
                Logger.error("Failed to send message: " + e.getMessage(), e);
                if (i < attempts - 1 && isControlMessage) {
                    try {
                        long backoff = Math.min(INITIAL_BACKOFF_MS * (1L << i), MAX_BACKOFF_MS);
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private boolean isControlMessage(MessageType type) {
        return type == MessageType.JOIN || type == MessageType.PING || type == MessageType.ACK;
    }

    public void shutdown() {
        running.set(false);
        interrupt();
    }

    private void drainQueue() {
        Message msg;
        while ((msg = messageQueue.poll()) != null) {
            try {
                byte[] data = msg.toJson().getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, multicastAddress, port);
                socket.send(packet);
            } catch (Exception e) {
                break;
            }
        }
    }

    public BlockingQueue<Message> getQueue() {
        return messageQueue;
    }

    public boolean isRunning() {
        return running.get();
    }
}
