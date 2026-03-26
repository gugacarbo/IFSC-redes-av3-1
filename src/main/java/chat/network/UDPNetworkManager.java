package chat.network;

import chat.exception.NetworkException;
import chat.util.Logger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

public class UDPNetworkManager {
  private static volatile UDPNetworkManager instance;
  private MulticastSocket socket;
  private InetAddress multicastGroup;
  private int port;
  private boolean isActive;

  private UDPNetworkManager() {
    this.socket = null;
    this.multicastGroup = null;
    this.port = 0;
    this.isActive = false;
  }

  public static UDPNetworkManager getInstance() {
    if (instance == null) {
      synchronized (UDPNetworkManager.class) {
        if (instance == null) {
          instance = new UDPNetworkManager();
        }
      }
    }
    return instance;
  }

  public synchronized void createSocket(int port, String multicastGroup) throws NetworkException {
    if (isActive) {
      throw new NetworkException("Socket already active. Call shutdown() first.");
    }

    try {
      this.port = port;
      this.multicastGroup = InetAddress.getByName(multicastGroup);

      if (!this.multicastGroup.isMulticastAddress()) {
        throw new NetworkException("Endereço multicast inválido: " + multicastGroup);
      }

      this.socket = new MulticastSocket(port);
      this.socket.joinGroup(new InetSocketAddress(this.multicastGroup, 0), null);
      this.isActive = true;
      Logger.info("Multicast socket created on port " + port);
    } catch (java.net.UnknownHostException e) {
      this.socket = null;
      this.isActive = false;
      Logger.error("Unknown host: " + e.getMessage(), e);
      throw new NetworkException("Não foi possível resolver o endereço: " + multicastGroup, e);
    } catch (java.net.BindException e) {
      this.socket = null;
      this.isActive = false;
      Logger.error("Port already in use: " + e.getMessage(), e);
      throw new NetworkException("A porta " + port + " já está em uso. Tente outra porta.", e);
    } catch (IOException e) {
      this.socket = null;
      this.isActive = false;
      Logger.error("Failed to create socket: " + e.getMessage(), e);
      throw new NetworkException("Erro ao criar socket de rede: " + e.getMessage(), e);
    }
  }

  public synchronized MulticastSocket getSocket() {
    return socket;
  }

  public synchronized boolean isActive() {
    return isActive;
  }

  public synchronized InetAddress getMulticastGroup() {
    return multicastGroup;
  }

  public synchronized int getPort() {
    return port;
  }

  public synchronized void shutdown() {
    if (socket != null && isActive) {
      try {
        if (multicastGroup != null) {
          socket.leaveGroup(new InetSocketAddress(multicastGroup, 0), null);
        }
        socket.close();
        Logger.info("Multicast socket closed successfully");
      } catch (IOException e) {
        Logger.error("Error closing socket: " + e.getMessage(), e);
      }
    }
    socket = null;
    multicastGroup = null;
    port = 0;
    isActive = false;
  }

  public static synchronized void resetInstance() {
    if (instance != null) {
      instance.shutdown();
      instance = null;
    }
  }
}
