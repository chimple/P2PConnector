package p2p.chimple.org.p2pconnector.db.entity;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class HandShakingInfoDeserializer implements JsonDeserializer<HandShakingInfo> {
    @Override
    public HandShakingInfo deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();
        String userId = "unknown";
        final JsonElement jsonUserId = jsonObject.get("user_id");
        if (jsonUserId != null) {
            userId = jsonUserId.getAsString();
        }

        final JsonElement jsonDeviceId = jsonObject.get("device_id");
        String deviceId = "unknown";
        if (jsonDeviceId != null) {
            deviceId = jsonDeviceId.getAsString();
        }

        Long sequence = 0L;
        final JsonElement jsonSequence = jsonObject.get("sequence");
        if (jsonSequence != null) {
            sequence = jsonSequence.getAsLong();
        }


        final HandShakingInfo handShakingInfo = new HandShakingInfo(userId, deviceId, sequence);
        return handShakingInfo;
    }
}