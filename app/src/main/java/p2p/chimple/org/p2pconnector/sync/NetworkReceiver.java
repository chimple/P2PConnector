package p2p.chimple.org.p2pconnector.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static p2p.chimple.org.p2pconnector.sync.Direct.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.sync.Direct.P2PSyncManager.p2pConnectionChangedEvent;
import static p2p.chimple.org.p2pconnector.sync.NSD.NSDSyncManager.nsdConnectionChangedEvent;

public class NetworkReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        boolean IsConnected = NetworkUtil.getConnectivityStatusString(context);
        Log.i(TAG, "Is Connected:" + IsConnected);

        SharedPreferences pref = context.getSharedPreferences(P2P_SHARED_PREF, 0);
        boolean isP2P = pref.getBoolean("IS_P2P", false); // getting String
        if(isP2P) {
            Intent networkChanged = new Intent(p2pConnectionChangedEvent);
            networkChanged.putExtra("isConnected", IsConnected);
            LocalBroadcastManager.getInstance(context).sendBroadcast(networkChanged);

        } else {
            Intent networkChanged = new Intent(nsdConnectionChangedEvent);
            networkChanged.putExtra("isConnected", IsConnected);
            LocalBroadcastManager.getInstance(context).sendBroadcast(networkChanged);
        }
    }
}
