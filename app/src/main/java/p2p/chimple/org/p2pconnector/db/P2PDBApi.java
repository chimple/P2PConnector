package p2p.chimple.org.p2pconnector.db;

import java.util.Arrays;
import java.util.List;

import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncDeviceStatus;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdDeviceIdAndMessage;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;

public interface P2PDBApi {

    // API designed for Application

    List<P2PUserIdDeviceIdAndMessage> getUsers();

    List<P2PUserIdMessage> fetchLatestMessagesByMessageType(String messageType, List<String> userIds);

    public boolean addMessage(String userId, String recipientId, String messageType, String message);

    public boolean addMessage(String userId, String recipientId, String messageType, String message, Boolean status, String sessionId);

    public List<P2PSyncInfo> getConversations(String firstUserId, String secondUserId, String messageType);

    public List<P2PSyncInfo> getLatestConversations(String firstUserId, String secondUserId, String messageType);

    public void persistMessage(String userId, String deviceId, String recepientUserId, String message, String messageType);

    public String serializeHandShakingMessage();

    public void persistP2PSyncInfos(String p2pSyncJson);

    public String buildAllSyncMessages(String handShakeJson);

    public boolean upsertProfile();

    public void addDeviceToSync(String deviceId, boolean syncImmediately);

    public List<P2PSyncDeviceStatus> getAllSyncDevices();

    public List<P2PSyncDeviceStatus> getAllNonSyncDevices();

    public P2PSyncDeviceStatus getLatestDeviceToSync();

    public void syncCompleted(String deviceId);

    public P2PSyncDeviceStatus getLatestDeviceToSyncFromDevices(List<String> items);

    public boolean upsertProfileForUserIdAndDevice(String userId, String deviceId, String message);

    public List<P2PSyncInfo> getLatestConversationsByUser(String firstUserId);

    public void persistP2PSyncMessage(P2PSyncInfo message);

    public List<P2PSyncInfo> fetchByUserAndDeviceAndSequence(String userId, String deviceId, Long sequence);
}
