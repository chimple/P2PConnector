package p2p.chimple.org.p2pconnector;


import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.List;

import kotlin.jvm.JvmField;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class P2PSyncDBInstrumentedTest {
    private AppDatabase database;
    private P2PSyncInfoDao p2PSyncInfoDao;

    @Rule
    @JvmField
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        try {
            database = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                    .allowMainThreadQueries()
                    .build();

        } catch (Exception ex) {
            Log.i("test", ex.getMessage());
        }

        p2PSyncInfoDao = database.p2pSyncDao();
    }

    @Test
    public void testAddingAndRetrievingData() {
        P2PSyncInfo info = new P2PSyncInfo();
        info.setDeviceId("A");
        info.setUserId("1");
        info.setRecipientUserId("2");
        info.setMessageType("Chat");
        info.setMessage("Good Morning");
        info.setSequence(1L);
        p2PSyncInfoDao.insertP2PSyncInfo(info);

        List<P2PUserIdMessage> messages = p2PSyncInfoDao.fetchLatestMessagesByMessageType("Chat");
        assertEquals(messages.size(), 1);
    }

    @After
    public void tearDown() {
        database.close();
    }
}
