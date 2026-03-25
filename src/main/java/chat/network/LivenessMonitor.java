package chat.network;

import chat.model.ChatMessage;

public interface LivenessMonitor {
  void handlePingMessage(ChatMessage message);

  void handlePongMessage(ChatMessage message);
}
