package chat.model;

import java.net.InetAddress;
import java.util.Objects;

public class Peer {
  private final String username;
  private final InetAddress address;
  private final int port;
  private long lastSeen;
  private boolean isActive;

  public Peer(String username, InetAddress address, int port) {
    this.username = username;
    this.address = address;
    this.port = port;
    this.lastSeen = System.currentTimeMillis();
    this.isActive = true;
  }

  public String getUsername() {
    return username;
  }

  public InetAddress getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public long getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public String getUniqueId() {
    return buildUniqueId(username, address, port);
  }

  public static String buildUniqueId(String username, InetAddress address, int port) {
    String hostAddress = address != null ? address.getHostAddress() : "unknown";
    String safeUsername = username != null ? username : "unknown";
    return safeUsername + "@" + hostAddress + ":" + port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Peer peer = (Peer) o;
    return Objects.equals(getUniqueId(), peer.getUniqueId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUniqueId());
  }

  @Override
  public String toString() {
    return "Peer{"
        + "username='"
        + username
        + '\''
        + ", address="
        + address
        + ", port="
        + port
        + ", lastSeen="
        + lastSeen
        + ", isActive="
        + isActive
        + '}';
  }
}
