package chat.controller;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import chat.config.AppConfig;
import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.network.ChatSession;
import chat.network.MulticastReceiver;
import chat.network.ProtocolHandler;
import chat.network.UDPNetworkManager;
import chat.util.Logger;
import chat.view.ChatPanel;
import chat.view.UsersListPanel;

public class ChatControllerImpl implements ChatController {
  private ChatSession session;
  private ProtocolHandler protocolHandler;
  private MulticastReceiver receiver;

  private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
  private final List<PeerListener> peerListeners = new CopyOnWriteArrayList<>();

  private ChatPanel chatPanel;
  private UsersListPanel usersListPanel;
  private String username;
  private String multicastGroup;
  private int port;
  private int ttl;

  public ChatControllerImpl() {
    loadConfig();
  }

  private void loadConfig() {
    AppConfig config = new AppConfig();
    try {
      config.load();
      this.username = config.getUsername();
      this.multicastGroup = config.getMulticastGroup();
      this.port = config.getPort();
      this.ttl = config.getTtl();
    } catch (Exception e) {
      Logger.warn("Failed to load config, using defaults");
      this.username = "User";
      this.multicastGroup = "224.0.0.1";
      this.port = 5000;
      this.ttl = 1;
    }
  }

  public void setChatPanel(ChatPanel chatPanel) {
    this.chatPanel = chatPanel;
  }

  public void setUsersListPanel(UsersListPanel usersListPanel) {
    this.usersListPanel = usersListPanel;
  }

  public void addMessageListener(MessageListener listener) {
    if (listener != null) {
      messageListeners.add(listener);
    }
  }

  public void removeMessageListener(MessageListener listener) {
    messageListeners.remove(listener);
  }

  public void addPeerListener(PeerListener listener) {
    if (listener != null) {
      peerListeners.add(listener);
    }
  }

  public void removePeerListener(PeerListener listener) {
    peerListeners.remove(listener);
  }

  public void connect() {
    if (session != null && session.isConnected()) {
      Logger.warn("Already connected");
      return;
    }

    try {
      loadConfig();

      session = new ChatSession(username, multicastGroup, port, ttl);
      session.join();

      protocolHandler = new ProtocolHandler();
      protocolHandler.setChatController(this);
      protocolHandler.setOwnCredentials(username, InetAddress.getLocalHost(), port);

      java.net.MulticastSocket multicastSocket = (java.net.MulticastSocket) UDPNetworkManager.getInstance().getSocket();

      receiver = new MulticastReceiver(multicastSocket, protocolHandler);
      receiver.start();

      SwingUtilities.invokeLater(
          () -> {
            if (chatPanel != null) {
              chatPanel.setConnected(true);
              chatPanel.setUsername(username);
              chatPanel.appendMessage(
                  new ChatMessage(
                      "Sistema",
                      "Conectado ao grupo " + multicastGroup + ":" + port,
                      MessageType.CHAT));
            }
          });

      notifySystemMessage("Conectado ao grupo " + multicastGroup + ":" + port);
      Logger.info("Connected to chat session");

    } catch (Exception e) {
      Logger.error("Failed to connect", e);
      notifySystemMessage("Erro ao conectar: " + e.getMessage());
      disconnect();
    }
  }

  public void disconnect() {
    if (session == null || !session.isConnected()) {
      return;
    }

    try {
      if (receiver != null) {
        receiver.shutdown();
        receiver = null;
      }

      session.leave();
      session = null;

      SwingUtilities.invokeLater(
          () -> {
            if (chatPanel != null) {
              chatPanel.setConnected(false);
              chatPanel.appendMessage(
                  new ChatMessage("Sistema", "Desconectado do chat", MessageType.CHAT));
            }
            if (usersListPanel != null) {
              usersListPanel.clear();
            }
          });

      notifySystemMessage("Desconectado do chat");
      Logger.info("Disconnected from chat session");

    } catch (Exception e) {
      Logger.error("Error during disconnect", e);
    }
  }

  public void sendMessage(String text) {
    if (session == null || !session.isConnected()) {
      notifySystemMessage("Não conectado ao chat");
      return;
    }

    if (text == null || text.trim().isEmpty()) {
      return;
    }

    try {
      session.send(text);
      ChatMessage message = new ChatMessage(username, text, MessageType.CHAT);
      notifyMessageSent(message);
    } catch (Exception e) {
      Logger.error("Failed to send message", e);
      notifySystemMessage("Erro ao enviar mensagem: " + e.getMessage());
    }
  }

  @Override
  public void handleChatMessage(ChatMessage message) {
    notifyMessageReceived(message);
  }

  private void notifyMessageReceived(ChatMessage message) {
    SwingUtilities.invokeLater(
        () -> {
          if (chatPanel != null) {
            chatPanel.appendMessage(message);
          }
        });
    for (MessageListener listener : messageListeners) {
      listener.onMessageReceived(message);
    }
  }

  private void notifyMessageSent(ChatMessage message) {
    SwingUtilities.invokeLater(
        () -> {
          if (chatPanel != null) {
            chatPanel.appendMessage(message);
          }
        });
    for (MessageListener listener : messageListeners) {
      listener.onMessageSent(message);
    }
  }

  private void notifySystemMessage(String message) {
    SwingUtilities.invokeLater(
        () -> {
          if (chatPanel != null) {
            chatPanel.appendMessage(new ChatMessage("Sistema", message, MessageType.CHAT));
          }
        });
    for (MessageListener listener : messageListeners) {
      listener.onSystemMessage(message);
    }
  }

  public boolean isConnected() {
    return session != null && session.isConnected();
  }

  public ChatSession getSession() {
    return session;
  }

  public void shutdown() {
    disconnect();
  }
}
