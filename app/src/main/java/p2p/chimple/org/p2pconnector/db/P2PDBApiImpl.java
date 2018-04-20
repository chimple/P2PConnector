package p2p.chimple.org.p2pconnector.db;

import android.content.Context;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfo;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfoDeserializer;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingMessage;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingMessageDeserializer;
import p2p.chimple.org.p2pconnector.db.entity.P2PLatestInfoByUserAndDevice;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

public class P2PDBApiImpl implements P2PDBApi {
    private static final String TAG = P2PDBApiImpl.class.getName();
    private AppDatabase db;
    private Context context;

    public P2PDBApiImpl(AppDatabase db, Context context) {
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

    public String serializeHandShakingMessage() {
        List<HandShakingInfo> handShakingInfos = new ArrayList<HandShakingInfo>();
        P2PLatestInfoByUserAndDevice[] infos = db.p2pSyncDao().getLatestInfoAvailableByUserIdAndDeviceId();
        for (P2PLatestInfoByUserAndDevice info : infos) {
            handShakingInfos.add(new HandShakingInfo(info.userId, info.deviceId, info.sequence));
        }

        Gson gson = this.registerHandShakingMessageBuilder();

        HandShakingMessage message = new HandShakingMessage("handshaking", handShakingInfos);
        Type handShakingType = new TypeToken<HandShakingMessage>() {}.getType();
        String json = gson.toJson(message, handShakingType);
        return json;
    }

    public String buildAllSyncMessages(List<HandShakingInfo> infos) {
        List<P2PSyncInfo> output = this.buildSyncInformation(infos);
        return this.convertP2PSyncInfoToJson(output);
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

    public String convertP2PSyncInfoToJson(List<P2PSyncInfo> infos) {
        Type collectionType = new TypeToken<List<P2PSyncInfo>>() {
        }.getType();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(P2PSyncInfo.class, new P2PSyncInfoDeserializer());
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(infos, collectionType);
        return json;
    }

    public List<HandShakingInfo> deSerializeHandShakingInformationFromJson(String handShakingJson) {
        Gson gson = this.registerHandShakingMessageBuilder();
        Type handShakingMessageType = new TypeToken<HandShakingMessage>() {
        }.getType();
        HandShakingMessage message = gson.fromJson(handShakingJson, handShakingMessageType);
        if (message != null) {
            return message.getInfos();
        }
        return null;
    }


    public List<P2PSyncInfo> buildSyncInformation(final List<HandShakingInfo> otherHandShakeInfos) {
        final List<HandShakingInfo> latestInfoFromCurrentDevice = this.queryInitialHandShakingMessage();

        Collections.sort(latestInfoFromCurrentDevice, new Comparator<HandShakingInfo>() {
            @Override
            public int compare(HandShakingInfo o1, HandShakingInfo o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });


//        final List<HandShakingInfo> otherHandShakeInfos = new ArrayList<HandShakingInfo>();
//        otherHandShakeInfos.add(new HandShakingInfo("1", "D", 2L));
//        otherHandShakeInfos.add(new HandShakingInfo("3", "A", 5L));
//        otherHandShakeInfos.add(new HandShakingInfo("4", "B", 2L));
//        otherHandShakeInfos.add(new HandShakingInfo("5", "R", 2L));

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
                        // if element exists in both list
                        if (input.getUserId().equals(other.getUserId())) {
                            if (input.getSequence() > other.getSequence()) {
                                validElementsFromOther.add(other);
                            } else {
                                removeElementsFromInput.add(input);
                            }
                        } else {

                        }
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
                        if (containsKey(item.getUserId())) {
                            HandShakingInfo storedItem = get(item.getUserId());
                            if (storedItem.getSequence() > item.getSequence()) {
                                storedItem.setStartingSequence(item.getSequence());
                            } else {
                                storedItem.setStartingSequence(storedItem.getSequence());
                                storedItem.setSequence(item.getSequence());
                            }
                        } else {
                            put(item.getUserId(), item);
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
}


class P2PSyncInfoDeserializer implements JsonDeserializer<P2PSyncInfo> {
    @Override
    public P2PSyncInfo deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();

        final JsonElement jsonUserId = jsonObject.get("user_id");
        final String userId = jsonUserId.getAsString();

        final JsonElement jsonDeviceId = jsonObject.get("device_id");
        final String deviceId = jsonDeviceId.getAsString();

        final JsonElement jsonSequence = jsonObject.get("sequence");
        final Long sequence = jsonSequence.getAsLong();

        final JsonElement jsonMessageType = jsonObject.get("message_type");
        final String messageType = jsonMessageType.getAsString();

        final JsonElement jsonReceipientType = jsonObject.get("recipient_user_id");
        final String receipientUserId = jsonReceipientType.getAsString();


        final JsonElement jsonMessage = jsonObject.get("message");
        final String message = jsonMessage.getAsString();


        final P2PSyncInfo p2PSyncInfo = new P2PSyncInfo(userId, deviceId, sequence, receipientUserId, message, messageType);
        return p2PSyncInfo;
    }
}