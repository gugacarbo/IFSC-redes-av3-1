package chat.model;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ChatMessage implements Message {
  private final String date;
  private final String time;
  private final String username;
  private final String message;
  private final MessageType type;
  private final String msgId;
  private String originalMsgId;
  private transient InetAddress address;
  private transient int port;

  public ChatMessage(String username, String message, MessageType type) {
    this.date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    this.time = LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    this.username = username;
    this.message = message;
    this.type = type;
    this.msgId = UUID.randomUUID().toString();
  }

  public ChatMessage(
      String date, String time, String username, String message, MessageType type, String msgId) {
    this.date = date;
    this.time = time;
    this.username = username;
    this.message = message;
    this.type = type;
    this.msgId = msgId;
  }

  public ChatMessage(String username, String message, MessageType type, String originalMsgId) {
    this.date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    this.time = LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    this.username = username;
    this.message = message;
    this.type = type;
    this.msgId = UUID.randomUUID().toString();
    this.originalMsgId = originalMsgId;
  }

  @Override
  public String getDate() {
    return date;
  }

  @Override
  public String getTime() {
    return time;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getContent() {
    return message;
  }

  @Override
  public MessageType getType() {
    return type;
  }

  @Override
  public String getMsgId() {
    return msgId;
  }

  @Override
  public String toJson() {
    return chat.util.JsonUtils.toJson(this);
  }

  public String getUniqueKey() {
    return msgId;
  }

  public String getOriginalMsgId() {
    return originalMsgId;
  }

  public void setOriginalMsgId(String originalMsgId) {
    this.originalMsgId = originalMsgId;
  }

  public InetAddress getAddress() {
    return address;
  }

  public void setAddress(InetAddress address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
