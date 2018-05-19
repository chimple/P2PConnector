package p2p.chimple.org.p2pconnector.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import p2p.chimple.org.p2pconnector.db.entity.P2PLatestInfoByUserAndDevice;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncDeviceStatus;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

@Dao
public interface P2PSyncDeviceStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public Long insertP2PSyncDeviceStatus(P2PSyncDeviceStatus info);

    @Query("SELECT * FROM P2PSyncDeviceStatus WHERE sync_time is not null order by discover_time asc")
    public P2PSyncDeviceStatus[] getAllSyncDevices();

    @Query("SELECT * FROM P2PSyncDeviceStatus WHERE sync_time is null order by discover_time asc")
    public P2PSyncDeviceStatus[] getAllNotSyncDevices();

    @Query("SELECT * FROM P2PSyncDeviceStatus WHERE sync_time is null and sync_immediately = 1 order by discover_time asc limit 1")
    public P2PSyncDeviceStatus getTopDeviceToSyncImmediately();

    @Query("SELECT * FROM P2PSyncDeviceStatus WHERE sync_time is null and (sync_immediately  = 0 or sync_immediately is null) order by discover_time asc limit 1")
    public P2PSyncDeviceStatus getTopDeviceToNotSyncImmediately();

    @Query("SELECT * FROM P2PSyncDeviceStatus WHERE sync_time is null and (sync_immediately  = 0 or sync_immediately is null) order by discover_time asc")
    public P2PSyncDeviceStatus[] getAllDevicesToNotSyncImmediately();


}
