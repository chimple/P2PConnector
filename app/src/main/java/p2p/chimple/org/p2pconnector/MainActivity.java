package p2p.chimple.org.p2pconnector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import p2p.chimple.org.p2pconnector.P2PActivity.TakeProfilePic;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customStatusUpdateEvent;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private P2PSyncManager p2pSyncManager;
    private MainActivity that = this;

    ImageView imageView;

    static final int CAM_REQUEST=1;

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
        this.p2pSyncManager = new P2PSyncManager(this.getApplicationContext());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String who = intent.getStringExtra("who");
            String line = intent.getStringExtra("line");
            that.updateStatus(who, line);
        }
    };



}
