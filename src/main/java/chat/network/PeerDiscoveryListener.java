package chat.network;

import chat.model.Peer;
import java.util.List;

public interface PeerDiscoveryListener {
  void onPeerJoined(Peer peer);

  void onPeerLeft(Peer peer);

  void onPeerUpdated(Peer peer);

  void onPeerListChanged(List<Peer> peers);
}
