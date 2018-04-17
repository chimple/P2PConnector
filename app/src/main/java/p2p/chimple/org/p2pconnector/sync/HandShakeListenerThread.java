package p2p.chimple.org.p2pconnector.sync;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class HandShakeListenerThread extends Thread {

    private static final String TAG = HandShakeListenerThread.class.getSimpleName();
    private HandShakeListenerCallBack callBack;
    private final ServerSocket mSocket;
    boolean mStopped = false;

    public HandShakeListenerThread(HandShakeListenerCallBack callBack, int port) {
        this.callBack = callBack;
        ServerSocket tmp = null;

        try {
            tmp = new ServerSocket(port);
        } catch (Exception e) {
            Log.i(TAG, "new ServerSocket failed: " + e.toString());
        }
        mSocket = tmp;
    }

    public void run() {

        if (callBack != null) {
            Log.i(TAG, "starting to listen");
            Socket socket = null;
            try {
                if (mSocket != null) {
                    socket = mSocket.accept();
                }
                if (socket != null) {
                    Log.i(TAG, "we got incoming connection");
                    callBack.GotConnection(socket.getInetAddress(), socket.getLocalAddress());
                    OutputStream stream = socket.getOutputStream();
                    String hello = "shakeback";
                    stream.write(hello.getBytes());
                    socket.close();
                } else if (!mStopped) {
                    callBack.ListeningFailed("Socket is null");
                }

            } catch (Exception e) {
                if (!mStopped) {
                    //return failure
                    Log.i(TAG, "accept socket failed: " + e.toString());
                    callBack.ListeningFailed(e.toString());
                }
            }
        }

    }

    public void cleanUp() {
        Log.i(TAG, "cancelled");
        mStopped = true;
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            Log.i(TAG, "closing socket failed: " + e.toString());
        }
    }
}