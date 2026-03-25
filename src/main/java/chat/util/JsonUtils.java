package chat.util;

import chat.model.ChatMessage;
import chat.model.Message;
import chat.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {
  private static final Gson gson = new GsonBuilder().create();

  public static String toJson(Message message) {
    return gson.toJson(message);
  }

  public static ChatMessage fromJson(String json) throws JsonSyntaxException {
    return gson.fromJson(json, ChatMessage.class);
  }

  public static ChatMessage createChatMessage(String username, String content) {
    return new ChatMessage(username, content, MessageType.CHAT);
  }

  public static ChatMessage createJoinMessage(String username) {
    return new ChatMessage(username, "", MessageType.JOIN);
  }

  public static ChatMessage createLeaveMessage(String username) {
    return new ChatMessage(username, "", MessageType.LEAVE);
  }

  public static ChatMessage createPingMessage(String username) {
    return new ChatMessage(username, "", MessageType.PING);
  }

  public static ChatMessage createPongMessage(String username, String originalMsgId) {
    return new ChatMessage(username, "", MessageType.PONG, originalMsgId);
  }

  public static ChatMessage createAckMessage(String username, String originalMsgId) {
    return new ChatMessage(username, "", MessageType.ACK, originalMsgId);
  }
}
