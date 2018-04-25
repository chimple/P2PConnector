package p2p.chimple.org.p2pconnector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.entity.HandShakingInfo;
import p2p.chimple.org.p2pconnector.sync.CommunicationCallBack;
import p2p.chimple.org.p2pconnector.sync.CommunicationThread;
import p2p.chimple.org.p2pconnector.sync.ConnectToThread;
import p2p.chimple.org.p2pconnector.sync.ConnectedThread;
import p2p.chimple.org.p2pconnector.sync.P2POrchester;
import p2p.chimple.org.p2pconnector.sync.P2POrchesterCallBack;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

public class MainActivity extends AppCompatActivity implements P2POrchesterCallBack, CommunicationCallBack, Handler.Callback {
    private static final String TAG = "MainActivity";
    private MainActivity that = this;
    private CountDownTimer disconnectGroupOwnerTimeOut;
    private List<String> clientIPAddressList = new ArrayList<String>();
    private P2POrchester mWDConnector = null;
    private CommunicationThread mTestListenerThread = null;
    private ConnectToThread mTestConnectToThread = null;
    private ConnectedThread mTestConnectedThread = null;
    final private int TestChatPortNumber = 8768;
    private boolean handShakingInformationReceived = false;
    private boolean handShakingInformationSent = false;
    private boolean allSyncInformationSent = false;
    private boolean allSyncInformationReceived = false;
    private Handler mHandler = new Handler((Handler.Callback) this);
    List<HandShakingInfo> handShakingReceivedInfos = null;

    StringBuffer sBuffer = new StringBuffer();
    //Status
    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            timeCounter = timeCounter + 1;
            ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ConnectedThread.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                String writeMessage = new String(writeBuf);
                updateStatus(TAG, "Wrote: " + writeMessage);
                break;
            case ConnectedThread.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.i(TAG, "MESSAGE READ:" + readMessage);
                this.processSyncMessages(readMessage);
//                if(readMessage.startsWith("START")) {
//                    sBuffer.setLength(0);
//                    readMessage = readMessage.replaceAll("START", "");
//                    if(readMessage.endsWith("END")) {
//                        sBuffer.append(readMessage);
//                        String finalMessage = sBuffer.toString();
//                        finalMessage = finalMessage.replaceAll("END", "");
//                        Log.i(TAG, "PRODESSING MESSAGE 111:" + finalMessage);
//                        this.processSyncMessages(finalMessage);
//                    } else  {
//                        sBuffer.append(readMessage);
//                    }
//
//                } else {
//                    if(!readMessage.endsWith("END")) {
//                        sBuffer.append(readMessage);
//                        Log.i(TAG, "APPEND TO BUFFER READ:" + sBuffer.toString());
//                    } else {
//                        sBuffer.append(readMessage);
//                        String finalMessage = sBuffer.toString();
//                        finalMessage = finalMessage.replaceAll("END", "");
//                        Log.i(TAG, "PRODESSING MESSAGE 222:" + finalMessage);
//                        this.processSyncMessages(finalMessage);
//                    }
//                }
                break;
            case ConnectedThread.SOCKET_DISCONNEDTED: {
                updateStatus(TAG + "CHAT", "WE are Stopped now.");
                stopConnectedThread();
            }
            break;
        }
        return true;
    }

    @SuppressLint("LongLogTag")
    private void processSyncMessages(String readMessage) {
        updateStatus(TAG, "information received:" + readMessage);
        if (!handShakingInformationReceived) {
            handShakingInformationReceived = true;
            if (mTestConnectedThread != null) {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                handShakingReceivedInfos = new P2PDBApiImpl(db, getApplicationContext()).deSerializeHandShakingInformationFromJson(readMessage);
                updateStatus(TAG, "handShakingInformationReceived" + readMessage);
                if (!handShakingInformationSent) {
                    sendInitialHandShakingInformation();
                }
            }
        } else {
            disconnectFromSocket();
        }
// else if (!allSyncInformationReceived) {
//            updateStatus(TAG + "allSyncInformationReceived:", readMessage);
//            allSyncInformationReceived = true;
//            if(readMessage != null) {
//                persistAllSyncInformation(readMessage);
//                disconnectFrom();
//            }
//            if (!allSyncInformationSent) {
//                sendAllSyncInformation(handShakingReceivedInfos);
//            }
//        } else {
//            disconnectFrom();
//        }
    }

    //reset commnication flag on next time process starts

    private void resetAllP2PCommunicationFlags() {
        this.handShakingInformationReceived = false;
        this.handShakingInformationSent = false;
        this.allSyncInformationSent = false;
        this.allSyncInformationReceived = false;
    }

    private void disconnectFromSocket() {
        updateStatus(TAG, "we got Ack message back, so lets disconnect.");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                updateStatus(TAG + "CHAT", "disconnect streams now...");
                stopConnectedThread();
                stopConnectToThread();
            }
        }, 1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button showIPButton = (Button) findViewById(R.id.button3);
        showIPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.printLocalIpAddresses(that);
            }
        });

        Button clearButton = (Button) findViewById(R.id.button2);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView) findViewById(R.id.debugdataBox)).setText("");
            }
        });


        Button toggleButton = (Button) findViewById(R.id.buttonToggle);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWDConnector != null) {
                    stopConnector();
                } else {
                    StartConnector();
                }
            }
        });


        this.execute();
    }

    public void execute() {
        timeHandler = new Handler();
        mStatusChecker.run();
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
        updateStatus(TAG, "starting listener now, and connector");
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

        updateStatus(TAG, "Stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.disconnectGroupOwnerTimeOut.cancel();
        stopConnector();
        updateStatus(TAG, "Destroying MainActivity");
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

            updateStatus("Data state", "Will connect to " + Address);
            mTestConnectToThread = new ConnectToThread(that, Address, TestChatPortNumber);
            mTestConnectToThread.start();

        } else {
            updateStatus("Data state", "All addresses connected, will start exit timer now.");
            // lets just see if we get more connections coming in before the timeout comes
            this.disconnectGroupOwnerTimeOut.start();
        }
    }

    public void startTestConnection(Socket socket, boolean outGoing) {
        mTestConnectedThread = new ConnectedThread(socket, mHandler);
        mTestConnectedThread.start();
        Log.i(TAG, "Initial Connection established");
        sendInitialHandShakingInformation();
    }



    @SuppressLint("LongLogTag")
    private void sayAck(String data) {
        if (mTestConnectedThread != null) {
            String message = "Got:" + data;
            Log.i(TAG + "CHAT REPLIED", "sayAck: Thanks for sending" + data);
            mTestConnectedThread.write(message.getBytes());
        }
    }

    private void persistAllSyncInformation(String message) {
        Log.i(TAG, "sync message:" + message);
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        new P2PDBApiImpl(db, getApplicationContext()).persistP2PSyncInfos(message);
    }

    @SuppressLint("LongLogTag")
    private void sendAllSyncInformation(List<HandShakingInfo> infos) {
        if (mTestConnectedThread != null) {
            // generate initial JSON
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            String updatedMessage = "START" + new P2PDBApiImpl(db, getApplicationContext()).buildAllSyncMessages(infos);
            updatedMessage += "END";
            Log.i(TAG + "sendAllSyncInformation:", updatedMessage);
            mTestConnectedThread.write(updatedMessage.getBytes());
//            mTestConnectedThread.write("END:sendAllSyncInformation".getBytes());
            allSyncInformationSent = true;
        }
    }


    @SuppressLint("LongLogTag")
    private void sendInitialHandShakingInformation() {
        if (mTestConnectedThread != null) {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            String initialMessage = new P2PDBApiImpl(db, getApplicationContext()).serializeHandShakingMessage();
            Log.i(TAG + "sendInitialHandShakingInformation:", initialMessage);
            mTestConnectedThread.write(initialMessage.getBytes()) ;
//            mTestConnectedThread.write("END:sendInitialHandShakingInformation".getBytes());
            handShakingInformationSent = true;
        }
    }

//    @SuppressLint("LongLogTag")
//    private void sendInitialHandShakingInformation() {
//        if (mTestConnectedThread != null) {
//            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
//            String initialMessage = "START" + new P2PDBApiImpl(db, getApplicationContext()).serializeHandShakingMessage();
//            initialMessage += "END";
//            Log.i(TAG + "sendInitialHandShakingInformation:", initialMessage);
//            mTestConnectedThread.write(initialMessage.getBytes()) ;
////            mTestConnectedThread.write("END:sendInitialHandShakingInformation".getBytes());
//            handShakingInformationSent = true;
//
//        }
//    }

    @Override
    public void Connected(Socket socket) {
        Log.i(TAG, "Connected to ");
        final Socket socketTmp = socket;
        mTestConnectToThread = null;
        startTestConnection(socketTmp, true);
    }

    @Override
    public void GotConnection(Socket socket) {
        Log.i(TAG, "We got incoming connection");
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
        resetAllP2PCommunicationFlags();
        if (isGroupOwner) {
            clientIPAddressList.add(address);

            updateStatus("Connectec", "Connected From remote host: " + address + ", CTread : " + mTestConnectedThread + ", CtoTread: " + mTestConnectToThread);

            if (mTestConnectedThread == null
                    && mTestConnectToThread == null) {
                goToNextClientWaiting();
            }
        } else {
            updateStatus("Connectec", "Connected to remote host: " + address);
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
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                timeCounter = 0;
                ((TextView) findViewById(R.id.debugdataBox)).append(logWho + " : " + status + "\n");
            }
        }));

    }

}
