package chat.view;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {

    private static final String APP_NAME = "UDP Chat P2P";
    private static final String VERSION = "1.0.0";
    private static final String DESCRIPTION = "Aplicacao de chat peer-to-peer usando UDP multicast";
    private static final String LICENSE = "Licenca: MIT";

    public AboutDialog(Frame parent) {
        super(parent, "Sobre", true);
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        gbc.gridwidth = 2;
        contentPanel.add(iconLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel(APP_NAME);
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(nameLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel versionLabel = new JLabel("Versao: " + VERSION);
        versionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(versionLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JTextArea descArea = new JTextArea(DESCRIPTION);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(descArea, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel licenseLabel = new JLabel(LICENSE);
        licenseLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        licenseLabel.setForeground(Color.GRAY);
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPanel.add(licenseLabel, gbc);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);

        pack();
        setMinimumSize(new Dimension(350, 250));
    }

    public static void showAbout(Frame parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            AboutDialog.showAbout(frame);
            System.exit(0);
        });
    }
}
