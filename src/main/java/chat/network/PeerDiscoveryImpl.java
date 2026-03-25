package chat.network;

import chat.model.ChatMessage;
import chat.model.Message;
import chat.model.MessageType;
import chat.model.Peer;
import chat.util.Logger;

import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerDiscoveryImpl implements PeerDiscovery {
    private static final long JOIN_INTERVAL_MS = 2000;
    private static final long JOIN_DURATION_MS = 10000;
    private static final long PING_INTERVAL_MS = 30000;
    private static final long PEER_TIMEOUT_MS = 90000;

    private final ConcurrentHashMap<String, Peer> peers;
    private final ScheduledExecutorService scheduler;
    private final String username;
    private MulticastSender sender;
    private volatile boolean running;

    public PeerDiscoveryImpl(String username) {
        this.peers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.username = username;
        this.running = false;
    }

    public void setSender(MulticastSender sender) {
        this.sender = sender;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;

        scheduler.scheduleAtFixedRate(this::broadcastJoin, 0, JOIN_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.schedule(this::stopJoinBroadcast, JOIN_DURATION_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::broadcastPing, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::checkPeerTimeouts, PEER_TIMEOUT_MS / 2, PEER_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
    }

    private void stopJoinBroadcast() {
        Logger.info("JOIN broadcast period ended");
    }

    private void broadcastJoin() {
        if (!running || sender == null) {
            return;
        }
        try {
            Message joinMsg = new ChatMessage(username, "joining", MessageType.JOIN);
            sender.sendMessage(joinMsg);
        } catch (Exception e) {
            Logger.error("Failed to broadcast JOIN: " + e.getMessage());
        }
    }

    private void broadcastPing() {
        if (!running || sender == null) {
            return;
        }
        try {
            Message pingMsg = new ChatMessage(username, "ping", MessageType.PING);
            sender.sendMessage(pingMsg);
        } catch (Exception e) {
            Logger.error("Failed to broadcast PING: " + e.getMessage());
        }
    }

    private void checkPeerTimeouts() {
        long now = System.currentTimeMillis();
        peers.values().removeIf(peer -> {
            if (!peer.getUsername().equals(username)) {
                boolean timedOut = (now - peer.getLastSeen()) > PEER_TIMEOUT_MS;
                if (timedOut) {
                    Logger.info("Peer timed out: " + peer.getUsername());
                }
                return timedOut;
            }
            return false;
        });
    }

    @Override
    public void handleJoinMessage(ChatMessage message) {
        handlePeerMessage(message);
    }

    @Override
    public void handleLeaveMessage(ChatMessage message) {
        removePeer(message.getUsername(), message.getAddress(), message.getPort());
    }

    @Override
    public void handlePongMessage(ChatMessage message) {
        updatePeerActivity(message.getUsername(), message.getAddress(), message.getPort());
    }

    @Override
    public void handlePingMessage(ChatMessage message) {
        if (!running || sender == null) {
            return;
        }
        try {
            Message pongMsg = new ChatMessage(username, "pong", MessageType.PONG);
            sender.sendMessage(pongMsg);
        } catch (Exception e) {
            Logger.error("Failed to send PONG: " + e.getMessage());
        }
    }

    private void handlePeerMessage(ChatMessage message) {
        addOrUpdatePeer(message.getUsername(), message.getAddress(), message.getPort());
    }

    private void addOrUpdatePeer(String peerUsername, InetAddress address, int port) {
        if (peerUsername.equals(username)) {
            return;
        }
        String uniqueId = address.getHostAddress() + ":" + port;
        Peer peer = peers.get(uniqueId);
        if (peer == null) {
            peer = new Peer(peerUsername, address, port);
            peers.put(uniqueId, peer);
            Logger.info("Peer joined: " + peerUsername);
        } else {
            peer.setLastSeen(System.currentTimeMillis());
            peer.setActive(true);
        }
    }

    private void removePeer(String peerUsername, InetAddress address, int port) {
        String uniqueId = address.getHostAddress() + ":" + port;
        Peer removed = peers.remove(uniqueId);
        if (removed != null) {
            Logger.info("Peer left: " + peerUsername);
        }
    }

    private void updatePeerActivity(String peerUsername, InetAddress address, int port) {
        String uniqueId = address.getHostAddress() + ":" + port;
        Peer peer = peers.get(uniqueId);
        if (peer != null) {
            peer.setLastSeen(System.currentTimeMillis());
            peer.setActive(true);
        }
    }

    @Override
    public Collection<Peer> getPeers() {
        return peers.values();
    }

    public Peer getPeer(String uniqueId) {
        return peers.get(uniqueId);
    }

    @Override
    public void shutdown() {
        running = false;
        if (sender != null) {
            try {
                Message leaveMsg = new ChatMessage(username, "leaving", MessageType.LEAVE);
                sender.sendMessage(leaveMsg);
            } catch (Exception e) {
                Logger.error("Failed to send LEAVE: " + e.getMessage());
            }
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        peers.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public int getPeerCount() {
        return peers.size();
    }
}
