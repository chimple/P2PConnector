package p2p.chimple.org.p2pconnector.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.scheduler.JobUtils;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

public class P2PApplication extends Application {
    private static final String TAG = P2PApplication.class.getName();
    private static Context context;
    private P2PApplication that;
    public static AppDatabase db;
    String USERID_UUID;

    public static int REGULAR_JOB_TIMINGS = 5 * 60 * 1000; // every 7 mins
    public static int IMMEDIATE_JOB_TIMINGS = 5 * 1000; // in next 5 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
        context = this;
        that = this;
    }

    private void initialize() {
        Log.d(TAG, "Initializing...");

        Thread initializationThread = new Thread() {
            @Override
            public void run() {
                // Initialize all of the important frameworks and objects
                that.createShardProfilePreferences();
                P2PContext.getInstance().initialize(P2PApplication.this);
                //TODO: for now force the creation here
                db = AppDatabase.getInstance(P2PApplication.this);

                Log.i(TAG, "app database instance" + String.valueOf(db));

                initializationComplete();
            }
        };

        initializationThread.start();
    }

    private void createShardProfilePreferences() {
        SharedPreferences pref = this.getContext().getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        USERID_UUID = UUID.randomUUID().toString();
        Log.i(TAG, "created UUID User:" + USERID_UUID);
        editor.putString("USER_ID", USERID_UUID);
        editor.putString("DEVICE_ID", UUID.randomUUID().toString());
        editor.commit(); // commit changes
    }


    private void initializationComplete() {
        Log.i(TAG, "Initialization complete...");
    }

    public static Context getContext() {
        return context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}