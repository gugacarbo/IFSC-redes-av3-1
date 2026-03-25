package chat.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

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

    public synchronized void createSocket(int port, String multicastGroup) throws IOException {
        if (isActive) {
            throw new IllegalStateException("Socket already active. Call shutdown() first.");
        }

        try {
            this.port = port;
            this.multicastGroup = InetAddress.getByName(multicastGroup);

            if (!this.multicastGroup.isMulticastAddress()) {
                throw new IOException("Invalid multicast address: " + multicastGroup);
            }

            this.socket = new MulticastSocket(port);
            this.socket.joinGroup(this.multicastGroup);
            this.isActive = true;
        } catch (IOException e) {
            this.socket = null;
            this.isActive = false;
            throw new IOException("Failed to create multicast socket: " + e.getMessage(), e);
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
                    socket.leaveGroup(multicastGroup);
                }
                socket.close();
            } catch (IOException e) {
                // Log error but continue cleanup
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
