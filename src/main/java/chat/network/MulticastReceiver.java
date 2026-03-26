package chat.network;

import chat.util.Logger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MulticastReceiver extends Thread {
  private static final int BUFFER_SIZE = 65536;
  private static final int SOCKET_TIMEOUT = 1000;

  private final MulticastSocket socket;
  private final ProtocolHandler protocolHandler;
  private final AtomicBoolean running;

  public MulticastReceiver(
      MulticastSocket socket, ProtocolHandler protocolHandler, InetAddress selfAddress) {
    this.socket = Objects.requireNonNull(socket, "Socket cannot be null");
    this.protocolHandler =
        Objects.requireNonNull(protocolHandler, "ProtocolHandler cannot be null");
    this.running = new AtomicBoolean(true);
    setDaemon(true);
  }

  @Override
  public void run() {
    byte[] buffer = new byte[BUFFER_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    try {
      socket.setSoTimeout(SOCKET_TIMEOUT);
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (running.get()) {
      try {
        socket.receive(packet);
        protocolHandler.process(packet);
      } catch (java.net.SocketTimeoutException e) {
        continue;
      } catch (java.net.SocketException e) {
        if (running.get()) {
          Logger.error("Socket error in receiver: " + e.getMessage(), e);
          Thread.currentThread().interrupt();
        }
        break;
      } catch (Exception e) {
        Logger.error("Error in receiver: " + e.getMessage(), e);
        if (running.get()) {
          Thread.currentThread().interrupt();
        }
        break;
      }
      resetPacket(packet, buffer);
    }
  }

  private void resetPacket(DatagramPacket packet, byte[] buffer) {
    packet.setData(buffer);
    packet.setLength(buffer.length);
  }

  public void shutdown() {
    running.set(false);
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
    interrupt();
  }

  public static String parseMessage(DatagramPacket packet) {
    byte[] data = packet.getData();
    int length = packet.getLength();
    return new String(data, 0, length, StandardCharsets.UTF_8);
  }
}
