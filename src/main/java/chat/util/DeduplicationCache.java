package chat.util;

import chat.model.ChatMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeduplicationCache {
  private static final int MAX_ENTRIES = 1000;
  private static final long ENTRY_TTL_MS = 60000;

  private final ConcurrentHashMap<String, Long> cache;
  private final ScheduledExecutorService cleanupScheduler;

  public DeduplicationCache() {
    this.cache = new ConcurrentHashMap<>();
    this.cleanupScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "dedup-cache-cleanup");
              t.setDaemon(true);
              return t;
            });
    this.cleanupScheduler.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
  }

  public boolean isDuplicate(ChatMessage message) {
    String msgId = message.getMsgId();
    return cache.containsKey(msgId);
  }

  public void add(ChatMessage message) {
    String msgId = message.getMsgId();
    cache.put(msgId, System.currentTimeMillis());
    evictIfNecessary();
  }

  private void evictIfNecessary() {
    if (cache.size() > MAX_ENTRIES) {
      long cutoffTime = System.currentTimeMillis() - ENTRY_TTL_MS;
      cache.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
      if (cache.size() > MAX_ENTRIES) {
        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
            .skip(MAX_ENTRIES)
            .map(e -> e.getKey())
            .forEach(cache::remove);
      }
    }
  }

  private void cleanup() {
    long cutoffTime = System.currentTimeMillis() - ENTRY_TTL_MS;
    cache.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
  }

  public void shutdown() {
    cleanupScheduler.shutdown();
    try {
      if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public int size() {
    return cache.size();
  }

  public void clear() {
    cache.clear();
  }
}
