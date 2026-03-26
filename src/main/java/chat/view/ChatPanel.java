package chat.view;

import chat.config.AppConfig;
import chat.model.ChatMessage;
import chat.model.MessageType;
import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.text.*;

public class ChatPanel extends JPanel {
  private static final int MAX_MESSAGE_LENGTH = 500;
  private static final Color OWN_MESSAGE_BG = new Color(0xE3, 0xF2, 0xFD);
  private static final Color SYSTEM_MESSAGE_COLOR = Color.GRAY;

  private final JTextPane messageArea;
  private final JTextField inputField;
  private final JButton sendButton;
  private final JButton clearButton;
  private final Document messageDocument;
  private Runnable onSendMessageListener;
  private String currentUsername;
  private boolean connected;

  public ChatPanel() {
    setLayout(new BorderLayout(10, 10));
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    AppConfig config = new AppConfig();
    try {
      config.load();
      this.currentUsername = config.getUsername();
    } catch (Exception e) {
      this.currentUsername = "";
    }
    this.connected = false;

    messageArea = new JTextPane();
    messageArea.setEditable(false);
    messageArea.setFocusable(false);
    messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    messageDocument = messageArea.getDocument();

    JScrollPane scrollPane = new JScrollPane(messageArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    add(scrollPane, BorderLayout.CENTER);

    JPanel inputPanel = new JPanel(new BorderLayout(5, 0));

    inputField = new JTextField();
    inputField.setDocument(
        new PlainDocument() {
          @Override
          public void insertString(int offs, String str, AttributeSet a)
              throws BadLocationException {
            if (getLength() + str.length() <= MAX_MESSAGE_LENGTH) {
              super.insertString(offs, str, a);
            }
          }
        });
    inputField.setEnabled(false);
    inputField.addKeyListener(
        new java.awt.event.KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
              dispatchSendMessage();
            }
          }
        });

    sendButton = new JButton("Enviar");
    sendButton.setEnabled(false);
    sendButton.addActionListener(e -> dispatchSendMessage());

    clearButton = new JButton("Limpar");
    clearButton.addActionListener(e -> clearChat());

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
    buttonPanel.add(sendButton);
    buttonPanel.add(clearButton);

    inputPanel.add(inputField, BorderLayout.CENTER);
    inputPanel.add(buttonPanel, BorderLayout.EAST);

    add(inputPanel, BorderLayout.SOUTH);
  }

  public void setOnSendMessageListener(Runnable listener) {
    this.onSendMessageListener = listener;
  }

  private void dispatchSendMessage() {
    if (!connected || onSendMessageListener == null) {
      return;
    }

    String text = inputField.getText().trim();
    if (!text.isEmpty()) {
      onSendMessageListener.run();
    }
  }

  private void clearChat() {
    int result =
        JOptionPane.showConfirmDialog(
            this,
            "Deseja realmente limpar todas as mensagens?",
            "Confirmar Limpeza",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      clearMessages();
    }
  }

  public void clearMessages() {
    SwingUtilities.invokeLater(
        () -> {
          try {
            messageDocument.remove(0, messageDocument.getLength());
          } catch (BadLocationException e) {
            // ignore
          }
        });
  }

  public void appendMessage(ChatMessage message) {
    SwingUtilities.invokeLater(
        () -> {
          try {
            String time = message.getTime();
            String username = message.getUsername();
            String content = message.getContent();

            boolean isOwnMessage =
                currentUsername != null
                    && currentUsername.equals(username)
                    && message.getType() == MessageType.CHAT;
            boolean isSystemMessage = message.getType() != MessageType.CHAT;

            Style style = messageArea.addStyle("message", null);
            StyleConstants.setFontFamily(style, Font.MONOSPACED);
            StyleConstants.setFontSize(style, 12);

            if (isSystemMessage) {
              StyleConstants.setForeground(style, SYSTEM_MESSAGE_COLOR);
              StyleConstants.setItalic(style, true);
              String systemText = "[Sistema] " + content + "\n";
              messageDocument.insertString(messageDocument.getLength(), systemText, style);
            } else if (isOwnMessage) {
              StyleConstants.setBackground(style, OWN_MESSAGE_BG);
              String formattedMessage = "[" + time + "] " + username + ": " + content + "\n";
              messageDocument.insertString(messageDocument.getLength(), formattedMessage, style);
            } else {
              String formattedMessage = "[" + time + "] " + username + ": " + content + "\n";
              messageDocument.insertString(messageDocument.getLength(), formattedMessage, style);
            }

            scrollToBottom();
          } catch (BadLocationException e) {
            // ignore
          }
        });
  }

  private void scrollToBottom() {
    SwingUtilities.invokeLater(
        () -> {
          messageArea.setCaretPosition(messageDocument.getLength());
        });
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
    SwingUtilities.invokeLater(
        () -> {
          inputField.setEnabled(connected);
          sendButton.setEnabled(connected);
        });
  }

  public void setUsername(String username) {
    this.currentUsername = username;
  }

  public String getInputText() {
    return inputField.getText();
  }

  public void clearInput() {
    inputField.setText("");
  }

  public void clear() {
    clearMessages();
  }
}
