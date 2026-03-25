package chat.view;

import chat.config.AppConfig;
import chat.network.ChatSession;
import chat.network.UDPNetworkManager;
import chat.util.Logger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainFrame extends JFrame {
  private static final int MIN_WIDTH = 600;
  private static final int MIN_HEIGHT = 400;

  private final AppConfig config;
  private ChatPanel chatPanel;
  private UsersListPanel usersListPanel;
  private JLabel statusLabel;
  private JLabel groupInfoLabel;
  private JButton connectButton;
  private JButton disconnectButton;
  private JButton clearButton;

  private ChatSession chatSession;
  private boolean connected = false;

  public MainFrame() {
    this.config = new AppConfig();

    setTitle("UDP Chat P2P");
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

    initLookAndFeel();
    initComponents();
    loadConfig();
    restoreWindowState();

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            handleWindowClose();
          }
        });
  }

  private void initLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      Logger.debug("Failed to set system look and feel: " + e.getMessage());
    }
  }

  private void initComponents() {
    setLayout(new BorderLayout());

    initMenuBar();
    initToolBar();
    initMainContent();
    initStatusBar();
  }

  private void initMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu arquivoMenu = new JMenu("Arquivo");
    JMenuItem sairItem = new JMenuItem("Sair");
    sairItem.addActionListener(e -> handleWindowClose());
    arquivoMenu.add(sairItem);
    menuBar.add(arquivoMenu);

    JMenu configuracoesMenu = new JMenu("Configuracoes");
    JMenuItem configItem = new JMenuItem("Configurar...");
    configItem.addActionListener(e -> openConfigDialog());
    configuracoesMenu.add(configItem);
    menuBar.add(configuracoesMenu);

    JMenu ajudaMenu = new JMenu("Ajuda");
    JMenuItem sobreItem = new JMenuItem("Sobre");
    sobreItem.addActionListener(e -> AboutDialog.showAbout(this));
    ajudaMenu.add(sobreItem);
    menuBar.add(ajudaMenu);

    setJMenuBar(menuBar);
  }

  private void initToolBar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);

    connectButton = new JButton("Conectar");
    connectButton.addActionListener(e -> connect());

    disconnectButton = new JButton("Desconectar");
    disconnectButton.addActionListener(e -> disconnect());
    disconnectButton.setEnabled(false);

    clearButton = new JButton("Limpar");
    clearButton.addActionListener(e -> chatPanel.clearMessages());

    toolBar.add(connectButton);
    toolBar.add(disconnectButton);
    toolBar.addSeparator();
    toolBar.add(clearButton);

    add(toolBar, BorderLayout.NORTH);
  }

  private void initMainContent() {
    chatPanel = new ChatPanel();
    chatPanel.setOnSendMessageListener(this::sendMessage);

    usersListPanel = new UsersListPanel();

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, usersListPanel);
    splitPane.setDividerLocation(500);
    splitPane.setResizeWeight(1.0);

    add(splitPane, BorderLayout.CENTER);
  }

  private void initStatusBar() {
    JPanel statusBar = new JPanel(new BorderLayout(5, 0));
    statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

    statusLabel = new JLabel("Desconectado");
    statusLabel.setForeground(Color.RED);

    groupInfoLabel = new JLabel("Grupo: " + config.getMulticastGroup() + ":" + config.getPort());

    statusBar.add(statusLabel, BorderLayout.WEST);
    statusBar.add(groupInfoLabel, BorderLayout.EAST);

    add(statusBar, BorderLayout.SOUTH);
  }

  private void loadConfig() {
    try {
      config.load();
    } catch (Exception e) {
      Logger.debug("Failed to load config: " + e.getMessage());
    }
    chatPanel.setUsername(config.getUsername());
  }

  private void restoreWindowState() {
    if (config.getWindowWidth() > 0 && config.getWindowHeight() > 0) {
      setSize(config.getWindowWidth(), config.getWindowHeight());
    } else {
      setSize(800, 600);
    }

    if (config.getWindowX() >= 0 && config.getWindowY() >= 0) {
      setLocation(config.getWindowX(), config.getWindowY());
    } else {
      setLocationRelativeTo(null);
    }
  }

  private void saveWindowState() {
    config.setWindowWidth(getWidth());
    config.setWindowHeight(getHeight());
    config.setWindowX(getX());
    config.setWindowY(getY());

    try {
      config.save();
    } catch (Exception e) {
      Logger.debug("Failed to save config: " + e.getMessage());
    }
  }

  private void openConfigDialog() {
    ConfigDialog dialog = new ConfigDialog(this, config);
    dialog.setVisible(true);
    if (dialog.isSaved()) {
      chatPanel.setUsername(config.getUsername());
      updateGroupInfo();
    }
  }

  private void connect() {
    if (!config.validate()) {
      JOptionPane.showMessageDialog(
          this,
          "Configuracoes invalidas. Por favor, configure o aplicativo.",
          "Erro de Configuracao",
          JOptionPane.ERROR_MESSAGE);
      openConfigDialog();
      return;
    }

    try {
      UDPNetworkManager.getInstance().createSocket(config.getPort(), config.getMulticastGroup());

      chatSession =
          new ChatSession(
              config.getUsername(), config.getMulticastGroup(), config.getPort(), config.getTtl());

      chatSession.join();

      connected = true;
      updateConnectionState(true);

      startReceiverThread();

    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this,
          "Falha ao conectar: " + e.getMessage(),
          "Erro de Conexao",
          JOptionPane.ERROR_MESSAGE);
      Logger.error("Connection error: " + e.getMessage());
    }
  }

  private void startReceiverThread() {
    Thread receiverThread =
        new Thread(
            () -> {
              try {
                var networkManager = UDPNetworkManager.getInstance();
                var socket = networkManager.getSocket();
                var multicastGroup = networkManager.getMulticastGroup();

                byte[] buffer = new byte[65536];
                var packet = new java.net.DatagramPacket(buffer, buffer.length);

                while (connected && socket != null && !socket.isClosed()) {
                  try {
                    socket.receive(packet);
                    String json =
                        new String(
                            packet.getData(),
                            0,
                            packet.getLength(),
                            java.nio.charset.StandardCharsets.UTF_8);

                    chat.network.ProtocolHandler handler = new chat.network.ProtocolHandler();
                    handler.process(packet);

                    var message = chat.util.JsonUtils.fromJson(json);
                    if (message != null) {
                      SwingUtilities.invokeLater(() -> chatPanel.appendMessage(message));
                    }
                  } catch (java.net.SocketTimeoutException e) {
                  } catch (Exception e) {
                    if (connected) {
                      Logger.debug("Receiver error: " + e.getMessage());
                    }
                  }
                }
              } catch (Exception e) {
                Logger.debug("Receiver thread error: " + e.getMessage());
              }
            },
            "MessageReceiver");
    receiverThread.setDaemon(true);
    receiverThread.start();
  }

  private void sendMessage() {
    String text = chatPanel.getInputText().trim();
    if (!text.isEmpty() && connected && chatSession != null) {
      try {
        chatSession.send(text);
        chatPanel.clearInput();
      } catch (Exception e) {
        Logger.error("Failed to send message: " + e.getMessage());
      }
    }
  }

  private void disconnect() {
    if (chatSession != null) {
      try {
        chatSession.leave();
      } catch (Exception e) {
        Logger.debug("Error during disconnect: " + e.getMessage());
      }
      chatSession = null;
    }

    try {
      UDPNetworkManager.getInstance().shutdown();
    } catch (Exception e) {
      Logger.debug("Error shutting down network: " + e.getMessage());
    }

    connected = false;
    updateConnectionState(false);
  }

  private void updateConnectionState(boolean connected) {
    connectButton.setEnabled(!connected);
    disconnectButton.setEnabled(connected);
    chatPanel.setConnected(connected);

    if (connected) {
      statusLabel.setText("Conectado");
      statusLabel.setForeground(new Color(76, 175, 80));
    } else {
      statusLabel.setText("Desconectado");
      statusLabel.setForeground(Color.RED);
      usersListPanel.clear();
    }
  }

  private void updateGroupInfo() {
    groupInfoLabel.setText("Grupo: " + config.getMulticastGroup() + ":" + config.getPort());
  }

  private void handleWindowClose() {
    if (connected) {
      int result =
          JOptionPane.showConfirmDialog(
              this,
              "Deseja desconectar antes de sair?",
              "Confirmar Saida",
              JOptionPane.YES_NO_CANCEL_OPTION);

      if (result == JOptionPane.YES_OPTION) {
        disconnect();
      } else if (result == JOptionPane.CANCEL_OPTION) {
        return;
      }
    }

    saveWindowState();
    System.exit(0);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          MainFrame frame = new MainFrame();
          frame.setVisible(true);
        });
  }
}
