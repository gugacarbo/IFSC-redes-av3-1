package chat.view;

import chat.config.AppConfig;
import chat.controller.ChatControllerImpl;
import chat.controller.MessageListener;
import chat.model.ChatMessage;
import chat.util.Logger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
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
  private JLabel errorLabel;
  private JLabel statusLabel;
  private JLabel groupInfoLabel;
  private JButton connectButton;
  private JButton disconnectButton;
  private JButton clearButton;

  private final ChatControllerImpl controller;
  private boolean connected = false;

  public MainFrame() {
    this.config = new AppConfig();
    this.controller = new ChatControllerImpl();

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
    controller.setChatPanel(chatPanel);
    controller.setUsersListPanel(usersListPanel);
    controller.addMessageListener(
        new MessageListener() {
          @Override
          public void onMessageReceived(ChatMessage message) {}

          @Override
          public void onMessageSent(ChatMessage message) {}

          @Override
          public void onSystemMessage(String message) {
            SwingUtilities.invokeLater(() -> updateErrorBanner(message));
          }
        });

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, usersListPanel);
    splitPane.setDividerLocation(500);
    splitPane.setResizeWeight(1.0);

    add(splitPane, BorderLayout.CENTER);
  }

  private void initStatusBar() {
    JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

    errorLabel = new JLabel(" ");
    errorLabel.setOpaque(true);
    errorLabel.setVisible(false);
    errorLabel.setForeground(new Color(183, 28, 28));
    errorLabel.setBackground(new Color(255, 235, 238));
    errorLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

    statusLabel = new JLabel("Desconectado");
    statusLabel.setForeground(Color.RED);

    groupInfoLabel = new JLabel("Grupo: " + config.getMulticastGroup() + ":" + config.getPort());

    JPanel statusBar = new JPanel(new BorderLayout(5, 0));
    statusBar.add(statusLabel, BorderLayout.WEST);
    statusBar.add(groupInfoLabel, BorderLayout.EAST);

    bottomPanel.add(errorLabel, BorderLayout.NORTH);
    bottomPanel.add(statusBar, BorderLayout.SOUTH);

    add(bottomPanel, BorderLayout.SOUTH);
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
    if (connected || controller.isConnected()) {
      return;
    }

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
      controller.connect();
      connected = controller.isConnected();
      updateConnectionState(connected);
    } catch (Exception e) {
      connected = false;
      updateConnectionState(false);
      JOptionPane.showMessageDialog(
          this,
          "Falha ao conectar: " + e.getMessage(),
          "Erro de Conexao",
          JOptionPane.ERROR_MESSAGE);
      Logger.error("Connection error: " + e.getMessage());
    }
  }

  private void sendMessage() {
    String text = chatPanel.getInputText().trim();
    if (!text.isEmpty() && connected) {
      controller.sendMessage(text);
      chatPanel.clearInput();
    }
  }

  private void disconnect() {
    controller.disconnect();
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

  private void updateErrorBanner(String message) {
    if (message == null || message.trim().isEmpty()) {
      clearErrorBanner();
      return;
    }

    String normalized = message.toLowerCase(Locale.ROOT);
    boolean isError =
        normalized.startsWith("erro")
            || normalized.contains("falha")
            || normalized.contains("nao conectado")
            || normalized.contains("não conectado");

    if (isError) {
      errorLabel.setText(message);
      errorLabel.setVisible(true);
      errorLabel.revalidate();
      errorLabel.repaint();
    } else if (normalized.startsWith("conectado") || normalized.startsWith("desconectado")) {
      clearErrorBanner();
    }
  }

  private void clearErrorBanner() {
    errorLabel.setText(" ");
    errorLabel.setVisible(false);
    errorLabel.revalidate();
    errorLabel.repaint();
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
    controller.shutdown();
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
