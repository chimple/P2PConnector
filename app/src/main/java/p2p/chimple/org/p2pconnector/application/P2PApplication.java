package p2p.chimple.org.p2pconnector.application;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import p2p.chimple.org.p2pconnector.db.AppDatabase;

public class P2PApplication extends Application {
    private static final String TAG = P2PApplication.class.getName();
    private static Context context;
    private P2PApplication that;
    public static AppDatabase db;

    public static int REGULAR_JOB_TIMINGS_FOR_MIN_LATENCY = 30 * 1000; // 30 seconds
    public static int REGULAR_JOB_TIMINGS_FOR_PERIOD = 30 * 1000;
    public static int IMMEDIATE_JOB_TIMINGS = 30 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        initialize();
        context = this;
        that = this;
    }

    private void initialize() {
        Log.d(TAG, "Initializing...");
        Thread initializationThread = new Thread() {
            @Override
            public void run() {
                P2PContext.getInstance().initialize(P2PApplication.this);
                //TODO: for now force the creation here
                db = AppDatabase.getInstance(P2PApplication.this);

                Log.i(TAG, "app database instance" + String.valueOf(db));

                initializationComplete();
            }
        };

        initializationThread.start();
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