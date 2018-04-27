package p2p.chimple.org.p2pconnector.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import p2p.chimple.org.p2pconnector.db.entity.P2PLatestInfoByUserAndDevice;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;


@Dao
public interface P2PSyncInfoDao {
    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId")
    public P2PSyncInfo[] getSyncInformationByUserIdAndDeviceId(String userId, String deviceId);

    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId")
    public P2PSyncInfo[] getSyncInformationByUserId(String userId);

    @Query("SELECT MAX(sequence) FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId GROUP BY user_id, device_id")
    public Long getLatestSequenceAvailableByUserIdAndDeviceId(String userId, String deviceId);

    @Query("SELECT MAX(step) FROM P2PSyncInfo WHERE user_id=:userId AND session_id=:sessionId GROUP BY user_id, session_id")
    public Long getLatestStepForUserIdAndSessionId(String userId, String sessionId);


    @Query("SELECT user_id, device_id, MAX(sequence) as sequence FROM P2PSyncInfo GROUP BY user_id, device_id")
    public P2PLatestInfoByUserAndDevice[] getLatestInfoAvailableByUserIdAndDeviceId();


    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId AND sequence > :startingSequence and sequence <= :endingSequence")
    public P2PSyncInfo[] fetchByUserAndDeviceBetweenSequences(String userId, String deviceId, Long startingSequence, Long endingSequence);


    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId AND sequence <= :sequence")
    public P2PSyncInfo[] fetchByUserAndDeviceUpToSequence(String userId, String deviceId, Long sequence);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public Long insertP2PSyncInfo(P2PSyncInfo info);

    @Update
    public void updateP2PSyncInfo(P2PSyncInfo updateP2PSyncInfo);

    @Query("SELECT distinct(user_id) from P2PSyncInfo")
    public String[] fetchAllUsers();


    @Query("SELECT distinct(user_id) from P2PSyncInfo")
    public String[] fetchAllNeighours();

    @Query("SELECT tmp.user_id, ps.message from (SELECT user_id, max(sequence) as sequence FROM P2PSyncInfo  WHERE message_type = :messageType AND user_id in (:userIds) group by user_id)  as tmp, P2PSyncInfo ps where ps.user_id = tmp.user_id and ps.sequence = tmp.sequence")
    public List<P2PUserIdMessage> fetchLatestMessagesByMessageType1(String messageType, List<String> userIds);

    @Query("SELECT tmp.user_id, ps.message from (SELECT user_id, max(sequence) as sequence FROM P2PSyncInfo  WHERE message_type = :messageType group by user_id)  as tmp, P2PSyncInfo ps where ps.user_id = tmp.user_id and ps.sequence = tmp.sequence")
    public List<P2PUserIdMessage> fetchLatestMessagesByMessageType(String messageType);


    @Query("SELECT * FROM P2PSyncInfo WHERE message_type = :messageType AND ((user_id = :userId or recipient_user_id = :recipientId) or (user_id = :recipientId or recipient_user_id = :userId))")
    public List<P2PSyncInfo> fetchConversations(String userId, String recipientId, String messageType);

    @Query("SELECT p2p.* from (SELECT session_id, max(step) as step from P2PSyncInfo where message_type = :messageType and status = 1 group by session_id) tmp, P2PSyncInfo p2p where p2p.session_id = tmp.session_id and p2p.step = tmp.step and ((p2p.user_id = :userId or p2p.recipient_user_id = :recipientId) or (p2p.user_id = :userId or p2p.recipient_user_id = :recipientId))")
    public List<P2PSyncInfo> fetchLatestConversations(String userId, String recipientId, String messageType);

}
