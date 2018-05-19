package p2p.chimple.org.p2pconnector;


import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kotlin.jvm.JvmField;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncDeviceStatusDao;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncDeviceStatus;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;

import static org.junit.Assert.assertEquals;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

@RunWith(AndroidJUnit4.class)
public class P2PSyncDeviceStatusInstrumentedTest {
    private AppDatabase database;
    private P2PSyncDeviceStatusDao p2pSyncDeviceStatusDao;
    private P2PDBApiImpl p2pDBAPI = null;
    private Context context;
    @Rule
    @JvmField
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        try {
            database = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                    .allowMainThreadQueries()
                    .build();

        } catch (Exception ex) {
            Log.i("test", ex.getMessage());
        }

        p2pSyncDeviceStatusDao = database.p2pSyncDeviceStatusDao();
        p2pDBAPI = new P2PDBApiImpl(database, context.getApplicationContext());
    }

    @Test
    public void syncTests() {
        p2pDBAPI.addDeviceToSync("deviceA", false);
        p2pDBAPI.addDeviceToSync("deviceB", false);
        p2pDBAPI.addDeviceToSync("deviceC", false);
        p2pDBAPI.addDeviceToSync("deviceD", false);

        List<P2PSyncDeviceStatus> devices = p2pDBAPI.getAllNonSyncDevices();
        assertEquals(devices.size(), 4);
        P2PSyncDeviceStatus topDevice = p2pDBAPI.getLatestDeviceToSync();
        assertEquals(topDevice.deviceId, "deviceA");

        //update couple of them
        p2pDBAPI.syncCompleted("deviceA");
        p2pDBAPI.syncCompleted("deviceB");

        devices = p2pDBAPI.getAllNonSyncDevices();
        assertEquals(devices.size(), 2);
        topDevice = p2pDBAPI.getLatestDeviceToSync();
        assertEquals(topDevice.deviceId, "deviceC");

        p2pDBAPI.addDeviceToSync("deviceE", true);
        p2pDBAPI.addDeviceToSync("deviceF", true);
        devices = p2pDBAPI.getAllNonSyncDevices();
        assertEquals(devices.size(), 4);
        topDevice = p2pDBAPI.getLatestDeviceToSync();
        assertEquals(topDevice.deviceId, "deviceE");

        p2pDBAPI.syncCompleted("deviceE");


        devices = p2pDBAPI.getAllNonSyncDevices();
        assertEquals(devices.size(), 3);
        topDevice = p2pDBAPI.getLatestDeviceToSync();
        assertEquals(topDevice.deviceId, "deviceF");

        p2pDBAPI.syncCompleted("deviceF");

        devices = p2pDBAPI.getAllNonSyncDevices();
        assertEquals(devices.size(), 2);
        p2pDBAPI.syncCompleted("deviceC");
        topDevice = p2pDBAPI.getLatestDeviceToSync();
        assertEquals(topDevice.deviceId, "deviceD");
    }


    @After
    public void tearDown() {
        database.close();
    }
}
