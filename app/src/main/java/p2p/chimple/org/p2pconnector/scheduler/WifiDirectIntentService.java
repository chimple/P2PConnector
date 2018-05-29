package p2p.chimple.org.p2pconnector.scheduler;

import android.app.Service;
import android.app.job.JobParameters;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;


import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.JOB_PARAMS;

public class WifiDirectIntentService extends JobIntentService {
    public static final int JOB_ID = 999;
    private static final String TAG = WifiDirectIntentService.class.getSimpleName();
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private P2PSyncManager p2pSyncManager;
    private JobParameters currentJobParams;
    ;
    private WifiDirectIntentService that = this;

    public WifiDirectIntentService() {
        super();
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleWork((Intent) msg.obj);
        }
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WifiDirectIntentService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        this.p2pSyncManager = P2PSyncManager.getInstance(this.getApplicationContext());
    }

    @Override
    public void onStart(Intent intent, int startId) {
        currentJobParams = intent.getExtras().getParcelable(JOB_PARAMS);
        Log.i(TAG, currentJobParams.toString());
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying P2PSync Manager");
        this.p2pSyncManager.onDestroy();
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Auto-generated method stub
        return null;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "do actual work");
        //broadcast result once done
        this.p2pSyncManager.execute(this.currentJobParams);
    }
}
