package p2p.chimple.org.p2pconnector.sync;

import android.net.wifi.p2p.WifiP2pGroup;

public interface P2POrchesterCallBack {
    public void Connected(String address, boolean isGroupOwner);

    public void GroupInfoChanged(WifiP2pGroup group);

    public void ConnectionStateChanged(SyncUtils.ConnectionState newState);

    public void ListeningStateChanged(SyncUtils.ReportingState newState);
}
