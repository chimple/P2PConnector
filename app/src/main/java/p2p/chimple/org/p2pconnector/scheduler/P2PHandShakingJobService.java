package p2p.chimple.org.p2pconnector.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)

public class P2PHandShakingJobService extends JobService {
    private static final String TAG = P2PHandShakingJobService.class.getSimpleName();
    private WifiDirectIntentBroadcastReceiver receiver;

    public static final String P2P_SYNC_RESULT_RECEIVED = "P2P_SYNC_RESULT_RECEIVED";
    public static final String JOB_PARAMS = "JOB_PARAMS";

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerWifiDirectIntentBroadcastReceiver();
        Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        this.unregisterWifiDirectIntentBroadcastReceiver();
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    /**
     * When the app's MainActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        // The work that this service "does" is simply wait for a certain duration and finish
        // the job (on another thread).
        Intent wifiDirectServiceIntent = new Intent(getApplicationContext(), WifiDirectIntentService.class);
        wifiDirectServiceIntent.putExtra(JOB_PARAMS, params);
        getApplicationContext().startService(wifiDirectServiceIntent);
        Log.i(TAG, "on start job: " + params.getJobId());

        // Return true as there's more work to be done with this job.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Stop tracking these job parameters, as we've 'finished' executing.
        Log.i(TAG, "on stop job: " + params.getJobId());
        // Return false to drop the job.
        return false;
    }

    private List<JobInfo> getAllPendingJob() {
        JobScheduler jobScheduler = (JobScheduler)
                getSystemService(JOB_SCHEDULER_SERVICE);

        // Get the list of scheduled jobs
        List<JobInfo> jobList = jobScheduler.getAllPendingJobs();
        return jobList;
    }

    private void unregisterWifiDirectIntentBroadcastReceiver() {
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            receiver = null;
            Log.i(TAG, "WifiDirectIntentBroadcast Receiver unregistered");
        }
    }

    private void registerWifiDirectIntentBroadcastReceiver() {
        receiver = new WifiDirectIntentBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(P2P_SYNC_RESULT_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        Log.i(TAG, "WifiDirectIntentBroadcast Receiver registered");
    }


    /**
     * BroadcastReceiver used to receive Intents fired from the WifiDirectHandler when P2P events occur
     * Used to update the UI and receive communication messages
     */
    public class WifiDirectIntentBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            JobParameters params = intent.getExtras().getParcelable(JOB_PARAMS);
            Log.i(TAG, "on finisned job: " + params.getJobId());
            jobFinished(params, false);

        }
    }
}
