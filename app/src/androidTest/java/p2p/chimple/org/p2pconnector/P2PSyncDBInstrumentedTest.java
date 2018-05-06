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
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import kotlin.jvm.JvmField;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;

import static org.junit.Assert.assertEquals;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

@RunWith(AndroidJUnit4.class)
public class P2PSyncDBInstrumentedTest {
    private AppDatabase database;
    private P2PSyncInfoDao p2PSyncInfoDao;
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

        p2PSyncInfoDao = database.p2pSyncDao();
        p2pDBAPI = new P2PDBApiImpl(database, context.getApplicationContext());

        SharedPreferences pref = context.getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("USER_ID", UUID.randomUUID().toString());
        editor.putString("DEVICE_ID", UUID.randomUUID().toString());
        editor.putString("PROFILE_PHOTO", "photo1.jpg");
        editor.commit(); // commit changes

    }

    @Test
    public void fetchLatestMessagesByMessageTypeTest() {
        String userId1 = "User1";
        String deviceId = "deviceId";
        String recepientUserId = "recepientUserId";
        String message = "message";
        String messageType = "Chat";
        p2pDBAPI.persistMessage(userId1, deviceId, recepientUserId, message, messageType);

        String userId2 = "User2";
        p2pDBAPI.persistMessage(userId2, deviceId, recepientUserId, message, messageType);


        String userId3 = "User3";
        p2pDBAPI.persistMessage(userId3, deviceId, recepientUserId, message, messageType);


        String userId4 = "User4";
        p2pDBAPI.persistMessage(userId4, deviceId, recepientUserId, message, messageType);

        List<P2PUserIdMessage> messages = p2pDBAPI.fetchLatestMessagesByMessageType("Chat", null);
        assertEquals(messages.size(), 4);

        List<String> userIds = new ArrayList<String>();
        userIds.add(userId1);
        userIds.add(userId2);

        List<P2PUserIdMessage> messagesForUserIds = p2pDBAPI.fetchLatestMessagesByMessageType("Chat", userIds);
        assertEquals(messagesForUserIds.size(), 2);

    }

    @Test
    public void getConversationsTest() {
        String userId1 = "M1";
        String recepientUserId = "recepientUserId";
        String messageType = "Chat";

        String message = "message1";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message);

        message = "message2";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message);

        message = "message3";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message);

        message = "message4";
        p2pDBAPI.addMessage(recepientUserId, userId1, messageType, message);


        List<P2PSyncInfo> conversations = p2pDBAPI.getConversations(userId1, recepientUserId, "Chat");
        assertEquals(conversations.size(), 4);
    }


    @Test
    public void getLatestConversations() {
        String userId1 = "M1";
        String recepientUserId = "recepientUserId";
        String messageType = "Chat";

        String message = "message1";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message, true, "session1");

        message = "message2";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message, false, "session1");

        message = "message3";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message, true, "session1");


        message = "message5";
        p2pDBAPI.addMessage(userId1, recepientUserId, messageType, message, true, "session2");

        message = "message6";
        p2pDBAPI.addMessage(recepientUserId, userId1, messageType, message, false, "session3");


        List<P2PSyncInfo> conversations = p2pDBAPI.getLatestConversations(userId1, recepientUserId, "Chat");
        assertEquals(conversations.size(), 2);
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


    @Test
    public void getUsersTest() {
        P2PSyncInfo info = new P2PSyncInfo();
        info.setDeviceId("A");
        info.setUserId("1");
        info.setRecipientUserId("2");
        info.setMessageType("Chat");
        info.setMessage("Good Morning");
        info.setSequence(1L);
        p2PSyncInfoDao.insertP2PSyncInfo(info);

        P2PSyncInfo info1 = new P2PSyncInfo();
        info1.setDeviceId("B");
        info1.setUserId("2");
        info1.setRecipientUserId("2");
        info1.setMessageType("Chat");
        info1.setMessage("Good Morning");
        info1.setSequence(2L);
        p2PSyncInfoDao.insertP2PSyncInfo(info1);


        List<String> users = p2pDBAPI.getUsers();
        assertEquals(users.size(), 2);
    }


    @Test
    public void upsertProfileTest() {
        p2pDBAPI.upsertProfile();
        SharedPreferences pref = this.context.getSharedPreferences(P2P_SHARED_PREF, 0);
        String fileName = pref.getString("PROFILE_PHOTO", null); // getting String
        String userId = pref.getString("USER_ID", null); // getting String
        String deviceId = pref.getString("DEVICE_ID", null); // getting String

        P2PSyncInfo userInfo = p2PSyncInfoDao.getProfileByUserId(userId, P2PSyncManager.MessageTypes.PHOTO.type());
        assertEquals(userInfo.getUserId(), userId);
        assertEquals(userInfo.getMessage(), fileName);
        assertEquals(userInfo.getDeviceId(), deviceId);
        assertEquals(userInfo.getSequence().longValue(), 1);

        pref = context.getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("PROFILE_PHOTO", "photo2.jpg");
        editor.commit(); // commit changes

        p2pDBAPI.upsertProfile();
        userInfo = p2PSyncInfoDao.getProfileByUserId(userId, P2PSyncManager.MessageTypes.PHOTO.type());
        assertEquals(userInfo.getUserId(), userId);
        assertEquals(userInfo.getMessage(), "photo2.jpg");
        assertEquals(userInfo.getDeviceId(), deviceId);
        assertEquals(userInfo.getSequence().longValue(), 1);
    }

    @After
    public void tearDown() {
        database.close();
    }
}
