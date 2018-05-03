package p2p.chimple.org.p2pconnector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import p2p.chimple.org.p2pconnector.P2PActivity.TakeProfilePic;
import p2p.chimple.org.p2pconnector.db.P2PDBApi;
import p2p.chimple.org.p2pconnector.scheduler.JobUtils;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customStatusUpdateEvent;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customTimerStatusUpdateEvent;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private P2PSyncManager p2pSyncManager;
    private MainActivity that = this;

    ImageView imageView;
    P2PDBApi p2pdbapi;
    static final int CAM_REQUEST=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.p2pSyncManager = new P2PSyncManager(this.getApplicationContext());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageTimerReceiver, new IntentFilter(customTimerStatusUpdateEvent));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView=(ImageView) findViewById(R.id.image_view);


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

        Button buttonAdd = (Button) findViewById(R.id.buttonAdd);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent camera = new Intent(getApplicationContext(),TakeProfilePic.class);
                startActivity(camera);
            }
        });

        Button buttonAllUsers = (Button) findViewById(R.id.buttonAllUsers);

        buttonAllUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object obj = new Object();
                obj= p2pdbapi.getUsers();
                Log.i("buttonNeighbour",obj.toString());
            }
        });

        Button buttonNeighbour = (Button) findViewById(R.id.buttonNeighbour);

        buttonNeighbour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object obj = new Object();
                obj= p2pdbapi.getNeighbours();
                Log.i("buttonNeighbour",obj.toString());
            }
        });

        this.execute();
    }

    public void execute() {
//        JobUtils.scheduleJob(getApplicationContext());
        this.p2pSyncManager.execute();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.p2pSyncManager.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        updateStatus(TAG, "Destroying MainActivity");
    }

    public void updateStatus(String who, String line) {
        final String logWho = who;
        final String status = line;
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                that.p2pSyncManager.setTimeCounter(0);
                ((TextView) findViewById(R.id.debugdataBox)).append(logWho + " : " + status + "\n");
            }
        }));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String who = intent.getStringExtra("who");
            String line = intent.getStringExtra("line");
            that.updateStatus(who, line);
        }
    };

    public void updateTimerStatus(final int timeCounter) {
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
            }
        }));
    }

    private BroadcastReceiver mMessageTimerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int timeCounter = intent.getIntExtra("timeCounter", -1);
            that.updateTimerStatus(timeCounter);
        }
    };
}
