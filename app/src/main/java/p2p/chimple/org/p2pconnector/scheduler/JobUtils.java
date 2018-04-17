package p2p.chimple.org.p2pconnector.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import p2p.chimple.org.p2pconnector.BuildConfig;

public class JobUtils {
    // schedule the start of the service every 10 - 30 seconds
    public static void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, P2PHandShakingJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setPeriodic(60 * 10 * 1000); // maximum delay
        builder.setPersisted(true);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }
}
