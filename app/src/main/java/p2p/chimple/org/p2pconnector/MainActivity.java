package p2p.chimple.org.p2pconnector;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import p2p.chimple.org.p2pconnector.P2PActivity.TakeProfilePic;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApi;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdMessage;
import p2p.chimple.org.p2pconnector.scheduler.JobUtils;
import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static junit.framework.Assert.assertEquals;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.IMMEDIATE_JOB_TIMINGS;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.REGULAR_JOB_TIMINGS;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customStatusUpdateEvent;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customTimerStatusUpdateEvent;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity that = this;

    private AppDatabase db;
    private Context context;
    ImageView imageView;
    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    static final int CAM_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageTimerReceiver, new IntentFilter(customTimerStatusUpdateEvent));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.image_view);

        try {
            db = Room.inMemoryDatabaseBuilder(getApplicationContext(), AppDatabase.class)
                    .allowMainThreadQueries()
                    .build();

        } catch (Exception ex) {
            Log.i("test", ex.getMessage());
        }
//        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = new P2PDBApiImpl(db,getApplicationContext());

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("USER_ID", UUID.randomUUID().toString());
        editor.putString("DEVICE_ID", UUID.randomUUID().toString());
        editor.putString("PROFILE_PHOTO", "photo1.jpg");
        editor.commit(); // commit changes


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

                JobUtils.scheduledJob(getApplicationContext(), IMMEDIATE_JOB_TIMINGS);
            }
        });

        Button buttonAdd = (Button) findViewById(R.id.buttonAdd);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent camera = new Intent(getApplicationContext(), TakeProfilePic.class);
                startActivity(camera);
            }
        });

        Button buttonAllUsers = (Button) findViewById(R.id.buttonAllUsers);

        buttonAllUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> users = p2pdbapi.getUsers();
                Log.i("buttonAllUsers", String.valueOf(users));
//                assertEquals(users.size(), 2);
//                List<String> result = new ArrayList();
//                result = that.p2pdbapi.getUsers();
//                Log.i("buttonAllUsers", String.valueOf(result));
            }
        });

        Button buttonNeighbour = (Button) findViewById(R.id.buttonNeighbour);

        buttonNeighbour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object obj = new Object();
                obj = P2PSyncManager.getInstance(getApplicationContext()).getNeighbours();
                Log.i("buttonNeighbour", String.valueOf(obj));
            }
        });

        this.execute();
    }

    public void execute() {
        JobUtils.scheduledJob(getApplicationContext(), REGULAR_JOB_TIMINGS);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        updateStatus(TAG, "Destroying MainActivity");
    }

    public void updateStatus(String who, String line) {
        final String logWho = who;
        final String status = line;
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
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
