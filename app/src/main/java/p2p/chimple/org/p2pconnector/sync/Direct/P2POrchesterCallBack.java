package p2p.chimple.org.p2pconnector.sync.Direct;

import android.net.wifi.p2p.WifiP2pGroup;

import p2p.chimple.org.p2pconnector.sync.SyncUtils;

public interface P2POrchesterCallBack {

    public void Connected(String address, boolean isGroupOwner);

    public void GroupInfoChanged(WifiP2pGroup group);

    public void ConnectionStateChanged(SyncUtils.ConnectionState newState);

    public void ListeningStateChanged(SyncUtils.ReportingState newState);
}