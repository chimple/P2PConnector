package p2p.chimple.org.p2pconnector.sync;

import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WifiConnectionUpdateCallBack {
    public void handleWifiP2PStateChange(int state);

    public void handleWifiP2PConnectionChange(NetworkInfo networkInfo);

    public boolean gotPeersList(Collection<WifiP2pDevice> list);

    public void gotServicesList(List<WifiDirectService> list);

    public Map<String, WifiDirectService> foundNeighboursList(List<WifiDirectService> list);

    public void GroupInfoAvailable(WifiP2pGroup group);

    public void connectionStatusChanged(SyncUtils.SyncHandShakeState state, NetworkInfo.DetailedState detailedState, int Error, WifiDirectService currentDevice);

    public void Connected(InetAddress remote, boolean ListeningStill);

}
