package p2p.chimple.org.p2pconnector.db;

import android.content.Context;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.VisibleForTesting;


import p2p.chimple.org.p2pconnector.db.converter.DateConverter;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncDeviceStatusDao;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncDeviceStatus;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

@Database(entities = {P2PSyncInfo.class, P2PSyncDeviceStatus.class},
        version = 1
)
@TypeConverters(
        DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "p2p_db";

    /**
     * The only instance
     */
    private static AppDatabase sInstance;

    private Context context;

    public abstract P2PSyncInfoDao p2pSyncDao();

    public abstract P2PSyncDeviceStatusDao p2pSyncDeviceStatusDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room
                    .databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .build();
            DatabaseInitializer.populateAsync(sInstance, context, P2PDBApiImpl.getInstance(context));
            sInstance.context = context;
        }
        return sInstance;
    }

    public Context getContext() {
        return context;
    }

    public static AppDatabase getInitializedInstance() {
        return sInstance;
    }

    /**
     * Switches the internal implementation with an empty in-memory database.
     *
     * @param context The context.
     */
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        sInstance = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                AppDatabase.class).build();
    }


    public static void destroyInstance() {
        sInstance = null;
    }
}
