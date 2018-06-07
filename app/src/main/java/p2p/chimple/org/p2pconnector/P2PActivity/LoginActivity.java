package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;
import p2p.chimple.org.p2pconnector.db.entity.P2PUserIdDeviceIdAndMessage;
import p2p.chimple.org.p2pconnector.scheduler.JobUtils;
import p2p.chimple.org.p2pconnector.sync.SyncUtils;

import static junit.framework.Assert.assertEquals;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customStatusUpdateEvent;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.customTimerStatusUpdateEvent;

public class LoginActivity extends Activity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private LoginActivity that = this;

    ListView usersList;
    public static TextView userIdStatusTitle, userIdSelectedStatus;

    String userid=null;
    String[] listItem=null;
    ArrayAdapter<String> adapter=null;
    List<P2PSyncInfo> userDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(customStatusUpdateEvent));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageTimerReceiver, new IntentFilter(customTimerStatusUpdateEvent));

        userIdStatusTitle = (TextView) findViewById(R.id.userIdStatusTitle);
        userIdSelectedStatus = (TextView) findViewById(R.id.userIdSelectedStatus);
        usersList = (ListView) findViewById(R.id.usersList);

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0);
        userid = pref.getString("USER_ID", null); // getting String

        if (userid!=null){
            String user = userIdSelectedStatus.getText().toString();
            userIdSelectedStatus.setText(user+" : "+userid);
        }

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
                String Status = userIdSelectedStatus.getText().toString();
                String DefaultStatus = "Select Your ID";
                Log.i("userId status", Status);
                if (Status.equals(DefaultStatus)){
                    Toast.makeText(getApplicationContext(),"Not Selected user \n Select your user ID ",Toast.LENGTH_LONG).show();
                    Log.i(TAG,"You have not yet Selected your user id \n Select your user ID");
                }else{
                    userid = userIdSelectedStatus.getText().toString();
                    Log.i("Selected user",userid);
                    JobUtils.scheduledJob(getApplicationContext(), true);
                }

            }
        });

        List<P2PUserIdDeviceIdAndMessage> users = P2PDBApiImpl.getInstance(getApplicationContext()).getUsers();
        Toast.makeText(getApplicationContext(),String.valueOf(users.size()),Toast.LENGTH_LONG).show();
        List<String> userIds = new ArrayList<String>();
        for (int i = 0; i < users.size(); i++) {
            P2PUserIdDeviceIdAndMessage um = users.get(i);
            Log.i("MainActivity AllUsers", um.userId);
            userIds.add(um.userId);
        }
        listItem = userIds.toArray(new String[userIds.size()]);
        adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_list_item_1, listItem);
        usersList.setAdapter(adapter);

        Toast.makeText(getApplicationContext(),"Select Users From the List ",Toast.LENGTH_LONG).show();

        usersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(getApplicationContext(),"MainActivity: "+value, Toast.LENGTH_SHORT).show();

                userid = value;
                userIdSelectedStatus.setText("Select Your ID : "+value);

                userDetails = P2PDBApiImpl.getInstance(getApplicationContext()).getInfoByUserId(value);
//                Log.i("getInfoByUserId", userDetails.deviceId);
//                setSharedPreferences();
            }
        });

        this.execute();
    }

    public void execute() {
        String Status = userIdSelectedStatus.getText().toString();
        String DefaultStatus = "Select Your ID";
        Log.i("userId status", Status);
        if (Status.equals(DefaultStatus)){
            Toast.makeText(getApplicationContext(),"No users found. \n Register first",Toast.LENGTH_LONG).show();
        }else{
            JobUtils.scheduledJob(getApplicationContext(), false);
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

    private void setSharedPreferences(){
        SharedPreferences pref = getApplicationContext().getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        Log.i("createShardProfilePref", "created UUID User:" + userDetails);
//        editor.putString("USER_ID", USERID_UUID);
//        editor.putString("DEVICE_ID", UUID.randomUUID().toString());
        editor.commit(); // commit changes
    }

}

