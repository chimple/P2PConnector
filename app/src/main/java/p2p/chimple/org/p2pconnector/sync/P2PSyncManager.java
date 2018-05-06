package p2p.chimple.org.p2pconnector.sync;

import android.app.job.JobParameters;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.JOB_PARAMS;
import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.P2P_SYNC_RESULT_RECEIVED;
import static p2p.chimple.org.p2pconnector.sync.P2POrchester.neighboursUpdateEvent;

public class P2PSyncManager implements P2POrchesterCallBack, CommunicationCallBack, Handler.Callback {
    private static final String TAG = P2PSyncManager.class.getSimpleName();
    private Context context;
    private P2PSyncManager that;
    private CountDownTimer disconnectGroupOwnerTimeOut;
    private List<String> clientIPAddressList = new ArrayList<String>();
    private P2POrchester mWDConnector = null;
    private CommunicationThread mTestListenerThread = null;
    private ConnectToThread mTestConnectToThread = null;
    private ConnectedThread mTestConnectedThread = null;
    final private int TestChatPortNumber = 8768;
    private Handler mHandler;
    private HandlerThread handlerThread;
    private P2PStateFlow p2PStateFlow;
    private boolean connectedInLastTwoMins = false;
    private boolean reStartedDueToNoActivity = false;
    private boolean isAllAvailableClientsConnectedAndSynced = false;
    //Status
    private int mInterval = 1000; // 1 second by default, can be changed later
    private int timeCounter = 0;
    Runnable mStatusChecker = null;

    private Map<String, WifiDirectService> neighbours = null;

    public static final String profileFileExtension = ".txt";
    public static final String customStatusUpdateEvent = "custom-status-update-event";
    public static final String customTimerStatusUpdateEvent = "custom-timer-status-update-event";
    public static final String P2P_SHARED_PREF = "p2pShardPref";

    public enum MessageTypes {
        PHOTO("Photo"),
        CHAT("Chat"),
        GAME("Game");

        private String type;

        MessageTypes(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }


    public P2PSyncManager(Context context) {
        this.that = this;
        this.context = context;
        this.handlerThread = new HandlerThread("P2PSyncManager");
        this.handlerThread.start();
        this.mHandler = new Handler(this.handlerThread.getLooper(), this);
        this.p2PStateFlow = P2PStateFlow.getInstanceUsingDoubleLocking(this);

        LocalBroadcastManager.getInstance(this.context).registerReceiver(mMessageReceiver, new IntentFilter(neighboursUpdateEvent));

        this.initStatusChecker();
    }

    private void initStatusChecker() {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                // call function to update timer
                timeCounter = timeCounter + 1;
                Log.i(TAG, "timeCounter" + timeCounter);
                if (timeCounter > 120 && !connectedInLastTwoMins && !reStartedDueToNoActivity) {
                    Log.i(TAG, "RESTART CONNECTOR TO RESET  ALL STATE");
                    that.reStartedDueToNoActivity = true;
                    that.reStartConnector(true, 10000);
                    //do something
                }
                that.broadcastCustomTimerStatusUpdateEvent();
                that.mHandler.postDelayed(mStatusChecker, mInterval);
            }

        };
    }

    private void broadcastCustomTimerStatusUpdateEvent() {
        Intent intent = new Intent(customTimerStatusUpdateEvent);
        // You can also include some extra data.
        intent.putExtra("timeCounter", this.timeCounter);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            synchronized (this) {
                neighbours = (HashMap<String, WifiDirectService>) intent.getSerializableExtra("neighbours");
                for (Map.Entry<String, WifiDirectService> entry : neighbours.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        Log.i(TAG, "got neighbour: " + entry.getKey() + " : " + ((WifiDirectService) entry.getValue()).print());
                    }
                }
            }
        }
    };


    public Map<String, WifiDirectService> getNeighbours() {
        return this.neighbours;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ConnectedThread.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                String writeMessage = new String(writeBuf);
                updateStatus(TAG, "Wrote: " + writeMessage);
                break;
            case ConnectedThread.MESSAGE_READ:
                synchronized (P2PSyncManager.class) {
                    byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i(TAG, "MESSAGE READ:" + readMessage);
                    this.p2PStateFlow.processMessages(readMessage);
                }
                break;
            case ConnectedThread.SOCKET_DISCONNEDTED: {
                updateStatus(TAG + "CHAT", "WE are Stopped now.");
                stopConnectedThread();
            }
            break;
        }
        return true;
    }

//    public void execute(final JobParameters currentJobParams) {
//        mStatusChecker.run();
//        //changing the time and its interval
//        disconnectGroupOwnerTimeOut = new CountDownTimer(20000, 2000) {
//            public void onTick(long millisUntilFinished) {
//                // not using
//            }
//
//            public void onFinish() {
//                Log.i(TAG, "execute on finished");
//                Intent result = new Intent(P2P_SYNC_RESULT_RECEIVED);
//                result.putExtra(JOB_PARAMS, currentJobParams);
//                LocalBroadcastManager.getInstance(that.context).sendBroadcast(result);
//
//            }
//        };
//        disconnectGroupOwnerTimeOut.start();
//    }

    public void execute(final JobParameters currentJobParams) {
        mStatusChecker.run();
        //changing the time and its interval
        disconnectGroupOwnerTimeOut = new CountDownTimer(20000, 2000) {
            public void onTick(long millisUntilFinished) {
                // not using
            }

            public void onFinish() {
                if (that.isAllAvailableClientsConnectedAndSynced) {
                    Intent result = new Intent(P2P_SYNC_RESULT_RECEIVED);
                    result.putExtra(JOB_PARAMS, currentJobParams);
                    LocalBroadcastManager.getInstance(that.context).sendBroadcast(result);
                }
            }
        };

        //Start Init

        WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        StartConnector();
    }

    private void reStartConnector(boolean shouldRestart, int delayMillis) {
        stopConnector();
        if (shouldRestart) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                //Lets give others chance on creating new group before we come back online
                public void run() {
                    StartConnector();
                }
            }, delayMillis);

        }
    }

    public void toggle() {
        if (mWDConnector != null) {
            stopConnector();
        } else {
            StartConnector();
            this.connectedInLastTwoMins = false;
            this.reStartedDueToNoActivity = false;
        }
    }

    public void StartConnector() {
        //lets be ready for incoming test communications
        updateStatus(TAG, "starting listener now, and connector");
        startListenerThread();
        mWDConnector = new P2POrchester(this.context, this, this.mHandler);
    }

    public void stopConnector() {
        stopConnectedThread();
        stopConnectToThread();
        stopListenerThread();

        if (mWDConnector != null) {
            mWDConnector.cleanUp();
            mWDConnector = null;
        }

        updateStatus(TAG, "Stopped");
    }

    public ConnectedThread getConnectedThread() {
        return mTestConnectedThread;
    }

    public Context getContext() {
        return this.context;
    }

    private void startListenerThread() {
        stopListenerThread();
        mTestListenerThread = new CommunicationThread(this, TestChatPortNumber);
        mTestListenerThread.start();
    }

    private void stopListenerThread() {
        if (mTestListenerThread != null) {
            mTestListenerThread.Stop();
            mTestListenerThread = null;
        }
    }

    private void stopConnectToThread() {
        if (mTestConnectToThread != null) {
            mTestConnectToThread.Stop();
            mTestConnectToThread = null;
        }
    }


    private void stopConnectedThread() {
        if (mTestConnectedThread != null) {
            mTestConnectedThread.Stop();
            mTestConnectedThread = null;
        }
    }

    private void goToClientWaiting(String address) {
        stopConnectedThread();
        stopConnectToThread();
        this.disconnectGroupOwnerTimeOut.cancel();
        updateStatus("Data state", "goToClientWaiting => Will connect to " + address);
        Log.i(TAG, "Data state" + "goToClientWaiting => Will connect to " + address);
        mTestConnectToThread = new ConnectToThread(this, address, TestChatPortNumber);
        mTestConnectToThread.start();

    }

    public void goToNextClientWaiting() {
        stopConnectedThread();
        stopConnectToThread();
        that.disconnectGroupOwnerTimeOut.cancel();
        that.connectedInLastTwoMins = false;
        that.reStartedDueToNoActivity = false;
        that.isAllAvailableClientsConnectedAndSynced = false;

        if (clientIPAddressList.size() > 0) {
            //With this test we'll just handle each client one-by-one in order they got connected
            String Address = clientIPAddressList.get(0);
            clientIPAddressList.remove(0);
            updateStatus("Data state", "Will connect to " + Address);
            Log.i(TAG, "Data state" + "Will connect to " + Address);
            mTestConnectToThread = new ConnectToThread(this, Address, TestChatPortNumber);
            mTestConnectToThread.start();
        } else {
            updateStatus("Data state", "All addresses connected, will start exit timer now.");
            // lets just see if we get more connections coming in before the timeout comes
            Log.i(TAG, "Data state" + "All addresses connected, will start exit timer now.");
            this.isAllAvailableClientsConnectedAndSynced = true;
            this.disconnectGroupOwnerTimeOut.start();
        }
    }

    private void startTestConnection(Socket socket, boolean shouldInitiate) {
        Log.i(TAG, "Initial Connection established");
        mTestConnectedThread = new ConnectedThread(socket, mHandler);
        mTestConnectedThread.start();
        if (shouldInitiate) {
            this.p2PStateFlow.transit(P2PStateFlow.Transition.SEND_HANDSHAKING_INFORMATION, null);
        }
    }

    @Override
    public void Connected(Socket socket) {
        Log.i(TAG, "Connected to ");
        this.connectedInLastTwoMins = true;
        final Socket socketTmp = socket;
        mTestConnectToThread = null;
        this.p2PStateFlow.resetAllStates();
        startTestConnection(socketTmp, true);
    }

    @Override
    public void GotConnection(Socket socket) {
        Log.i(TAG, "We got incoming connection");
        this.connectedInLastTwoMins = true;
        final Socket socketTmp = socket;
        startListenerThread();
        mTestConnectToThread = null;
        this.p2PStateFlow.resetAllStates();
        startTestConnection(socketTmp, false);
    }

    @Override
    public void ConnectionFailed(String reason) {
        goToNextClientWaiting();
    }

    @Override
    public void ListeningFailed(String reason) {
        startListenerThread();
    }

    @Override
    public void Connected(String address, boolean isGroupOwner) {
        if (isGroupOwner) {
            clientIPAddressList.add(address);

            updateStatus("Connectec", "Connected From remote host: " + address + ", CTread : " + mTestConnectedThread + ", CtoTread: " + mTestConnectToThread);
            Log.i(TAG, "Connectec" + "Connected From remote host: " + address + ", CTread : " + mTestConnectedThread + ", CtoTread: " + mTestConnectToThread);
            if (mTestConnectedThread == null
                    && mTestConnectToThread == null) {
                goToNextClientWaiting();
            }
        } else {
            updateStatus("Connectec", "Connected to remote host: " + address);
            Log.i(TAG, "Connectec" + "Connected to remote host: " + address);
        }
    }

    @Override
    public void GroupInfoChanged(WifiP2pGroup group) {
        // updateStatus("GroupInfoChanged:", "group: " + group.getOwner());
    }

    @Override
    public void ConnectionStateChanged(SyncUtils.ConnectionState newState) {
        updateStatus("ConnectionStateChanged:", "New state: " + newState);
    }

    @Override
    public void ListeningStateChanged(SyncUtils.ReportingState newState) {
        updateStatus("ListeningStateChanged", "New state: " + newState);
    }


    public void updateStatus(String who, String line) {
        final String logWho = who;
        final String status = line;
        this.broadcastCustomStatusUpdateEvent(who, line);
    }

    private void broadcastCustomStatusUpdateEvent(String who, String line) {
        Log.d("sender", "Broadcasting message customStatusUpdateEvent");
        this.timeCounter = 0;
        Intent intent = new Intent(customStatusUpdateEvent);
        // You can also include some extra data.
        intent.putExtra("who", who);
        intent.putExtra("line", line);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    public void onDestroy() {
        this.disconnectGroupOwnerTimeOut.cancel();
        this.stopConnector();
        this.mStatusChecker = null;
        updateStatus(TAG, "onDestroy");
    }


    // Manage photo

    public static String createProfilePhoto(String generateUserId, byte[] contents, Context context) {
        Boolean canWrite = false;
        String fileName = null;
        File pathDir = context.getExternalFilesDir(null);
        if (null == pathDir) {
            pathDir = context.getFilesDir();
        }

        canWrite = pathDir.canWrite();

        if (canWrite) {
            fileName = P2PSyncManager.generateUserPhotoFileName(generateUserId);
            File file = new File(pathDir, fileName);
            try {
                // Make sure the Pictures directory exists.
                if (!checkIfFileExists(fileName, context)) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                OutputStream os = new FileOutputStream(file);
                String str = new String(contents, StandardCharsets.UTF_8);
                Log.i(TAG, "created profile photo:" + fileName + " with contents" + str);
                os.write(contents);
                os.close();

                // update shared preferences
                SharedPreferences pref = context.getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("PROFILE_PHOTO", generateUserId);
                editor.commit(); // commit changes
            } catch (IOException e) {
                // Unable to create file, likely because external storage is
                // not currently mounted.
                Log.w("ExternalStorage", "Error writing " + file, e);
            }

        } else {
            Log.i(TAG, "could not write to external storage");
        }
        return fileName;
    }


    public static byte[] getProfilePhotoContents(String fileName, Context context) {
        byte[] results = null;
        File pathDir = context.getExternalFilesDir(null);
        if (null == pathDir) {
            pathDir = context.getFilesDir();
        }
        try {
            File file = new File(pathDir, fileName);
            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream bis;
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(bytes, 0, bytes.length);
            bis.close();
            results = bytes;
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            results = null;
        }
        return results;
    }


    public static boolean checkIfFileExists(String fileName, Context context) {
        File pathDir = context.getExternalFilesDir(null);
        if (null == pathDir) {
            pathDir = context.getFilesDir();
        }
        File file = new File(pathDir, fileName);
        return file.exists();
    }


    public static String generateUserPhotoFileName(String userId) {
        return "profile-" + userId + profileFileExtension;
    }

    public int getTimeCounter() {
        return timeCounter;
    }

    public void setTimeCounter(int timeCounter) {
        this.timeCounter = timeCounter;
    }
}
