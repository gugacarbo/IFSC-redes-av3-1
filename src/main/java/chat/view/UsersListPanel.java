package chat.view;

import chat.model.Peer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class UsersListPanel extends JPanel {
  private final JList<Peer> peerList;
  private final DefaultListModel<Peer> listModel;

  public UsersListPanel() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 5, 5));
    setMinimumSize(new Dimension(180, 200));
    setPreferredSize(new Dimension(200, 400));

    JLabel titleLabel = new JLabel("Usuários Conectados");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
    add(titleLabel);

    listModel = new DefaultListModel<>();
    peerList = new JList<>(listModel);
    peerList.setCellRenderer(new PeerCellRenderer());
    peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane scrollPane = new JScrollPane(peerList);
    scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(scrollPane);
  }

  public void addPeer(Peer peer) {
    if (!listModel.contains(peer)) {
      listModel.addElement(peer);
    }
  }

  public void removePeer(Peer peer) {
    listModel.removeElement(peer);
  }

  public void updatePeerStatus(Peer peer) {
    int index = listModel.indexOf(peer);
    if (index != -1) {
      listModel.set(index, peer);
    }
  }

  public void refreshList() {
    peerList.repaint();
  }

  public void clear() {
    listModel.clear();
  }

  private class PeerCellRenderer implements ListCellRenderer<Peer> {
    @Override
    public Component getListCellRendererComponent(
        JList<? extends Peer> list,
        Peer peer,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
      panel.setBorder(new EmptyBorder(2, 5, 2, 5));

      JLabel statusIndicator = new JLabel();
      statusIndicator.setPreferredSize(new Dimension(12, 12));
      if (peer.isActive()) {
        statusIndicator.setBackground(new Color(76, 175, 80));
      } else {
        statusIndicator.setBackground(new Color(158, 158, 158));
      }
      statusIndicator.setOpaque(true);
      statusIndicator.setBorder(new LineBorder(Color.DARK_GRAY, 1));

      String displayText =
          peer.getUsername()
              + " ("
              + peer.getAddress().getHostAddress()
              + ":"
              + peer.getPort()
              + ")";
      JLabel textLabel = new JLabel(displayText);
      textLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

      if (!peer.isActive()) {
        textLabel.setForeground(Color.GRAY);
      }

      panel.add(statusIndicator);
      panel.add(textLabel);

      if (isSelected) {
        panel.setBackground(list.getSelectionBackground());
        textLabel.setForeground(list.getSelectionForeground());
      }

      return panel;
    }
  }
}
