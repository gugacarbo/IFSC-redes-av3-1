package chat.network;

import static org.junit.jupiter.api.Assertions.*;

import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.model.Peer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeerDiscoveryTest {

  private PeerDiscoveryImpl peerDiscovery;
  private static final String USERNAME = "testuser";

  @BeforeEach
  void setUp() {
    peerDiscovery = new PeerDiscoveryImpl(USERNAME);
  }

  @AfterEach
  void tearDown() {
    if (peerDiscovery.isRunning()) {
      peerDiscovery.shutdown();
    }
  }

  @Test
  void testPeerDiscoveryCreation() {
    assertNotNull(peerDiscovery);
    assertEquals(0, peerDiscovery.getPeerCount());
    assertFalse(peerDiscovery.isRunning());
  }

  @Test
  void testHandleJoinMessage() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("192.168.1.100");
    ChatMessage joinMessage = new ChatMessage("peer1", "joining", MessageType.JOIN);
    joinMessage.setAddress(address);
    joinMessage.setPort(5000);

    peerDiscovery.handleJoinMessage(joinMessage);

    assertEquals(1, peerDiscovery.getPeerCount());
    Peer peer = peerDiscovery.getPeer("192.168.1.100:5000");
    assertNotNull(peer);
    assertEquals("peer1", peer.getUsername());
    assertTrue(peer.isActive());
  }

  @Test
  void testHandleJoinMessage_IgnoreOwnMessage() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("192.168.1.100");
    ChatMessage joinMessage = new ChatMessage(USERNAME, "joining", MessageType.JOIN);
    joinMessage.setAddress(address);
    joinMessage.setPort(5000);

    peerDiscovery.handleJoinMessage(joinMessage);

    assertEquals(0, peerDiscovery.getPeerCount());
  }

  @Test
  void testHandleLeaveMessage() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("192.168.1.100");
    ChatMessage joinMessage = new ChatMessage("peer1", "joining", MessageType.JOIN);
    joinMessage.setAddress(address);
    joinMessage.setPort(5000);
    peerDiscovery.handleJoinMessage(joinMessage);

    assertEquals(1, peerDiscovery.getPeerCount());

    ChatMessage leaveMessage = new ChatMessage("peer1", "leaving", MessageType.LEAVE);
    leaveMessage.setAddress(address);
    leaveMessage.setPort(5000);
    peerDiscovery.handleLeaveMessage(leaveMessage);

    assertEquals(0, peerDiscovery.getPeerCount());
  }

  @Test
  void testHandlePongMessage_UpdatesLastSeen() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("192.168.1.100");
    ChatMessage joinMessage = new ChatMessage("peer1", "joining", MessageType.JOIN);
    joinMessage.setAddress(address);
    joinMessage.setPort(5000);
    peerDiscovery.handleJoinMessage(joinMessage);

    Peer peer = peerDiscovery.getPeer("192.168.1.100:5000");
    long originalLastSeen = peer.getLastSeen();

    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    ChatMessage pongMessage = new ChatMessage("peer1", "pong", MessageType.PONG);
    pongMessage.setAddress(address);
    pongMessage.setPort(5000);
    peerDiscovery.handlePongMessage(pongMessage);

    assertTrue(peer.getLastSeen() > originalLastSeen);
  }

  @Test
  void testGetPeers() throws UnknownHostException {
    InetAddress address1 = InetAddress.getByName("192.168.1.100");
    ChatMessage join1 = new ChatMessage("peer1", "joining", MessageType.JOIN);
    join1.setAddress(address1);
    join1.setPort(5000);
    peerDiscovery.handleJoinMessage(join1);

    InetAddress address2 = InetAddress.getByName("192.168.1.101");
    ChatMessage join2 = new ChatMessage("peer2", "joining", MessageType.JOIN);
    join2.setAddress(address2);
    join2.setPort(5001);
    peerDiscovery.handleJoinMessage(join2);

    Collection<Peer> peers = peerDiscovery.getPeers();
    assertEquals(2, peers.size());
  }

  @Test
  void testMultipleJoins_SamePeer() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("192.168.1.100");

    ChatMessage join1 = new ChatMessage("peer1", "joining", MessageType.JOIN);
    join1.setAddress(address);
    join1.setPort(5000);
    peerDiscovery.handleJoinMessage(join1);

    assertEquals(1, peerDiscovery.getPeerCount());

    ChatMessage join2 = new ChatMessage("peer1", "joining", MessageType.JOIN);
    join2.setAddress(address);
    join2.setPort(5000);
    peerDiscovery.handleJoinMessage(join2);

    assertEquals(1, peerDiscovery.getPeerCount());
  }

  @Test
  void testShutdown() throws UnknownHostException {
    peerDiscovery.start();
    assertTrue(peerDiscovery.isRunning());

    InetAddress address = InetAddress.getByName("192.168.1.100");
    ChatMessage join = new ChatMessage("peer1", "joining", MessageType.JOIN);
    join.setAddress(address);
    join.setPort(5000);
    peerDiscovery.handleJoinMessage(join);

    peerDiscovery.shutdown();

    assertFalse(peerDiscovery.isRunning());
    assertEquals(0, peerDiscovery.getPeerCount());
  }

  @Test
  void testStart() {
    peerDiscovery.start();
    assertTrue(peerDiscovery.isRunning());
    peerDiscovery.shutdown();
  }
}
