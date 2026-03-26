package chat.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import chat.config.AppConfig;
import chat.network.UDPNetworkManager;

public class ConfigDialog extends JDialog {
  private final AppConfig config;
  private JTextField usernameField;
  private JTextField multicastGroupField;
  private JTextField portField;
  private JTextField ttlField;
  private boolean saved = false;

  public ConfigDialog(Frame parent, AppConfig config) {
    super(parent, "Configuracoes", true);
    this.config = config;
    setModal(true);
    setResizable(false);
    initComponents();
    loadCurrentValues();
    pack();
    setLocationRelativeTo(parent);
  }

  private void initComponents() {
    JPanel formPanel = new JPanel(new GridBagLayout());
    formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Usuario:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    usernameField = new JTextField(20);
    formPanel.add(usernameField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Grupo Multicast:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    multicastGroupField = new JTextField(20);
    formPanel.add(multicastGroupField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Porta:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    portField = new JTextField(20);
    formPanel.add(portField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.weightx = 0;
    formPanel.add(new JLabel("TTL:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    ttlField = new JTextField(20);
    formPanel.add(ttlField, gbc);

    add(formPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

    JButton saveButton = new JButton("Salvar");
    saveButton.addActionListener(this::onSave);
    buttonPanel.add(saveButton);

    JButton testButton = new JButton("Testar Conexao");
    testButton.addActionListener(this::onTest);
    buttonPanel.add(testButton);

    JButton cancelButton = new JButton("Cancelar");
    cancelButton.addActionListener(e -> dispose());
    buttonPanel.add(cancelButton);

    add(buttonPanel, BorderLayout.SOUTH);

    getRootPane().setDefaultButton(saveButton);
  }

  private void loadCurrentValues() {
    usernameField.setText(config.getUsername());
    multicastGroupField.setText(config.getMulticastGroup());
    portField.setText(String.valueOf(config.getPort()));
    ttlField.setText(String.valueOf(config.getTtl()));
  }

  private boolean validateInputs() {
    String username = usernameField.getText().trim();
    String multicastGroup = multicastGroupField.getText().trim();
    String portStr = portField.getText().trim();
    String ttlStr = ttlField.getText().trim();

    if (username.isEmpty()) {
      showError("Usuario nao pode estar vazio.");
      return false;
    }

    if (!AppConfig.isValidMulticastIp(multicastGroup)) {
      showError("Grupo multicast invalido. Use um endereco entre 224.0.0.0 e 239.255.255.255.");
      return false;
    }

    int port;
    try {
      port = Integer.parseInt(portStr);
      if (!AppConfig.isValidPort(port)) {
        showError("Porta invalida. Use um valor entre 1024 e 65535.");
        return false;
      }
    } catch (NumberFormatException e) {
      showError("Porta deve ser um numero.");
      return false;
    }

    int ttl;
    try {
      ttl = Integer.parseInt(ttlStr);
      if (!AppConfig.isValidTtl(ttl)) {
        showError("TTL invalido. Use um valor entre 1 e 255.");
        return false;
      }
    } catch (NumberFormatException e) {
      showError("TTL deve ser um numero.");
      return false;
    }

    return true;
  }

  private void onSave(ActionEvent e) {
    if (validateInputs()) {
      config.setUsername(usernameField.getText().trim());
      config.setMulticastGroup(multicastGroupField.getText().trim());
      config.setPort(Integer.parseInt(portField.getText().trim()));
      config.setTtl(Integer.parseInt(ttlField.getText().trim()));

      try {
        config.save();
        saved = true;
        dispose();
      } catch (Exception ex) {
        showError("Falha ao salvar configuracao: " + ex.getMessage());
      }
    }
  }

  private void onTest(ActionEvent e) {
    if (!validateInputs()) {
      return;
    }

    String multicastGroup = multicastGroupField.getText().trim();
    int port = Integer.parseInt(portField.getText().trim());

    UDPNetworkManager nm = UDPNetworkManager.getInstance();
    if (nm.isActive() && nm.getPort() == port) {
      JOptionPane.showMessageDialog(
          this,
          "O aplicativo ja esta conectado nesta porta. Desconecte primeiro para testar.",
          "Teste de Conexao",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    JButton testConnectionButton = (JButton) e.getSource();
    testConnectionButton.setEnabled(false);
    testConnectionButton.setText("Testando...");

    new Thread(
            () -> {
              boolean success = false;
              String message;

              try (MulticastSocket testSocket = new MulticastSocket(port)) {
                InetAddress address = InetAddress.getByName(multicastGroup);
                testSocket.joinGroup(new InetSocketAddress(address, 0), null);
                testSocket.setSoTimeout(3000);

                byte[] testData = "TEST".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                var testPacket = new java.net.DatagramPacket(testData, testData.length, address, port);
                testSocket.send(testPacket);

                byte[] buffer = new byte[1024];
                var response = new java.net.DatagramPacket(buffer, buffer.length);
                testSocket.receive(response);
                success = true;
                message = "Conexao bem sucedida!";
              } catch (java.net.SocketTimeoutException ex) {
                success = true;
                message = "Conexao bem sucedida (timeout ao esperar resposta)";
              } catch (Exception ex) {
                message = "Falha na conexao: " + ex.getMessage();
              }

              final boolean finalSuccess = success;
              final String finalMessage = message;
              SwingUtilities.invokeLater(
                  () -> {
                    testConnectionButton.setEnabled(true);
                    testConnectionButton.setText("Testar Conexao");
                    if (finalSuccess) {
                      JOptionPane.showMessageDialog(
                          ConfigDialog.this,
                          finalMessage,
                          "Teste de Conexao",
                          JOptionPane.INFORMATION_MESSAGE);
                    } else {
                      JOptionPane.showMessageDialog(
                          ConfigDialog.this,
                          finalMessage,
                          "Teste de Conexao",
                          JOptionPane.ERROR_MESSAGE);
                    }
                  });
            })
        .start();
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Erro de Validacao", JOptionPane.ERROR_MESSAGE);
  }

  public boolean isSaved() {
    return saved;
  }
}
