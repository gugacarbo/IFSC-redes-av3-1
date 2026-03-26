package chat.network;

import chat.model.ChatMessage;
import chat.model.MessageType;
import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingDeque;

public class ChatSession {
  private final String username;
  private final String multicastGroup;
  private final int port;
  private final int ttl;
  private volatile boolean connected;

  private final LinkedBlockingDeque<ChatMessage> outboundMessages;

  private MulticastSender sender;
  private Thread shutdownHook;

  public ChatSession(String username, String multicastGroup, int port, int ttl) {
    this.username = username;
    this.multicastGroup = multicastGroup;
    this.port = port;
    this.ttl = ttl;
    this.connected = false;
    this.outboundMessages = new LinkedBlockingDeque<>();
  }

  public synchronized void join() throws Exception {
    if (connected) {
      throw new IllegalStateException("Session already connected");
    }

    UDPNetworkManager networkManager = UDPNetworkManager.getInstance();
    networkManager.createSocket(port, multicastGroup);

    InetAddress multicastAddress = networkManager.getMulticastGroup();
    sender = new MulticastSender(networkManager.getSocket(), multicastAddress, port);
    sender.start();

    shutdownHook = new Thread(this::leave);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    connected = true;
  }

  public synchronized void leave() {
    if (!connected) {
      return;
    }

    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException e) {
      }
      shutdownHook = null;
    }

    if (sender != null) {
      sender.shutdown();
      sender = null;
    }

    UDPNetworkManager.getInstance().shutdown();
    outboundMessages.clear();

    connected = false;
  }

  public void send(String message) {
    if (!connected) {
      throw new IllegalStateException("Session not connected");
    }

    ChatMessage chatMessage = new ChatMessage(username, message, MessageType.CHAT);
    outboundMessages.offer(chatMessage);

    if (sender != null) {
      sender.sendMessage(chatMessage);
    }
  }

  public boolean isConnected() {
    return connected;
  }

  public String getUsername() {
    return username;
  }

  public String getMulticastGroup() {
    return multicastGroup;
  }

  public int getPort() {
    return port;
  }

  public int getTtl() {
    return ttl;
  }

  public LinkedBlockingDeque<ChatMessage> getOutboundMessages() {
    return outboundMessages;
  }

  public MulticastSender getSender() {
    return sender;
  }
}
