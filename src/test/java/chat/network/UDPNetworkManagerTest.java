package chat.network;

import static org.junit.jupiter.api.Assertions.*;

import chat.exception.NetworkException;
import java.net.MulticastSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UDPNetworkManagerTest {

  @BeforeEach
  void setUp() {
    UDPNetworkManager.resetInstance();
  }

  @AfterEach
  void tearDown() {
    UDPNetworkManager.resetInstance();
  }

  @Test
  void testGetInstance_ReturnsSameInstance() {
    UDPNetworkManager instance1 = UDPNetworkManager.getInstance();
    UDPNetworkManager instance2 = UDPNetworkManager.getInstance();

    assertSame(instance1, instance2, "getInstance() should return the same instance");
  }

  @Test
  void testSingletonPattern_MultipleInstancesAreIdentical() {
    UDPNetworkManager instance1 = UDPNetworkManager.getInstance();
    UDPNetworkManager instance2 = UDPNetworkManager.getInstance();
    UDPNetworkManager instance3 = UDPNetworkManager.getInstance();

    assertSame(instance1, instance2);
    assertSame(instance2, instance3);
  }

  @Test
  void testCreateSocket_Success() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();
    int testPort = 5001;
    String multicastGroup = "239.255.255.250";

    manager.createSocket(testPort, multicastGroup);

    assertTrue(manager.isActive(), "Manager should be active after socket creation");
    assertNotNull(manager.getSocket(), "Socket should not be null");
    assertEquals(multicastGroup, manager.getMulticastGroup().getHostAddress());
    assertEquals(testPort, manager.getPort());
  }

  @Test
  void testCreateSocket_InvalidMulticastAddress() {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    assertThrows(
        NetworkException.class,
        () -> {
          manager.createSocket(5001, "192.168.1.1");
        });
  }

  @Test
  void testCreateSocket_AlreadyActive() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    manager.createSocket(5001, "239.255.255.250");

    assertThrows(
        NetworkException.class,
        () -> {
          manager.createSocket(5002, "239.255.255.251");
        });
  }

  @Test
  void testShutdown_ClosesSocket() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    manager.createSocket(5001, "239.255.255.250");
    assertTrue(manager.isActive());

    manager.shutdown();

    assertFalse(manager.isActive(), "Manager should be inactive after shutdown");
    assertNull(manager.getSocket(), "Socket should be null after shutdown");
  }

  @Test
  void testShutdown_MultipleCallsAreSafe() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    manager.createSocket(5001, "239.255.255.250");
    manager.shutdown();
    manager.shutdown();
    manager.shutdown();

    assertFalse(manager.isActive());
  }

  @Test
  void testCreateSocket_AfterShutdown() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    manager.createSocket(5001, "239.255.255.250");
    manager.shutdown();

    manager.createSocket(5002, "239.255.255.251");

    assertTrue(manager.isActive());
    assertEquals(5002, manager.getPort());
  }

  @Test
  void testSocketType_IsMulticastSocket() throws Exception {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    manager.createSocket(5001, "239.255.255.250");

    assertTrue(manager.getSocket() instanceof MulticastSocket);
  }

  @Test
  void testGetSocket_BeforeCreation_ReturnsNull() {
    UDPNetworkManager manager = UDPNetworkManager.getInstance();

    assertNull(manager.getSocket());
    assertFalse(manager.isActive());
  }

  @Test
  void testResetInstance_AfterUse() throws Exception {
    UDPNetworkManager manager1 = UDPNetworkManager.getInstance();
    manager1.createSocket(5001, "239.255.255.250");

    UDPNetworkManager.resetInstance();

    UDPNetworkManager manager2 = UDPNetworkManager.getInstance();
    assertNotSame(manager1, manager2, "resetInstance should create a new instance");
    assertFalse(manager2.isActive());
  }

  @Test
  void testThreadSafety_MultipleThreadsGetSameInstance() throws InterruptedException {
    final UDPNetworkManager[] instances = new UDPNetworkManager[10];
    Thread[] threads = new Thread[10];

    for (int i = 0; i < 10; i++) {
      final int index = i;
      threads[i] =
          new Thread(
              () -> {
                instances[index] = UDPNetworkManager.getInstance();
              });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 1; i < 10; i++) {
      assertSame(instances[0], instances[i], "All threads should get the same instance");
    }
  }
}
