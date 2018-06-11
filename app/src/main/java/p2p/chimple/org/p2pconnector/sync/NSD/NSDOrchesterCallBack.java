package p2p.chimple.org.p2pconnector.sync.NSD;
import java.util.List;

import p2p.chimple.org.p2pconnector.sync.SyncUtils;

public interface NSDOrchesterCallBack {
    public void NSDDiscovertyStateChanged(SyncUtils.DiscoveryState newState);
    public void NSDConnectionStateChanged(SyncUtils.ConnectionState newState);
    public void NSDListeningStateChanged(SyncUtils.ReportingState newState);
    public void NSDConnected(String address, boolean isOwner);
}
