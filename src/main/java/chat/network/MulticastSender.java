package chat.network;

import chat.model.Message;
import chat.util.JsonUtils;
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
      messageQueue.poll();
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
    try {
      byte[] data = JsonUtils.toWireJson(msg).getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet = new DatagramPacket(data, data.length, multicastAddress, port);
      socket.send(packet);
    } catch (Exception e) {
      Logger.error("Failed to send message: " + e.getMessage(), e);
    }
  }

  public void shutdown() {
    running.set(false);
    interrupt();
  }

  private void drainQueue() {
    Message msg;
    while ((msg = messageQueue.poll()) != null) {
      try {
        byte[] data = JsonUtils.toWireJson(msg).getBytes(StandardCharsets.UTF_8);
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
