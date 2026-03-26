package chat.network;

import chat.model.ChatMessage;
import chat.model.MessageType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PendingRequestTracker {
  private static final long RETRY_INTERVAL_MS = 5000;
  private static final int MAX_RETRIES = 3;
  private static final long CLEANUP_TIMEOUT_MS = 15000;

  private final ConcurrentHashMap<String, PendingRequest> pendingRequests =
      new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private Consumer<ChatMessage> messageSender;

  public PendingRequestTracker() {
    startCleanupTask();
  }

  public void setMessageSender(Consumer<ChatMessage> messageSender) {
    this.messageSender = messageSender;
  }

  public void trackRequest(ChatMessage message) {
    if (message.getType() == MessageType.JOIN || message.getType() == MessageType.PING) {
      cleanupExpiredRequests();
      pendingRequests.put(
          message.getMsgId(), new PendingRequest(message, System.currentTimeMillis()));
    }
  }

  public void handleAckMessage(ChatMessage message) {
    if (message.getOriginalMsgId() != null) {
      pendingRequests.remove(message.getOriginalMsgId());
    }
  }

  public void handlePongMessage(ChatMessage message) {
    if (message.getOriginalMsgId() != null) {
      pendingRequests.remove(message.getOriginalMsgId());
    }
  }

  public boolean hasPendingRequest(String msgId) {
    cleanupExpiredRequests();
    return pendingRequests.containsKey(msgId);
  }

  public int getPendingCount() {
    cleanupExpiredRequests();
    return pendingRequests.size();
  }

  private void startCleanupTask() {
    scheduler.scheduleAtFixedRate(
        () -> {
          cleanupExpiredRequests();
          retryPendingRequests();
        },
        RETRY_INTERVAL_MS,
        RETRY_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  private void cleanupExpiredRequests() {
    long now = System.currentTimeMillis();
    pendingRequests
        .entrySet()
        .removeIf(entry -> (now - entry.getValue().getTimestamp()) > CLEANUP_TIMEOUT_MS);
  }

  private void retryPendingRequests() {
    if (messageSender == null) return;

    for (PendingRequest request : pendingRequests.values()) {
      if (request.canRetry()) {
        request.incrementRetryCount();
        messageSender.accept(request.getMessage());
      }
    }
  }

  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private class PendingRequest {
    private final ChatMessage message;
    private final long timestamp;
    private int retryCount;

    public PendingRequest(ChatMessage message, long timestamp) {
      this.message = message;
      this.timestamp = timestamp;
      this.retryCount = 0;
    }

    public ChatMessage getMessage() {
      return message;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public int getRetryCount() {
      return retryCount;
    }

    public void incrementRetryCount() {
      retryCount++;
    }

    public boolean canRetry() {
      return retryCount < MAX_RETRIES;
    }
  }
}
