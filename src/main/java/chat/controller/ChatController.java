package chat.controller;

import chat.model.ChatMessage;

public interface ChatController {
    void handleChatMessage(ChatMessage message);
}
