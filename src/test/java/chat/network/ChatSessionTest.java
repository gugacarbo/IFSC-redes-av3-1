package chat.network;

import static org.junit.jupiter.api.Assertions.*;

import chat.model.Peer;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatSessionTest {

  private ChatSession session;

  @BeforeEach
  void setUp() {
    session = new ChatSession("testuser", "224.0.0.1", 5000, 1);
  }

  @AfterEach
  void tearDown() {
    if (session.isConnected()) {
      session.leave();
    }
    UDPNetworkManager.resetInstance();
  }

  @Test
  void testSessionInitialState() {
    assertFalse(session.isConnected());
    assertEquals("testuser", session.getUsername());
    assertEquals("224.0.0.1", session.getMulticastGroup());
    assertEquals(5000, session.getPort());
    assertEquals(1, session.getTtl());
    assertTrue(session.getActivePeers().isEmpty());
  }

  @Test
  void testJoinSetsConnected() throws Exception {
    session.join();
    assertTrue(session.isConnected());
  }

  @Test
  void testLeaveClearsState() throws Exception {
    session.join();
    session.leave();
    assertFalse(session.isConnected());
    assertTrue(session.getActivePeers().isEmpty());
  }

  @Test
  void testDoubleJoinThrowsException() throws Exception {
    session.join();
    assertThrows(IllegalStateException.class, () -> session.join());
  }

  @Test
  void testLeaveWhenNotConnectedDoesNothing() {
    assertDoesNotThrow(() -> session.leave());
    assertFalse(session.isConnected());
  }

  @Test
  void testSendThrowsWhenNotConnected() {
    assertThrows(IllegalStateException.class, () -> session.send("Hello"));
  }

  @Test
  void testSendAddsMessageToQueue() throws Exception {
    session.join();
    session.send("Test message");
    assertFalse(session.getOutboundMessages().isEmpty());
  }

  @Test
  void testPeerManagement() throws Exception {
    session.join();
    Peer peer = new Peer("otheruser", InetAddress.getByName("192.168.1.1"), 5000);

    session.addPeer(peer);
    assertEquals(1, session.getActivePeers().size());
    assertEquals(peer, session.getPeer(peer.getUniqueId()));

    session.removePeer(peer);
    assertTrue(session.getActivePeers().isEmpty());
  }

  @Test
  void testUpdatePeer() throws Exception {
    session.join();
    Peer peer = new Peer("otheruser", InetAddress.getByName("192.168.1.1"), 5000);

    session.addPeer(peer);

    Peer updatedPeer = new Peer("newname", InetAddress.getByName("192.168.1.1"), 5000);
    session.updatePeer(updatedPeer);

    assertEquals("newname", session.getPeer(peer.getUniqueId()).getUsername());
  }

  @Test
  void testShutdownHookRemovalOnLeave() throws Exception {
    session.join();
    assertTrue(session.isConnected());
    session.leave();
    assertFalse(session.isConnected());
  }
}
