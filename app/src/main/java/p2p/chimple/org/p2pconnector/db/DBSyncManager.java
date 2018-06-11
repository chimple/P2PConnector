package p2p.chimple.org.p2pconnector.db;

import android.content.Context;

public class DBSyncManager {

    private static final String TAG = DBSyncManager.class.getSimpleName();
    private Context context;
    private static DBSyncManager instance;

    public enum MessageTypes {
        PHOTO("Photo"),
        CHAT("Chat"),
        GAME("Game");

        private String type;

        MessageTypes(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }


    public static DBSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DBSyncManager.class) {
                instance = new DBSyncManager(context);
            }
        }

        return instance;
    }

    private DBSyncManager(Context context) {
        this.context = context;
    }


    public Context getContext() {
        return this.context;
    }
}
