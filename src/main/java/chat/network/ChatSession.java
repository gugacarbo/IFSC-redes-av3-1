package chat.network;

import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.model.Peer;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class ChatSession {
    private final String username;
    private final String multicastGroup;
    private final int port;
    private final int ttl;
    private volatile boolean connected;

    private final List<Peer> activePeers;
    private final LinkedBlockingDeque<ChatMessage> outboundMessages;

    private MulticastSender sender;
    private Thread shutdownHook;
    private final ConcurrentHashMap<String, Peer> peerMap;

    public ChatSession(String username, String multicastGroup, int port, int ttl) {
        this.username = username;
        this.multicastGroup = multicastGroup;
        this.port = port;
        this.ttl = ttl;
        this.connected = false;
        this.activePeers = new CopyOnWriteArrayList<>();
        this.outboundMessages = new LinkedBlockingDeque<>();
        this.peerMap = new ConcurrentHashMap<>();
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

        broadcast(MessageType.JOIN, "Joined chat");

        shutdownHook = new Thread(this::leave);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        connected = true;
    }

    public synchronized void leave() {
        if (!connected) {
            return;
        }

        broadcast(MessageType.LEAVE, "Left chat");

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

        activePeers.clear();
        peerMap.clear();
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

    private void broadcast(MessageType type, String content) {
        ChatMessage message = new ChatMessage(username, content, type);
        if (sender != null && sender.isRunning()) {
            sender.sendMessage(message);
        }
    }

    public void addPeer(Peer peer) {
        peerMap.put(peer.getUniqueId(), peer);
        if (!activePeers.contains(peer)) {
            activePeers.add(peer);
        }
    }

    public void removePeer(Peer peer) {
        peerMap.remove(peer.getUniqueId());
        activePeers.remove(peer);
    }

    public void updatePeer(Peer peer) {
        peerMap.put(peer.getUniqueId(), peer);
        int index = activePeers.indexOf(peer);
        if (index >= 0) {
            activePeers.set(index, peer);
        }
    }

    public Peer getPeer(String uniqueId) {
        return peerMap.get(uniqueId);
    }

    public List<Peer> getActivePeers() {
        return new CopyOnWriteArrayList<>(activePeers);
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
}