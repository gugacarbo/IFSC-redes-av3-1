package chat.view;

import chat.config.AppConfig;
import chat.model.ChatMessage;
import chat.model.MessageType;
import chat.util.JsonUtils;
import chat.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class ConfigPanel extends JDialog {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final int TEST_TIMEOUT_MS = 3000;

    private final AppConfig config;
    private final JTextField usernameField;
    private final JTextField multicastGroupField;
    private final JTextField portField;
    private final JTextField ttlField;
    private final JLabel errorLabel;

    public ConfigPanel(Frame parent, AppConfig config) {
        super(parent, "Configurações", true);
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        usernameField = new JTextField(config.getUsername(), 20);
        multicastGroupField = new JTextField(config.getMulticastGroup(), 20);
        portField = new JTextField(String.valueOf(config.getPort()), 20);
        ttlField = new JTextField(String.valueOf(config.getTtl()), 20);
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Multicast Group:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(multicastGroupField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("TTL (1-255):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        formPanel.add(ttlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(errorLabel, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton testButton = new JButton("Testar Conexão");
        JButton saveButton = new JButton("Salvar");
        JButton cancelButton = new JButton("Cancelar");

        testButton.addActionListener(e -> testConnection());
        saveButton.addActionListener(e -> saveConfig());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void setError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private boolean validateInputs() {
        String username = usernameField.getText().trim();
        String multicastGroup = multicastGroupField.getText().trim();
        String portStr = portField.getText().trim();
        String ttlStr = ttlField.getText().trim();

        if (username.isEmpty()) {
            setError("Username não pode estar vazio");
            usernameField.requestFocusInWindow();
            return false;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            setError("Username deve ter 3-20 caracteres alfanuméricos");
            usernameField.requestFocusInWindow();
            return false;
        }

        if (!AppConfig.isValidMulticastIp(multicastGroup)) {
            setError("Endereço multicast inválido (224.0.0.0-239.255.255.255)");
            multicastGroupField.requestFocusInWindow();
            return false;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (!AppConfig.isValidPort(port)) {
                setError("Porta deve estar entre 1024 e 65535");
                portField.requestFocusInWindow();
                return false;
            }
        } catch (NumberFormatException e) {
            setError("Porta deve ser um número válido");
            portField.requestFocusInWindow();
            return false;
        }

        int ttl;
        try {
            ttl = Integer.parseInt(ttlStr);
            if (!AppConfig.isValidTtl(ttl)) {
                setError("TTL deve estar entre 1 e 255");
                ttlField.requestFocusInWindow();
                return false;
            }
        } catch (NumberFormatException e) {
            setError("TTL deve ser um número válido");
            ttlField.requestFocusInWindow();
            return false;
        }

        clearError();
        return true;
    }

    private void saveConfig() {
        if (!validateInputs()) {
            return;
        }

        config.setUsername(usernameField.getText().trim());
        config.setMulticastGroup(multicastGroupField.getText().trim());
        config.setPort(Integer.parseInt(portField.getText().trim()));
        config.setTtl(Integer.parseInt(ttlField.getText().trim()));

        try {
            config.save();
            Logger.info("Configuração salva com sucesso");
            dispose();
        } catch (Exception e) {
            setError("Erro ao salvar configuração: " + e.getMessage());
            Logger.error("Erro ao salvar configuração", e);
        }
    }

    private void testConnection() {
        if (!validateInputs()) {
            return;
        }

        String multicastGroup = multicastGroupField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        int ttl = Integer.parseInt(ttlField.getText().trim());

        setError("Testando conexão...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<>() {
            private String resultMessage;
            private boolean success;

            @Override
            protected Void doInBackground() {
                AtomicBoolean received = new AtomicBoolean(false);
                AtomicReference<String> error = new AtomicReference<>();

                try (MulticastSocket socket = new MulticastSocket()) {
                    socket.setSoTimeout(1000);
                    socket.setTimeToLive(ttl);

                    InetAddress group = InetAddress.getByName(multicastGroup);
                    socket.joinGroup(group);

                    ChatMessage testMsg = new ChatMessage(
                            usernameField.getText().trim(),
                            "TEST",
                            MessageType.CHAT
                    );
                    String json = JsonUtils.toJson(testMsg);
                    byte[] buffer = json.getBytes("UTF-8");

                    DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, group, port);
                    socket.send(sendPacket);

                    long startTime = System.currentTimeMillis();
                    byte[] receiveBuffer = new byte[1024];

                    while (System.currentTimeMillis() - startTime < TEST_TIMEOUT_MS) {
                        try {
                            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                            socket.receive(receivePacket);

                            String receivedMsg = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                            if (receivedMsg.contains("TEST")) {
                                received.set(true);
                                break;
                            }
                        } catch (java.net.SocketTimeoutException e) {
                            break;
                        }
                    }

                    socket.leaveGroup(group);

                    if (received.get()) {
                        success = true;
                        resultMessage = "Conexão bem-sucedida!";
                    } else {
                        success = true;
                        resultMessage = "Socket criado com sucesso (sem echo)";
                    }

                } catch (Exception e) {
                    success = false;
                    resultMessage = "Erro: " + e.getMessage();
                    error.set(e.getMessage());
                    Logger.error("Teste de conexão falhou", e);
                }
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (success) {
                    clearError();
                    JOptionPane.showMessageDialog(ConfigPanel.this,
                            resultMessage,
                            "Testar Conexão",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    setError(resultMessage);
                }
            }
        }.execute();
    }
}