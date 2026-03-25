package chat.controller;

import chat.model.ChatMessage;

public interface MessageListener {
    void onMessageReceived(ChatMessage message);
    void onMessageSent(ChatMessage message);
    void onSystemMessage(String message);
}
