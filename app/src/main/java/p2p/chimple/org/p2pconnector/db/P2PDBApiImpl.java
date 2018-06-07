package p2p.chimple.org.p2pconnector.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;


import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfo;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfoDeserializer;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingMessage;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingMessageDeserializer;
import p2p.chimple.org.p2pconnector.db.entity.P2PLatestInfoByUserAndDevice;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncDeviceStatus;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdDeviceIdAndMessage;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;
import p2p.chimple.org.p2pconnector.db.entity.ProfileMessage;
import p2p.chimple.org.p2pconnector.db.entity.ProfileMessageDeserializer;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

public class P2PDBApiImpl implements P2PDBApi {
    private static final String TAG = P2PDBApiImpl.class.getName();
    private AppDatabase db;
    private Context context;
    private static P2PDBApiImpl p2pDBApiInstance;

    public static P2PDBApiImpl getInstance(Context context) {
        synchronized (P2PDBApiImpl.class) {
            if (p2pDBApiInstance == null) {
                p2pDBApiInstance = new P2PDBApiImpl(AppDatabase.getInstance(context), context);
            }
            return p2pDBApiInstance;
        }
    }


    private P2PDBApiImpl(AppDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    public void persistMessage(String userId, String deviceId, String recepientUserId, String message, String messageType) {
        Long maxSequence = db.p2pSyncDao().getLatestSequenceAvailableByUserIdAndDeviceId(userId, deviceId);
        if (maxSequence == null) {
            maxSequence = 0L;
        }

        maxSequence++;
        P2PSyncInfo info = new P2PSyncInfo(userId, deviceId, maxSequence, recepientUserId, message, messageType);
        db.p2pSyncDao().insertP2PSyncInfo(info);
        Log.i(TAG, "inserted data" + info);
    }

    private void persistP2PSyncMessage(P2PSyncInfo message) {
        db.p2pSyncDao().insertP2PSyncInfo(message);
        Log.i(TAG, "got Sync info:" + message.deviceId);
        Log.i(TAG, "got Sync info:" + message.userId);
        Log.i(TAG, "got Sync info:" + message.message);
        Log.i(TAG, "got Sync info:" + message.messageType);
        Log.i(TAG, "got Sync info:" + message.sequence);
        Log.i(TAG, "got Sync info:" + message.recipientUserId);
        Log.i(TAG, "inserted data" + message);
    }

    public void persistP2PSyncInfos(String p2pSyncJson) {
        List<P2PSyncInfo> infos = this.deSerializeP2PSyncInfoFromJson(p2pSyncJson);
        db.beginTransaction();
        try {
            for (P2PSyncInfo info : infos) {
                this.persistP2PSyncMessage(info);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }


    public boolean persistProfileMessage(String photoJson) {
        Log.i(TAG, "persistProfileMessage: " + photoJson);
        ProfileMessage message = this.deSerializeProfileMessageFromJson(photoJson);
        if (message != null) {
            try {
                db.beginTransaction();
                String imageString = new String(message.getData());
                Log.i(TAG, "imageString:" + imageString);
                byte[] data = Base64.decode(imageString, Base64.DEFAULT);
//                final BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inJustDecodeBounds = true;
                Bitmap decodedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                decodedImage.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();
                String fileName = P2PSyncManager.createProfilePhoto(message.getUserId(), bitmapdata, this.context);
                this.upsertProfileForUserIdAndDevice(message.getUserId(), message.getDeviceId(), fileName);
                P2PSyncManager.getInstance(context).updateInSharedPreference(P2PSyncManager.connectedDevice, message.getDeviceId());
                db.setTransactionSuccessful();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return false;
            } finally {
                db.endTransaction();
            }
        } else {
            return true;
        }

    }

    @Override
    public void addDeviceToSync(String deviceId, boolean syncImmediately) {
        P2PSyncDeviceStatus currentStatus = db.p2pSyncDeviceStatusDao().getDeviceInfo(deviceId);
        P2PSyncDeviceStatus status = currentStatus;
        if (currentStatus == null) {
            // treat as new request
            status = new P2PSyncDeviceStatus(deviceId, syncImmediately);
        } else {
            if (currentStatus.syncTime == null) {
                // not yet sync
                if (currentStatus.syncImmediately == false) {
                    if (syncImmediately == true) {
                        status = new P2PSyncDeviceStatus(deviceId, true);
                    } else {
                        status.syncTime = null;
                    }
                } else if (currentStatus.syncImmediately == true) {
                    status.syncTime = null;
                }
            } else {
                // treat as new request
                status = new P2PSyncDeviceStatus(deviceId, syncImmediately);
            }
        }

        db.p2pSyncDeviceStatusDao().insertP2PSyncDeviceStatus(status);
    }

    @Override
    public void syncCompleted(String deviceId) {
        P2PSyncDeviceStatus status = new P2PSyncDeviceStatus(deviceId, false);
        status.setSyncTime(new Date());
        db.p2pSyncDeviceStatusDao().insertP2PSyncDeviceStatus(status);
        Log.i(TAG, "sync completed with deviceId:" + deviceId);

    }

    @Override
    public List<P2PSyncDeviceStatus> getAllSyncDevices() {
        return Arrays.asList(db.p2pSyncDeviceStatusDao().getAllSyncDevices());
    }

    @Override
    public List<P2PSyncDeviceStatus> getAllNonSyncDevices() {
        return Arrays.asList(db.p2pSyncDeviceStatusDao().getAllNotSyncDevices());
    }

    @Override
    public P2PSyncDeviceStatus getLatestDeviceToSync() {
        P2PSyncDeviceStatus syncImmediatelyRequest = db.p2pSyncDeviceStatusDao().getTopDeviceToSyncImmediately();
        if (syncImmediatelyRequest == null) {
            syncImmediatelyRequest = db.p2pSyncDeviceStatusDao().getTopDeviceToNotSyncImmediately();
        }

        return syncImmediatelyRequest;
    }

    @Override
    public P2PSyncDeviceStatus getLatestDeviceToSyncFromDevices(List<String> items) {
        String deviceIds = StringUtils.join(items, ',');

        P2PSyncDeviceStatus syncImmediatelyRequest = db.p2pSyncDeviceStatusDao().getTopDeviceToSyncImmediately(deviceIds);
        if (syncImmediatelyRequest == null) {
            syncImmediatelyRequest = db.p2pSyncDeviceStatusDao().getTopDeviceToNotSyncImmediately(deviceIds);
        }

        return syncImmediatelyRequest;
    }


    public String serializeHandShakingMessage() {
        try {
            List<HandShakingInfo> handShakingInfos = new ArrayList<HandShakingInfo>();
            P2PLatestInfoByUserAndDevice[] infos = db.p2pSyncDao().getLatestInfoAvailableByUserIdAndDeviceId();
            for (P2PLatestInfoByUserAndDevice info : infos) {
                handShakingInfos.add(new HandShakingInfo(info.userId, info.deviceId, info.sequence));
            }

            Gson gson = this.registerHandShakingMessageBuilder();

            HandShakingMessage message = new HandShakingMessage("handshaking", handShakingInfos);
            Type handShakingType = new TypeToken<HandShakingMessage>() {
            }.getType();
            String json = gson.toJson(message, handShakingType);
            return json;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public String serializeProfileMessage(String userId, String deviceId, String contents) {
        try {
            String photoContents = contents;
            Gson gson = this.registerProfileMessageBuilder();
            ProfileMessage message = new ProfileMessage(userId, deviceId, "profileMessage", photoContents);
            Type ProfileMessageType = new TypeToken<ProfileMessage>() {
            }.getType();
            String json = gson.toJson(message, ProfileMessageType);
            return json;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public String buildAllSyncMessages(String handShakeJson) {
        List<HandShakingInfo> infos = deSerializeHandShakingInformationFromJson(handShakeJson);
        List<P2PSyncInfo> output = this.buildSyncInformation(infos);

        Iterator outIT = output.iterator();
        while(outIT.hasNext()) {
            P2PSyncInfo info = (P2PSyncInfo) outIT.next();
            Log.i(TAG, "got pSync Info of message" + info.getMessage());
            Log.i(TAG, "got pSync Info of message type" + info.getMessageType());
            if(info != null && info.messageType.equals("Photo")) {
                String message = info.getMessage();
                Log.i(TAG, "got pSync message original" + info.getMessage());
                if(message != null) {
                    File file = new File(context.getApplicationContext().getExternalFilesDir(null) + "/Cache", "DefaultImage.jpg");
                    String encodedMesssage =  P2PSyncManager.encodeFileToBase64Binary(file.getAbsolutePath());
                    Log.i(TAG, "got pSync message content" + encodedMesssage);
                    info.setMessage(encodedMesssage);
                }
            }
        }

        String json = this.convertP2PSyncInfoToJson(output);
        Log.i(TAG, "SYNC JSON:" + json);
        return json;
    }

    private List<HandShakingInfo> queryInitialHandShakingMessage() {
        List<HandShakingInfo> handShakingInfos = new ArrayList<HandShakingInfo>();
        P2PLatestInfoByUserAndDevice[] infos = db.p2pSyncDao().getLatestInfoAvailableByUserIdAndDeviceId();
        for (P2PLatestInfoByUserAndDevice info : infos) {
            handShakingInfos.add(new HandShakingInfo(info.userId, info.deviceId, info.sequence));
        }
        return handShakingInfos;
    }


    private Gson registerHandShakingMessageBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(HandShakingInfo.class, new HandShakingInfoDeserializer());
        gsonBuilder.registerTypeAdapter(HandShakingMessage.class, new HandShakingMessageDeserializer());
        Gson gson = gsonBuilder.create();
        return gson;
    }

    private Gson registerProfileMessageBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ProfileMessage.class, new ProfileMessageDeserializer());
        Gson gson = gsonBuilder.create();
        return gson;
    }

    private Gson registerP2PSyncInfoBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(P2PSyncInfo.class, new P2PSyncInfoDeserializer());
        Gson gson = gsonBuilder.create();

        return gson;
    }

    public String convertP2PSyncInfoToJson(List<P2PSyncInfo> infos) {
        Type collectionType = new TypeToken<List<P2PSyncInfo>>() {
        }.getType();
        Gson gson = this.registerP2PSyncInfoBuilder();
        String json = gson.toJson(infos, collectionType);
        return json;
    }

    private List<P2PSyncInfo> deSerializeP2PSyncInfoFromJson(String p2pSyncJson) {
        Log.i(TAG, "P2P Sync Info received" + p2pSyncJson);
        Gson gson = this.registerP2PSyncInfoBuilder();
        Type collectionType = new TypeToken<List<P2PSyncInfo>>() {
        }.getType();
        List<P2PSyncInfo> infos = gson.fromJson(p2pSyncJson, collectionType);
        return infos;
    }

    private ProfileMessage deSerializeProfileMessageFromJson(String photoJson) {
        Log.i(TAG, "P2P Photo Message received" + photoJson);
        Gson gson = this.registerProfileMessageBuilder();
        Type ProfileMessageType = new TypeToken<ProfileMessage>() {
        }.getType();
        ProfileMessage message = gson.fromJson(photoJson, ProfileMessageType);
        Log.i(TAG, "got deviceId " + message.getDeviceId());
        Log.i(TAG, "got getMessageType " + message.getMessageType());
        Log.i(TAG, "got getUserId " + message.getUserId());
        Log.i(TAG, "got getData " + message.getData());
        return message;
    }


    public List<HandShakingInfo> deSerializeHandShakingInformationFromJson(String handShakingJson) {
        List result = new ArrayList();
        try {
            Gson gson = this.registerHandShakingMessageBuilder();
            Type handShakingMessageType = new TypeToken<HandShakingMessage>() {
            }.getType();
            HandShakingMessage message = gson.fromJson(handShakingJson, handShakingMessageType);
            if (message != null) {
                result = message.getInfos();
            }

        } catch (Exception e) {
            Log.i(TAG, "deSerializeHandShakingInformationFromJson exception" + e.getMessage());
        }
        return result;
    }


    private List<P2PSyncInfo> buildSyncInformation(final List<HandShakingInfo> otherHandShakeInfos) {
        final List<HandShakingInfo> latestInfoFromCurrentDevice = this.queryInitialHandShakingMessage();

        Collections.sort(latestInfoFromCurrentDevice, new Comparator<HandShakingInfo>() {
            @Override
            public int compare(HandShakingInfo o1, HandShakingInfo o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });


        Collections.sort(otherHandShakeInfos, new Comparator<HandShakingInfo>() {
            @Override
            public int compare(HandShakingInfo o1, HandShakingInfo o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });

        final List<HandShakingInfo> validElementsFromOther = new ArrayList<HandShakingInfo>();
        final List<HandShakingInfo> removeElementsFromInput = new ArrayList<HandShakingInfo>();

        CollectionUtils.forAllDo(latestInfoFromCurrentDevice, new Closure<HandShakingInfo>() {
            @Override
            public void execute(final HandShakingInfo input) {
                Log.i(TAG, "processing element" + input);
                CollectionUtils.find(otherHandShakeInfos, new Predicate<HandShakingInfo>() {
                    @Override
                    public boolean evaluate(HandShakingInfo other) {
                        // if element exists in both list for same device
                        if (input.getDeviceId().equals(other.getDeviceId())) {
                            if (input.getUserId().equals(other.getUserId())) {
                                if (input.getSequence() > other.getSequence()) {
                                    validElementsFromOther.add(other);
                                } else {
                                    removeElementsFromInput.add(input);
                                }
                            }
                        } else {
                            validElementsFromOther.add(other);
                        }
//                        if (input.getUserId().equals(other.getUserId())) {
//                            if (input.getSequence() > other.getSequence()) {
//                                validElementsFromOther.add(other);
//                            } else {
//                                removeElementsFromInput.add(input);
//                            }
//                        } else {
//
//                        }
                        return false;
                    }
                });
            }
        });

        latestInfoFromCurrentDevice.addAll(validElementsFromOther);
        latestInfoFromCurrentDevice.removeAll(removeElementsFromInput);

        Collections.sort(latestInfoFromCurrentDevice, new Comparator<HandShakingInfo>() {
            @Override
            public int compare(HandShakingInfo o1, HandShakingInfo o2) {
                return (o1.getUserId().compareTo(o2.getUserId()));
            }
        });


        @SuppressWarnings("unchecked")
        Map<String, HandShakingInfo> map = new HashMap<String, HandShakingInfo>() {
            {
                IteratorUtils.forEach(latestInfoFromCurrentDevice.iterator(), new Closure() {
                    @Override
                    public void execute(Object input) {
                        HandShakingInfo item = (HandShakingInfo) input;
                        String key = item.getUserId() + "_" + item.getDeviceId();
                        if (containsKey(key)) {
                            HandShakingInfo storedItem = get(key);
                            if (storedItem.getSequence() > item.getSequence()) {
                                storedItem.setStartingSequence(item.getSequence());
                            } else {
                                storedItem.setStartingSequence(storedItem.getSequence());
                                storedItem.setSequence(item.getSequence());
                            }
                        } else {
                            put(key, item);
                        }
                    }
                });
            }
        };


        // process Map (execute queries and get result)

        Collection<HandShakingInfo> collectionValues = map.values();
        List<P2PSyncInfo> results = new ArrayList<P2PSyncInfo>();
        for (HandShakingInfo i : collectionValues) {
            P2PSyncInfo[] res = null;
            if (i.getStartingSequence() != null && i.getSequence() != null) {
                res = db.p2pSyncDao().fetchByUserAndDeviceBetweenSequences(i.getUserId(), i.getDeviceId(), i.getStartingSequence(), i.getSequence());
            } else if (i.getStartingSequence() == null && i.getSequence() != null) {
                res = db.p2pSyncDao().fetchByUserAndDeviceUpToSequence(i.getUserId(), i.getDeviceId(), i.getSequence());
            }
            if (res != null) {
                results.addAll(Arrays.asList(res));
            }
        }

        return results;
    }

    public List<P2PUserIdDeviceIdAndMessage> getUsers() {
        return Arrays.asList(db.p2pSyncDao().fetchAllUsers());
    }

    public List<P2PSyncInfo> getInfoByUserId(String userid){
        return Arrays.asList(db.p2pSyncDao().getSyncInformationByUserId(userid));
    }

    public List<P2PUserIdMessage> fetchLatestMessagesByMessageType(String messageType, List<String> userIds) {
        if (userIds != null && userIds.size() > 0) {
            return db.p2pSyncDao().fetchLatestMessagesByMessageType(messageType, userIds);
        } else {
            return db.p2pSyncDao().fetchLatestMessagesByMessageType(messageType);
        }
    }

    public boolean addMessage(String userId, String recipientId, String messageType, String message) {
        try {
            SharedPreferences pref = this.context.getSharedPreferences(P2P_SHARED_PREF, 0);
            String deviceId = pref.getString("DEVICE_ID", null); // getting String

            Long maxSequence = db.p2pSyncDao().getLatestSequenceAvailableByUserIdAndDeviceId(userId, deviceId);
            if (maxSequence == null) {
                maxSequence = 0L;
            }

            maxSequence++;
            P2PSyncInfo info = new P2PSyncInfo(userId, deviceId, maxSequence, recipientId, message, messageType);
            db.p2pSyncDao().insertP2PSyncInfo(info);
            Log.i(TAG, "inserted data" + info);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public boolean addMessage(String userId, String recipientId, String messageType, String message, Boolean status, String sessionId) {
        try {
            SharedPreferences pref = this.context.getSharedPreferences(P2P_SHARED_PREF, 0);
            String deviceId = pref.getString("DEVICE_ID", null); // getting String

            Long maxSequence = db.p2pSyncDao().getLatestSequenceAvailableByUserIdAndDeviceId(userId, deviceId);
            if (maxSequence == null) {
                maxSequence = 0L;
            }
            maxSequence++;

            Long step = db.p2pSyncDao().getLatestStepForUserIdAndSessionId(userId, sessionId);
            if (step == null) {
                step = 0L;
            }

            step++;

            P2PSyncInfo info = new P2PSyncInfo(userId, deviceId, maxSequence, recipientId, message, messageType);
            info.setSessionId(sessionId);
            info.setStatus(status);
            info.setStep(step);
            db.p2pSyncDao().insertP2PSyncInfo(info);
            Log.i(TAG, "inserted data" + info);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public List<P2PSyncInfo> getConversations(String firstUserId, String secondUserId, String messageType) {
        return db.p2pSyncDao().fetchConversations(firstUserId, secondUserId, messageType);
    }

    public List<P2PSyncInfo> getLatestConversations(String firstUserId, String secondUserId, String messageType) {
        return db.p2pSyncDao().fetchLatestConversations(firstUserId, secondUserId, messageType);
    }

    public String readProfilePhoto() {
        SharedPreferences pref = this.context.getSharedPreferences(P2P_SHARED_PREF, 0);
        String userId = pref.getString("USER_ID", null); // getting String
        return P2PSyncManager.generateUserPhotoFileName(userId);
    }

    public boolean upsertProfile() {
        try {
            SharedPreferences pref = this.context.getSharedPreferences(P2P_SHARED_PREF, 0);
            String fileName = pref.getString("PROFILE_PHOTO", null); // getting String
            String userId = pref.getString("USER_ID", null); // getting String
            String deviceId = pref.getString("DEVICE_ID", null); // getting String

            return this.upsertProfileForUserIdAndDevice(userId, deviceId, fileName);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public boolean upsertProfileForUserIdAndDevice(String userId, String deviceId, String message) {
        try {
            P2PSyncInfo userInfo = db.p2pSyncDao().getProfileByUserId(userId, P2PSyncManager.MessageTypes.PHOTO.type());
            if (userInfo != null) {
                userInfo.setUserId(userId);
                userInfo.setDeviceId(deviceId);
                userInfo.setMessage(message);
                userInfo.setMessageType(P2PSyncManager.MessageTypes.PHOTO.type());
            } else {
                userInfo = new P2PSyncInfo();
                userInfo.setUserId(userId);
                userInfo.setDeviceId(deviceId);

                Long maxSequence = db.p2pSyncDao().getLatestSequenceAvailableByUserIdAndDeviceId(userId, deviceId);
                if (maxSequence == null) {
                    maxSequence = 0L;
                }

                maxSequence++;
                userInfo.setSequence(maxSequence);
                userInfo.setMessage(message);
                userInfo.setMessageType(P2PSyncManager.MessageTypes.PHOTO.type());
            }
            db.p2pSyncDao().insertP2PSyncInfo(userInfo);
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    @Override
    public List<P2PSyncInfo> getLatestConversationsByUser(String firstUserId) {
        return db.p2pSyncDao().fetchLatestConversationsByUser(firstUserId);
    }
}


class P2PSyncInfoDeserializer implements JsonDeserializer<P2PSyncInfo> {
    @Override
    public P2PSyncInfo deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();

        final JsonElement jsonUserId = jsonObject.get("userId");
        final String userId = jsonUserId.getAsString();

        final JsonElement jsonDeviceId = jsonObject.get("deviceId");
        final String deviceId = jsonDeviceId.getAsString();

        final JsonElement jsonSequence = jsonObject.get("sequence");
        final Long sequence = jsonSequence.getAsLong();

        final JsonElement jsonMessageType = jsonObject.get("messageType");
        final String messageType = jsonMessageType.getAsString();

        String recipientUserId = null;
        final JsonElement jsonRecipientType = jsonObject.get("recipientUserId");
        if (jsonRecipientType != null) {
            recipientUserId = jsonRecipientType.getAsString();
        }

        String message = null;
        final JsonElement jsonMessage = jsonObject.get("message");
        if (jsonMessage != null) {
            message = jsonMessage.getAsString();
        }
        final String receivedMessage = message == null ? "" : message;

        final P2PSyncInfo p2PSyncInfo = new P2PSyncInfo(userId, deviceId, sequence, recipientUserId, receivedMessage, messageType);
        return p2PSyncInfo;
    }
}