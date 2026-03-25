package chat.util;

import java.net.InetAddress;
import java.util.regex.Pattern;

public class ValidationUtils {
  private static final int MIN_PORT = 1024;
  private static final int MAX_PORT = 65535;
  private static final int MIN_TTL = 1;
  private static final int MAX_TTL = 255;
  private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";
  private static final String MULTICAST_IP_PATTERN = "^2([2-4][0-9]|5[0-9])\\.";

  private static final Pattern usernameRegex = Pattern.compile(USERNAME_PATTERN);

  private ValidationUtils() {}

  public static boolean isValidPort(int port) {
    return port >= MIN_PORT && port <= MAX_PORT;
  }

  public static boolean isValidPort(String portStr) {
    if (portStr == null || portStr.isEmpty()) {
      return false;
    }
    try {
      int port = Integer.parseInt(portStr);
      return isValidPort(port);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isValidIp(String ip) {
    if (ip == null || ip.isEmpty()) {
      return false;
    }
    try {
      InetAddress address = InetAddress.getByName(ip);
      byte[] bytes = address.getAddress();
      if (bytes.length == 4) {
        int firstOctet = bytes[0] & 0xFF;
        return firstOctet >= 224 && firstOctet <= 239;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isValidUsername(String username) {
    if (username == null || username.isEmpty()) {
      return false;
    }
    return usernameRegex.matcher(username).matches();
  }

  public static boolean isValidTtl(int ttl) {
    return ttl >= MIN_TTL && ttl <= MAX_TTL;
  }

  public static boolean isValidTtl(String ttlStr) {
    if (ttlStr == null || ttlStr.isEmpty()) {
      return false;
    }
    try {
      int ttl = Integer.parseInt(ttlStr);
      return isValidTtl(ttl);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static String getPortErrorMessage() {
    return "Port must be between " + MIN_PORT + " and " + MAX_PORT;
  }

  public static String getIpErrorMessage() {
    return "IP must be a valid multicast address (224.0.0.0 - 239.255.255.255)";
  }

  public static String getUsernameErrorMessage() {
    return "Username must be 3-20 characters, containing only letters, numbers, and underscores";
  }

  public static String getTtlErrorMessage() {
    return "TTL must be between " + MIN_TTL + " and " + MAX_TTL;
  }
}
