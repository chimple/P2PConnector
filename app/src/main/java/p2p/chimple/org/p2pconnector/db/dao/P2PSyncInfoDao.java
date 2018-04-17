package p2p.chimple.org.p2pconnector.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import p2p.chimple.org.p2pconnector.db.entity.P2PLatestInfoByUserAndDevice;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;


@Dao
public interface P2PSyncInfoDao
{
    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId")
    public P2PSyncInfo[] getSyncInformationByUserIdAndDeviceId(String userId, String deviceId);

    @Query("SELECT * FROM P2PSyncInfo WHERE user_id=:userId")
    public P2PSyncInfo[] getSyncInformationByUserId(String userId);

    @Query("SELECT MAX(sequence) FROM P2PSyncInfo WHERE user_id=:userId AND device_id=:deviceId GROUP BY user_id, device_id")
    public Long getLatestSequenceAvailableByUserIdAndDeviceId(String userId, String deviceId);

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

}
