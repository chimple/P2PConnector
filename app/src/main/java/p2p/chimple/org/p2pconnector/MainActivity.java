package p2p.chimple.org.p2pconnector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import p2p.chimple.org.p2pconnector.P2PActivity.LoginActivity;
import p2p.chimple.org.p2pconnector.P2PActivity.TakeProfilePic;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.scheduler.JobUtils;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customStatusUpdateEvent;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customTimerStatusUpdateEvent;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity that = this;

    Button regUser, newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageTimerReceiver, new IntentFilter(customTimerStatusUpdateEvent));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        regUser = (Button) findViewById(R.id.regUser);
        newUser = (Button) findViewById(R.id.newUser);

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
                if (P2PDBApiImpl.getInstance(getApplicationContext()).getUsers().size() < 1 ){
                    Toast.makeText(getApplicationContext(),"You have not yet registered \n Register first",Toast.LENGTH_LONG).show();
                    Log.i(TAG,"You have not yet registered \n Register first");
                }else{
                    JobUtils.scheduledJob(getApplicationContext(), true);
                }

            }
        });

        regUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (P2PDBApiImpl.getInstance(getApplicationContext()).getUsers().size() < 1 ){
                    Toast.makeText(getApplicationContext(),"No users found. \n Register first",Toast.LENGTH_LONG).show();
                    Log.i(TAG,"No users found. \n Register first");
                }else{
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        newUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TakeProfilePic.class);
                startActivity(intent);
            }
        });
        this.execute();
    }

    public void execute() {
        if (P2PDBApiImpl.getInstance(getApplicationContext()).getUsers().size() >= 1 ){
            JobUtils.scheduledJob(getApplicationContext(), false);
        }else{
            Toast.makeText(getApplicationContext(),"No users found. \n Register first",Toast.LENGTH_LONG).show();
        }
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

    public void updateTimerStatus(final String timeCounter) {
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                ((TextView) findViewById(R.id.TimeBox)).setText(timeCounter);
            }
        }));
    }

    private BroadcastReceiver mMessageTimerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String timeCounter = intent.getStringExtra("timeCounter");
            that.updateTimerStatus(timeCounter);
        }
    };
}