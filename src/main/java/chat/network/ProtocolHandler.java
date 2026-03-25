package chat.network;

import java.net.DatagramPacket;

public interface ProtocolHandler {
    void process(DatagramPacket packet);
}
