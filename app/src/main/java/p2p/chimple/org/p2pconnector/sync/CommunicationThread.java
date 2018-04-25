package p2p.chimple.org.p2pconnector.sync;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CommunicationThread extends Thread {
    private static final String TAG = CommunicationThread.class.getSimpleName();

    private CommunicationCallBack callBack;
    private final ServerSocket mSocket;
    boolean mStopped = false;

    public CommunicationThread(CommunicationCallBack callback, int port) {
        this.callBack = callback;
        ServerSocket tmp = null;

        try {
            tmp = new ServerSocket(port);
            Log.i(TAG, "CommunicationThread ServerSocket created....");
        } catch (IOException e) {
            Log.i(TAG, "new ServerSocket failed: " + e.toString());
        }
        mSocket = tmp;
    }

    public void run() {

        if (this.callBack != null) {
            Log.i(TAG, "starting to listen");
            Socket socket = null;
            try {
                if (mSocket != null) {
                    socket = mSocket.accept();
                }
                if (socket != null) {
                    Log.i(TAG, "Incoming test-connection");
                    this.callBack.GotConnection(socket);
                } else if (!mStopped) {
                    this.callBack.ListeningFailed("Socket is null");
                }

            } catch (Exception e) {
                if (!mStopped) {
                    //return failure
                    Log.i(TAG, "accept socket failed: " + e.toString());
                    this.callBack.ListeningFailed(e.toString());
                }
            }
        }
    }

    public void Stop() {
        Log.i(TAG, "communication cancelled");
        mStopped = true;
        try {
            if (mSocket != null) {
                mSocket.close();
                Log.i(TAG, "communication socket closed");
            }
        } catch (IOException e) {
            Log.i(TAG, "closing socket failed: " + e.toString());
        }
    }
}
