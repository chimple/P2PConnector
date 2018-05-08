package p2p.chimple.org.p2pconnector.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import static android.content.Context.JOB_SCHEDULER_SERVICE;


public class JobUtils {
    private static final String TAG = JobUtils.class.getName();

    private static boolean isJobRunning = false;

    public synchronized static void scheduledJob(Context context, int period) {
        if(!isJobRunning()) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            boolean isAnyPendingJob = isAnyJobScheduled(context);
            if (isAnyPendingJob) {
                Log.i(TAG, "Cancelling all pending jobs");
                jobScheduler.cancelAll();
            }
            JobInfo.Builder builder = buildJob(context);
            builder.setMinimumLatency(period);
            jobScheduler.schedule(builder.build());
            Log.i(TAG, "Scheduling immediate job");
        } else {
            Log.i(TAG, "Job is already running");
        }
    }

    private synchronized static JobInfo.Builder buildJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, P2PHandShakingJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setPersisted(true);
//        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        return builder;
    }

    public synchronized static boolean isAnyJobScheduled(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 0) {
                hasBeenScheduled = true;
                Log.i(TAG, "found scheduled job:" + hasBeenScheduled);
                Log.i(TAG, "found scheduled job of type periodic:" + jobInfo.isPeriodic());
                break;
            }
        }

        return hasBeenScheduled;
    }

    public synchronized static void cancelAllJobs(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(0);
        scheduler.cancelAll();
    }

    public synchronized static boolean isJobRunning() {
        return isJobRunning;
    }

    public synchronized static void setJobRunning(boolean jobRunning) {
        isJobRunning = jobRunning;
    }
}
