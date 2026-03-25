package chat.network;

import chat.controller.ChatController;
import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.util.JsonUtils;
import chat.util.Logger;
import com.google.gson.JsonSyntaxException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ProtocolHandler {
  private ChatController chatController;
  private PeerDiscovery peerDiscovery;
  private LivenessMonitor livenessMonitor;
  private PendingRequestTracker pendingRequestTracker;
  private String ownUsername;
  private InetAddress ownAddress;
  private int ownPort;

  public ProtocolHandler() {}

  public void setChatController(ChatController controller) {
    this.chatController = controller;
  }

  public void setPeerDiscovery(PeerDiscovery discovery) {
    this.peerDiscovery = discovery;
  }

  public void setLivenessMonitor(LivenessMonitor monitor) {
    this.livenessMonitor = monitor;
  }

  public void setPendingRequestTracker(PendingRequestTracker tracker) {
    this.pendingRequestTracker = tracker;
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
      message.setAddress(packet.getAddress());
      message.setPort(packet.getPort());

      if (!validateMessage(message)) {
        Logger.warn("Invalid message received, discarding");
        return;
      }

      if (isOwnMessage(message, packet.getAddress(), packet.getPort())) {
        Logger.debug("Ignoring own message from " + message.getUsername());
        return;
      }

      routeMessage(message);

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
    if (message.getType() == null) {
      return false;
    }
    if (message.getUsername() == null || message.getUsername().isEmpty()) {
      return false;
    }
    if (message.getMsgId() == null || message.getMsgId().isEmpty()) {
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

  private void routeMessage(ChatMessage message) {
    MessageType type = message.getType();

    switch (type) {
      case CHAT:
        if (chatController != null) {
          chatController.handleChatMessage(message);
        }
        break;
      case JOIN:
        if (peerDiscovery != null) {
          peerDiscovery.handleJoinMessage(message);
        }
        break;
      case LEAVE:
        if (peerDiscovery != null) {
          peerDiscovery.handleLeaveMessage(message);
        }
        break;
      case PING:
        if (livenessMonitor != null) {
          livenessMonitor.handlePingMessage(message);
        }
        if (peerDiscovery != null) {
          peerDiscovery.handlePingMessage(message);
        }
        break;
      case PONG:
        if (pendingRequestTracker != null) {
          pendingRequestTracker.handlePongMessage(message);
        }
        if (livenessMonitor != null) {
          livenessMonitor.handlePongMessage(message);
        }
        if (peerDiscovery != null) {
          peerDiscovery.handlePongMessage(message);
        }
        break;
      case ACK:
        if (pendingRequestTracker != null) {
          pendingRequestTracker.handleAckMessage(message);
        }
        break;
      default:
        Logger.warn("Unknown message type: " + type);
    }
  }
}
