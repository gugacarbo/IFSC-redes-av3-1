package chat.view;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("UDP Chat P2P");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        initMenuBar();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu arquivoMenu = new JMenu("Arquivo");
        JMenuItem sairItem = new JMenuItem("Sair");
        sairItem.addActionListener(e -> System.exit(0));
        arquivoMenu.add(sairItem);

        JMenu configuracoesMenu = new JMenu("Configuracoes");
        JMenuItem configItem = new JMenuItem("Configurar...");
        configuracoesMenu.add(configItem);

        JMenu ajudaMenu = new JMenu("Ajuda");
        JMenuItem sobreItem = new JMenuItem("Sobre");
        sobreItem.addActionListener(e -> AboutDialog.showAbout(this));
        ajudaMenu.add(sobreItem);

        menuBar.add(arquivoMenu);
        menuBar.add(configuracoesMenu);
        menuBar.add(ajudaMenu);

        setJMenuBar(menuBar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
