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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity that = this;

    private Context context;
    ImageView imageView;
    ListView listView;
    String[] listItem=null;
    ArrayAdapter<String> adapter=null;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    private P2PSyncInfo p2PSyncInfo;
    static final int CAM_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageTimerReceiver, new IntentFilter(customTimerStatusUpdateEvent));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.image_view);

        listView = (ListView) findViewById(R.id.list);


        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = new P2PDBApiImpl(db,getApplicationContext());


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

                for (int i = 0; i < users.size(); i++) {
                    Log.i("buttonAllUsers", users.get(i));
                    listItem = users.toArray(new String[i]);
                }
                adapter = new ArrayAdapter<String>(getBaseContext(),
                        android.R.layout.simple_list_item_1, listItem);
                listView.setAdapter(adapter);
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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(getApplicationContext(),value, Toast.LENGTH_SHORT).show();

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
