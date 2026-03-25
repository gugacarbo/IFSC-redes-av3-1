package chat.config;

import chat.exception.ConfigurationException;
import chat.util.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class AppConfig {
    private String username;
    private String multicastGroup;
    private int port;
    private int ttl;
    private int windowWidth;
    private int windowHeight;
    private int windowX;
    private int windowY;

    private static final String CONFIG_DIR = ".udp_chat";
    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MULTICAST_GROUP = "multicastGroup";
    private static final String KEY_PORT = "port";
    private static final String KEY_TTL = "ttl";
    private static final String KEY_WINDOW_WIDTH = "windowWidth";
    private static final String KEY_WINDOW_HEIGHT = "windowHeight";
    private static final String KEY_WINDOW_X = "windowX";
    private static final String KEY_WINDOW_Y = "windowY";

    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_MULTICAST_GROUP = "224.0.0.1";
    private static final int DEFAULT_TTL = 1;

    public AppConfig() {
        this.username = "";
        this.multicastGroup = DEFAULT_MULTICAST_GROUP;
        this.port = DEFAULT_PORT;
        this.ttl = DEFAULT_TTL;
        this.windowWidth = 800;
        this.windowHeight = 600;
        this.windowX = -1;
        this.windowY = -1;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMulticastGroup() {
        return multicastGroup;
    }

    public void setMulticastGroup(String multicastGroup) {
        this.multicastGroup = multicastGroup;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public int getWindowX() {
        return windowX;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }

    private Path getConfigPath() {
        String homeDir = System.getProperty("user.home");
        return Paths.get(homeDir, CONFIG_DIR, CONFIG_FILE);
    }

    private void ensureConfigDirExists(Path configPath) throws IOException {
        Path configDir = configPath.getParent();
        if (configDir != null && !Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    public void load() throws ConfigurationException {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            Logger.info("Config file not found, using defaults");
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException e) {
            Logger.error("Failed to load config: " + e.getMessage(), e);
            throw new ConfigurationException("Falha ao carregar configurações: " + e.getMessage(), e);
        }

        if (properties.containsKey(KEY_USERNAME)) {
            this.username = properties.getProperty(KEY_USERNAME);
        }
        if (properties.containsKey(KEY_MULTICAST_GROUP)) {
            this.multicastGroup = properties.getProperty(KEY_MULTICAST_GROUP);
        }
        if (properties.containsKey(KEY_PORT)) {
            this.port = Integer.parseInt(properties.getProperty(KEY_PORT));
        }
        if (properties.containsKey(KEY_TTL)) {
            this.ttl = Integer.parseInt(properties.getProperty(KEY_TTL));
        }
        if (properties.containsKey(KEY_WINDOW_WIDTH)) {
            this.windowWidth = Integer.parseInt(properties.getProperty(KEY_WINDOW_WIDTH));
        }
        if (properties.containsKey(KEY_WINDOW_HEIGHT)) {
            this.windowHeight = Integer.parseInt(properties.getProperty(KEY_WINDOW_HEIGHT));
        }
        if (properties.containsKey(KEY_WINDOW_X)) {
            this.windowX = Integer.parseInt(properties.getProperty(KEY_WINDOW_X));
        }
        if (properties.containsKey(KEY_WINDOW_Y)) {
            this.windowY = Integer.parseInt(properties.getProperty(KEY_WINDOW_Y));
        }
        
        if (!validate()) {
            Logger.warn("Loaded config is invalid, using defaults");
        } else {
            Logger.info("Configuration loaded successfully");
        }
    }

    public void save() throws ConfigurationException {
        Path configPath = getConfigPath();
        try {
            ensureConfigDirExists(configPath);
        } catch (IOException e) {
            Logger.error("Failed to create config directory: " + e.getMessage(), e);
            throw new ConfigurationException("Falha ao criar diretório de configurações: " + e.getMessage(), e);
        }

        Properties properties = new Properties();
        properties.setProperty(KEY_USERNAME, username);
        properties.setProperty(KEY_MULTICAST_GROUP, multicastGroup);
        properties.setProperty(KEY_PORT, String.valueOf(port));
        properties.setProperty(KEY_TTL, String.valueOf(ttl));
        properties.setProperty(KEY_WINDOW_WIDTH, String.valueOf(windowWidth));
        properties.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(windowHeight));
        properties.setProperty(KEY_WINDOW_X, String.valueOf(windowX));
        properties.setProperty(KEY_WINDOW_Y, String.valueOf(windowY));

        try (OutputStream output = Files.newOutputStream(configPath)) {
            properties.store(output, "UDP Chat Configuration");
            Logger.info("Configuration saved successfully");
        } catch (IOException e) {
            Logger.error("Failed to save config: " + e.getMessage(), e);
            throw new ConfigurationException("Falha ao salvar configurações: " + e.getMessage(), e);
        }
    }

    public static boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }

    public static boolean isValidMulticastIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            return first >= 224 && first <= 239;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidTtl(int ttl) {
        return ttl >= 1 && ttl <= 255;
    }

    public boolean validate() {
        return isValidPort(port) && isValidMulticastIp(multicastGroup) && isValidTtl(ttl);
    }
}
