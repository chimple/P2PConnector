package p2p.chimple.org.p2pconnector.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class P2PHandShakeJobStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        JobUtils.scheduleJob(context);
    }
}