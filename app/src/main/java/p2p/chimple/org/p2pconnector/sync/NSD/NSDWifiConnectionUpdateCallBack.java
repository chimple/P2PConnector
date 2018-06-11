package p2p.chimple.org.p2pconnector.sync.NSD;

import java.net.InetAddress;
import java.util.List;

import p2p.chimple.org.p2pconnector.sync.SyncUtils;

public interface NSDWifiConnectionUpdateCallBack {
    public void Connected(InetAddress remote, boolean ListeningStill);
    public void processServiceList(List<NSDSyncService> list);
    public void serviceUpdateStatus(SyncUtils.DiscoveryState newState);
}
