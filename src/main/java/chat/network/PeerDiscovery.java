package chat.network;

import chat.model.ChatMessage;
import chat.model.Peer;
import java.util.Collection;

public interface PeerDiscovery {
  void handleJoinMessage(ChatMessage message);

  void handleLeaveMessage(ChatMessage message);

  void handlePongMessage(ChatMessage message);

  void handlePingMessage(ChatMessage message);

  Collection<Peer> getPeers();

  void shutdown();
}
