package chat.network;

import chat.controller.ChatController;
import chat.model.ChatMessage;
import chat.util.JsonUtils;
import chat.util.Logger;
import com.google.gson.JsonSyntaxException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ProtocolHandler {
  private ChatController chatController;
  private String ownUsername;
  private InetAddress ownAddress;
  private int ownPort;

  public ProtocolHandler() {}

  public void setChatController(ChatController controller) {
    this.chatController = controller;
  }

  public void setOwnCredentials(String username, InetAddress address, int port) {
    this.ownUsername = username;
    this.ownAddress = address;
    this.ownPort = port;
  }

  public void process(DatagramPacket packet) {
    try {
      String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
      ChatMessage message = JsonUtils.fromJson(json);

      if (!validateMessage(message)) {
        Logger.warn("Invalid message received, discarding");
        return;
      }

      message.setAddress(packet.getAddress());
      message.setPort(packet.getPort());

      if (isOwnMessage(message, packet.getAddress(), packet.getPort())) {
        Logger.debug("Ignoring own message from " + message.getUsername());
        return;
      }

      if (chatController != null) {
        chatController.handleChatMessage(message);
      }

    } catch (JsonSyntaxException e) {
      Logger.warn("Invalid JSON received, discarding: " + e.getMessage());
    } catch (Exception e) {
      Logger.error("Error processing packet", e);
    }
  }

  private boolean validateMessage(ChatMessage message) {
    if (message == null) {
      return false;
    }
    if (message.getUsername() == null || message.getUsername().isEmpty()) {
      return false;
    }
    if (message.getContent() == null) {
      return false;
    }
    return true;
  }

  private boolean isOwnMessage(ChatMessage message, InetAddress address, int port) {
    if (ownUsername == null || ownAddress == null) {
      return false;
    }
    if (!message.getUsername().equals(ownUsername)) {
      return false;
    }
    return address.equals(ownAddress) && port == ownPort;
  }
}
