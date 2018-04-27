package p2p.chimple.org.p2pconnector.db.entity;

import android.arch.persistence.room.ColumnInfo;

public class P2PUserIdMessage {
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "message")
    public String message;
}
