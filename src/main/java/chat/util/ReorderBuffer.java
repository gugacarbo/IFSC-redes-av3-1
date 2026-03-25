package chat.util;

import chat.model.ChatMessage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ReorderBuffer {
  private static final int MAX_BUFFER_SIZE = 100;
  private static final int OVERFLOW_REMOVE_COUNT = 20;
  private static final long MAX_DELAY_MS = 5000;
  private static final long TIMESTAMP_TOLERANCE_MS = 1000;

  private final PriorityQueue<BufferedMessage> buffer;
  private final ReentrantLock lock;

  public ReorderBuffer() {
    this.buffer =
        new PriorityQueue<>(
            Comparator.comparingLong(BufferedMessage::getReceiveTimestamp)
                .thenComparing(BufferedMessage::getMsgId));
    this.lock = new ReentrantLock();
  }

  public void add(ChatMessage message) {
    lock.lock();
    try {
      long now = System.currentTimeMillis();
      BufferedMessage buffered = new BufferedMessage(message, now);

      buffer.removeIf(m -> (now - m.getReceiveTimestamp()) > MAX_DELAY_MS);

      if (buffer.size() >= MAX_BUFFER_SIZE) {
        List<BufferedMessage> toRemove = new ArrayList<>();
        for (int i = 0; i < OVERFLOW_REMOVE_COUNT && !buffer.isEmpty(); i++) {
          toRemove.add(buffer.poll());
        }
      }

      buffer.add(buffered);
    } finally {
      lock.unlock();
    }
  }

  private static final long DELIVERY_THRESHOLD_MS = 100;

  public List<ChatMessage> deliver() {
    lock.lock();
    try {
      List<ChatMessage> result = new ArrayList<>();
      long now = System.currentTimeMillis();

      List<BufferedMessage> toDeliver = new ArrayList<>();
      while (!buffer.isEmpty()) {
        BufferedMessage next = buffer.peek();
        if (next == null) break;

        long age = now - next.getReceiveTimestamp();

        if (age > MAX_DELAY_MS) {
          buffer.poll();
          continue;
        }

        if (age < DELIVERY_THRESHOLD_MS) {
          break;
        }

        toDeliver.add(buffer.poll());
      }

      toDeliver.sort(
          Comparator.comparingLong(BufferedMessage::getReceiveTimestamp)
              .thenComparing(BufferedMessage::getMsgId));

      for (BufferedMessage bm : toDeliver) {
        result.add(bm.getMessage());
      }

      return result;
    } finally {
      lock.unlock();
    }
  }

  public int size() {
    lock.lock();
    try {
      return buffer.size();
    } finally {
      lock.unlock();
    }
  }

  public void clear() {
    lock.lock();
    try {
      buffer.clear();
    } finally {
      lock.unlock();
    }
  }

  private static class BufferedMessage {
    private final ChatMessage message;
    private final long receiveTimestamp;

    public BufferedMessage(ChatMessage message, long receiveTimestamp) {
      this.message = message;
      this.receiveTimestamp = receiveTimestamp;
    }

    public ChatMessage getMessage() {
      return message;
    }

    public long getReceiveTimestamp() {
      return receiveTimestamp;
    }

    public String getMsgId() {
      return message.getMsgId();
    }
  }
}
