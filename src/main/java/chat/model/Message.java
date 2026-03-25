package chat.model;

public interface Message {
  String getDate();

  String getTime();

  String getUsername();

  String getContent();

  MessageType getType();

  String getMsgId();

  String toJson();
}
