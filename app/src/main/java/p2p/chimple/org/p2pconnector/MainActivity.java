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
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private P2PSyncManager p2pSyncManager;
    private MainActivity that = this;

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
    protected void onCreate(Bundle savedInstanceState) {
        this.p2pSyncManager = new P2PSyncManager(this.getApplicationContext(), this);
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
                that.p2pSyncManager.toggle();
            }
        });


        this.execute();
    }

    public void execute() {
        timeHandler = new Handler();
        mStatusChecker.run();
        this.p2pSyncManager.execute();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.p2pSyncManager.onDestroy();
        updateStatus(TAG, "Destroying MainActivity");
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
