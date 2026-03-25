package chat.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void testDefaultValues() {
        AppConfig config = new AppConfig();
        assertEquals("", config.getUsername());
        assertEquals("224.0.0.1", config.getMulticastGroup());
        assertEquals(5000, config.getPort());
        assertEquals(1, config.getTtl());
        assertEquals(800, config.getWindowWidth());
        assertEquals(600, config.getWindowHeight());
        assertEquals(-1, config.getWindowX());
        assertEquals(-1, config.getWindowY());
    }

    @Test
    void testIsValidPort() {
        assertFalse(AppConfig.isValidPort(1023));
        assertTrue(AppConfig.isValidPort(1024));
        assertTrue(AppConfig.isValidPort(5000));
        assertTrue(AppConfig.isValidPort(65535));
        assertFalse(AppConfig.isValidPort(65536));
        assertFalse(AppConfig.isValidPort(-1));
    }

    @Test
    void testIsValidMulticastIp() {
        assertFalse(AppConfig.isValidMulticastIp("223.255.255.255"));
        assertTrue(AppConfig.isValidMulticastIp("224.0.0.0"));
        assertTrue(AppConfig.isValidMulticastIp("224.0.0.1"));
        assertTrue(AppConfig.isValidMulticastIp("239.255.255.255"));
        assertFalse(AppConfig.isValidMulticastIp("240.0.0.0"));
        assertFalse(AppConfig.isValidMulticastIp(null));
        assertFalse(AppConfig.isValidMulticastIp(""));
        assertFalse(AppConfig.isValidMulticastIp("invalid"));
    }

    @Test
    void testIsValidTtl() {
        assertFalse(AppConfig.isValidTtl(0));
        assertTrue(AppConfig.isValidTtl(1));
        assertTrue(AppConfig.isValidTtl(128));
        assertTrue(AppConfig.isValidTtl(255));
        assertFalse(AppConfig.isValidTtl(256));
        assertFalse(AppConfig.isValidTtl(-1));
    }

    @Test
    void testValidate() {
        AppConfig config = new AppConfig();
        assertTrue(config.validate());

        config.setPort(1023);
        assertFalse(config.validate());

        config.setPort(5000);
        config.setMulticastGroup("192.168.1.1");
        assertFalse(config.validate());

        config.setMulticastGroup("230.0.0.1");
        assertTrue(config.validate());

        config.setTtl(256);
        assertFalse(config.validate());
    }

    @Test
    void testSettersAndGetters() {
        AppConfig config = new AppConfig();

        config.setUsername("Alice");
        assertEquals("Alice", config.getUsername());

        config.setMulticastGroup("232.0.0.5");
        assertEquals("232.0.0.5", config.getMulticastGroup());

        config.setPort(7000);
        assertEquals(7000, config.getPort());

        config.setTtl(64);
        assertEquals(64, config.getTtl());

        config.setWindowWidth(1920);
        assertEquals(1920, config.getWindowWidth());

        config.setWindowHeight(1080);
        assertEquals(1080, config.getWindowHeight());

        config.setWindowX(50);
        assertEquals(50, config.getWindowX());

        config.setWindowY(75);
        assertEquals(75, config.getWindowY());
    }

    @Test
    void testConfigFileFormat() throws IOException {
        Path configDir = tempDir.resolve(".udp_chat");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.properties");

        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());

            AppConfig config = new AppConfig();
            config.setUsername("TestUser");
            config.setMulticastGroup("230.0.0.1");
            config.setPort(6000);
            config.setTtl(10);
            config.setWindowWidth(1024);
            config.setWindowHeight(768);
            config.setWindowX(100);
            config.setWindowY(200);
            config.save();

            assertTrue(Files.exists(configFile));
            String content = Files.readString(configFile);
            assertTrue(content.contains("username=TestUser"));
            assertTrue(content.contains("multicastGroup=230.0.0.1"));
            assertTrue(content.contains("port=6000"));
            assertTrue(content.contains("ttl=10"));
            assertTrue(content.contains("windowWidth=1024"));
            assertTrue(content.contains("windowHeight=768"));
            assertTrue(content.contains("windowX=100"));
            assertTrue(content.contains("windowY=200"));

        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testLoadConfig() throws IOException {
        Path configDir = tempDir.resolve(".udp_chat");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("config.properties");

        String configContent = """
            username=LoadedUser
            multicastGroup=235.0.0.1
            port=8000
            ttl=32
            windowWidth=1280
            windowHeight=720
            windowX=10
            windowY=20
            """;
        Files.writeString(configFile, configContent);

        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());

            AppConfig config = new AppConfig();
            config.load();

            assertEquals("LoadedUser", config.getUsername());
            assertEquals("235.0.0.1", config.getMulticastGroup());
            assertEquals(8000, config.getPort());
            assertEquals(32, config.getTtl());
            assertEquals(1280, config.getWindowWidth());
            assertEquals(720, config.getWindowHeight());
            assertEquals(10, config.getWindowX());
            assertEquals(20, config.getWindowY());

        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testLoadNonExistentFile() {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());

            AppConfig config = new AppConfig();
            config.load();

            assertEquals("224.0.0.1", config.getMulticastGroup());
            assertEquals(5000, config.getPort());
            assertEquals(1, config.getTtl());

        } catch (IOException e) {
            fail("Should not throw when config file does not exist");
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
