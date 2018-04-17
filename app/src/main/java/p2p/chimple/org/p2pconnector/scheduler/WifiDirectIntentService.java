package p2p.chimple.org.p2pconnector.scheduler;

import android.app.Service;
import android.app.job.JobParameters;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import p2p.chimple.org.p2pconnector.sync.CommunicationCallBack;
import p2p.chimple.org.p2pconnector.sync.CommunicationThread;
import p2p.chimple.org.p2pconnector.sync.ConnectToThread;
import p2p.chimple.org.p2pconnector.sync.ConnectedThread;
import p2p.chimple.org.p2pconnector.sync.P2POrchester;
import p2p.chimple.org.p2pconnector.sync.P2POrchesterCallBack;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.JOB_PARAMS;
import static p2p.chimple.org.p2pconnector.scheduler.P2PHandShakingJobService.P2P_SYNC_RESULT_RECEIVED;

public class WifiDirectIntentService extends Service implements P2POrchesterCallBack, CommunicationCallBack, Handler.Callback {
    private static final String TAG = WifiDirectIntentService.class.getSimpleName();
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private JobParameters currentJobParams;
    private CountDownTimer disconnectGroupOwnerTimeOut;
    private List<String> clientIPAddressList;
    private P2POrchester mWDConnector = null;
    private CommunicationThread mTestListenerThread = null;
    private ConnectToThread mTestConnectToThread = null;
    private ConnectedThread mTestConnectedThread = null;
    private WifiDirectIntentService that = this;
    final private int TestChatPortNumber = 8768;
    private boolean gotFirstMessage = false;
    private Handler handler = new Handler((Handler.Callback) this);

    public WifiDirectIntentService() {
        super();
        Log.i(TAG, "WifiDirectIntentService Contructor");
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ConnectedThread.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                String writeMessage = new String(writeBuf);
                Log.i(TAG + "CHAT", "Wrote: " + writeMessage);
                break;
            case ConnectedThread.MESSAGE_READ:
                sayAck("WHAT I GOT HERERE: " + msg.arg1);
                if(gotFirstMessage) {
                    Log.i(TAG + "CHAT", "we got Ack message back, so lets disconnect.");
                    // we got Ack message back, so lets disconnect
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            Log.i(TAG + "CHAT", "disconnect disabled for testing.");
                            goToNextClientWaiting();
                        }
                    }, 1000);
                }else{
                    byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    gotFirstMessage = true;
                    Log.i(TAG + "CHAT", "Got first message: " + readMessage);
                }
                break;
            case ConnectedThread.SOCKET_DISCONNEDTED: {
                Log.i(TAG + "CHAT", "WE are Disconnected now.");
                stopConnectedThread();
            }
            break;
        }
        return true;
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
        clientIPAddressList = new ArrayList<String>();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }


    public void execute() {
        disconnectGroupOwnerTimeOut = new CountDownTimer(30000, 4000) {
            public void onTick(long millisUntilFinished) {
                // not using
            }

            public void onFinish() {
                // no clients queuing up, thus lets reset the group now.
                stopConnector();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    //Lets give others chance on creating new group before we come back online
                    public void run() {
                        StartConnector();
                    }
                }, 10000);
            }
        };

        //Start Init

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        StartConnector();
    }

    public void StartConnector() {
        //lets be ready for incoming test communications
        Log.i("Whatsup", "starting listener now, and connector");
        startListenerThread();
        mWDConnector = new P2POrchester(that, that);
    }

    public void stopConnector() {
        stopConnectedThread();
        stopConnectToThread();
        stopListenerThread();

        if (mWDConnector != null) {
            mWDConnector.cleanUp();
            mWDConnector = null;
        }
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
        this.disconnectGroupOwnerTimeOut.cancel();
        stopConnector();
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
        this.execute();

        Intent result = new Intent(P2P_SYNC_RESULT_RECEIVED);
        result.putExtra(JOB_PARAMS, currentJobParams);
        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
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


    private void goToNextClientWaiting() {
        stopConnectedThread();
        stopConnectToThread();
        this.disconnectGroupOwnerTimeOut.cancel();

        if (clientIPAddressList.size() > 0) {
            //With this test we'll just handle each client one-by-one in order they got connected
            String Address = clientIPAddressList.get(0);
            clientIPAddressList.remove(0);

            Log.i("Data state", "Will connect to " + Address);
            mTestConnectToThread = new ConnectToThread(that, Address, TestChatPortNumber);
            mTestConnectToThread.start();

        } else {
            Log.i("Data state", "All addresses connected, will start exit timer now.");
            // lets just see if we get more connections coming in before the timeout comes
            this.disconnectGroupOwnerTimeOut.start();
        }
    }

    public void startTestConnection(Socket socket, boolean outGoing) {
        mTestConnectedThread = new ConnectedThread(socket, handler);
        mTestConnectedThread.start();
        sayHi();
    }


    private void sayAck(String data) {
        if (mTestConnectedThread != null) {
            String message = "Got:"+data;
            Log.i(TAG + "CHAT REPLIED", "sayAck");
            mTestConnectedThread.write(message.getBytes());
        }
    }


    private void sayHi() {
        if (mTestConnectedThread != null) {
            Random ran = new Random(System.currentTimeMillis());
            long millisInFuture = 5000 + (ran.nextInt(50000));
            String message = "Hello from " + millisInFuture + " Time.....";
            Log.i(TAG +"CHAT", "sayHi");
            mTestConnectedThread.write(message.getBytes());
        }
    }

    @Override
    public void Connected(Socket socket) {
        Log.i("Whatsup", "Connected to ");
        final Socket socketTmp = socket;
        mTestConnectToThread = null;
        startTestConnection(socketTmp, true);
    }

    @Override
    public void GotConnection(Socket socket) {
        Log.i("Whatsup", "We got incoming connection");
        final Socket socketTmp = socket;
        startListenerThread();
        mTestConnectToThread = null;
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

            Log.i("Connectec", "Connected From remote host: " + address + ", CTread : " + mTestConnectedThread + ", CtoTread: " + mTestConnectToThread);

            if (mTestConnectedThread == null
                    && mTestConnectToThread == null) {
                goToNextClientWaiting();
            }
        } else {
            Log.i("Connectec", "Connected to remote host: " + address);
        }
    }

    @Override
    public void GroupInfoChanged(WifiP2pGroup group) {
        Log.i("GroupInfoChanged:", "group: " + group.getOwner());
    }

    @Override
    public void ConnectionStateChanged(SyncUtils.ConnectionState newState) {
        Log.i("ConnectionStateChanged:", "New state: " + newState);
    }

    @Override
    public void ListeningStateChanged(SyncUtils.ReportingState newState) {
        Log.i("ListeningStateChanged", "New state: " + newState);
    }
}
