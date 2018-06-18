package p2p.chimple.org.p2pconnector.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {

    public static boolean getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiCheck = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifiCheck != null) {
            if (wifiCheck.isConnected()) {
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }
    }
}