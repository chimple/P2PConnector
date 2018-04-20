package p2p.chimple.org.p2pconnector.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.Date;

@Entity(indices = {
        @Index("user_id"),
        @Index("device_id"),
        @Index("sequence")
}
)
public class P2PSyncInfo {

    public P2PSyncInfo() {

    }

    @PrimaryKey(autoGenerate = true)
    public Long id; // auto generated primary key

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="user_id")
    public String userId; //current logged-in user

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="device_id")
    public String deviceId;

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="sequence")
    public Long sequence;

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="message_type")
    public String messageType;

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="recipient_user_id")
    public String recipientUserId;

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="message")
    public String message;

    @Expose(serialize = true, deserialize = true)
    @ColumnInfo(name="file_name")
    public String fileName;

    @ColumnInfo(name="logged_at")
    public Date loggedAt;

    @Ignore
    public P2PSyncInfo(String userId, String deviceId, Long sequence, String receipientUserId, String message, String messageType) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.sequence = sequence;
        this.recipientUserId = receipientUserId;
        this.message = message;
        this.messageType = messageType;
        this.loggedAt = new Date();
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}


