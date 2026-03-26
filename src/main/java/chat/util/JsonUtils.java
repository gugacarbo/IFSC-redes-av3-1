package chat.util;

import chat.model.ChatMessage;
import chat.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {
  private static final Gson gson = new GsonBuilder().create();

  private static final class WirePayload {
    private final String date;
    private final String time;
    private final String username;
    private final String message;

    private WirePayload(String date, String time, String username, String message) {
      this.date = date;
      this.time = time;
      this.username = username;
      this.message = message;
    }
  }

  public static String toJson(Message message) {
    return gson.toJson(message);
  }

  public static String toWireJson(Message message) {
    return gson.toJson(
        new WirePayload(
            message.getDate(), message.getTime(), message.getUsername(), message.getContent()));
  }

  public static ChatMessage fromJson(String json) throws JsonSyntaxException {
    return gson.fromJson(json, ChatMessage.class);
  }
}
