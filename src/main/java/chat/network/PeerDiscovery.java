package chat.network;

import chat.model.ChatMessage;

public interface PeerDiscovery {
    void handleJoinMessage(ChatMessage message);
    void handleLeaveMessage(ChatMessage message);
}
