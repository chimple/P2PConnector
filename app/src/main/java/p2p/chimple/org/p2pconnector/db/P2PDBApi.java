package p2p.chimple.org.p2pconnector.db;

import java.util.List;

import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

public interface P2PDBApi {
    public void persistMessage(String userId, String deviceId, String recepientUserId, String message, String messageType);

    public String serializeHandShakingMessage();

    public List<P2PSyncInfo> buildSyncInformation(List<HandShakingInfo> infos);
}
