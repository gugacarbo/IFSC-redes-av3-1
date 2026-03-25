package chat.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class PeerTest {

    private Peer peer;
    private InetAddress testAddress;

    @BeforeEach
    void setUp() throws UnknownHostException {
        testAddress = InetAddress.getByName("192.168.1.1");
        peer = new Peer("testuser", testAddress, 5000);
    }

    @Test
    void testPeerCreation() {
        assertEquals("testuser", peer.getUsername());
        assertEquals(testAddress, peer.getAddress());
        assertEquals(5000, peer.getPort());
        assertTrue(peer.isActive());
        assertTrue(peer.getLastSeen() > 0);
    }

    @Test
    void testGetUniqueId() {
        String uniqueId = peer.getUniqueId();
        assertEquals("192.168.1.1:5000", uniqueId);
    }

    @Test
    void testEquals_SameUniqueId() throws UnknownHostException {
        Peer peer2 = new Peer("differentuser", testAddress, 5000);
        assertEquals(peer, peer2);
    }

    @Test
    void testEquals_DifferentUniqueId() throws UnknownHostException {
        InetAddress differentAddress = InetAddress.getByName("192.168.1.2");
        Peer peer2 = new Peer("testuser", differentAddress, 5000);
        assertNotEquals(peer, peer2);
    }

    @Test
    void testEquals_SameInstance() {
        assertEquals(peer, peer);
    }

    @Test
    void testEquals_Null() {
        assertNotEquals(peer, null);
    }

    @Test
    void testHashCode_SameUniqueId() throws UnknownHostException {
        Peer peer2 = new Peer("differentuser", testAddress, 5000);
        assertEquals(peer.hashCode(), peer2.hashCode());
    }

    @Test
    void testHashCode_DifferentUniqueId() throws UnknownHostException {
        InetAddress differentAddress = InetAddress.getByName("192.168.1.2");
        Peer peer2 = new Peer("testuser", differentAddress, 5000);
        assertNotEquals(peer.hashCode(), peer2.hashCode());
    }

    @Test
    void testSetLastSeen() {
        long originalLastSeen = peer.getLastSeen();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        peer.setLastSeen(System.currentTimeMillis());
        assertTrue(peer.getLastSeen() >= originalLastSeen);
    }

    @Test
    void testSetActive() {
        assertTrue(peer.isActive());
        peer.setActive(false);
        assertFalse(peer.isActive());
        peer.setActive(true);
        assertTrue(peer.isActive());
    }

    @Test
    void testToString() {
        String str = peer.toString();
        assertTrue(str.contains("testuser"));
        assertTrue(str.contains("192.168.1.1"));
        assertTrue(str.contains("5000"));
    }
}
