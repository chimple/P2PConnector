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

import android.util.Log;

import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;


import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.JOB_PARAMS;

public class WifiDirectIntentService extends Service {
    private static final String TAG = WifiDirectIntentService.class.getSimpleName();
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private P2PSyncManager p2pSyncManager;
    private JobParameters currentJobParams;
    ;
    private WifiDirectIntentService that = this;

    public WifiDirectIntentService() {
        super();
        Log.i(TAG, "WifiDirectIntentService Contructor");
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        this.p2pSyncManager = new P2PSyncManager(this.getApplicationContext());
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
        this.p2pSyncManager.onDestroy();
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Auto-generated method stub
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     *
     * @param intent The value passed to {@link
     *               Context#startService(Intent)}.
     */
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "do actual work");
        //broadcast result once done
        this.p2pSyncManager.execute(this.currentJobParams);
    }
}
