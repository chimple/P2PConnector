package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

import static junit.framework.Assert.assertEquals;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class LoginActivity extends Activity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private LoginActivity that = this;

    private Context context;
    ImageView imageView;
    ListView listView;
    String[] listItem=null;
    ArrayAdapter<String> adapter=null;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    private P2PSyncInfo p2PSyncInfo;
    static final int CAM_REQUEST = 1;

    String fileName=null;
    String userId=null;
    String deviceId=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        imageView = (ImageView) findViewById(R.id.image_view);

        listView = (ListView) findViewById(R.id.list);


        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = P2PDBApiImpl.getInstance(getApplicationContext());

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0);
        fileName = pref.getString("PROFILE_PHOTO", null); // getting String
        userId = pref.getString("USER_ID", null); // getting String
        deviceId = pref.getString("DEVICE_ID", null); // getting String
        Log.i("buttonAllUsers:","PROFILE_PHOTO filename :"+fileName+", USER_ID :  "+userId+", DEVICE_ID :  "+deviceId);


        List<String> users = p2pdbapi.getUsers();
        Toast.makeText(getApplicationContext(),String.valueOf(users.size()),Toast.LENGTH_LONG).show();
        for (int i = 0; i < users.size(); i++) {
            Log.i("MainActivity AllUsers", users.get(i));
            listItem = users.toArray(new String[i]);
        }
        adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_list_item_1, listItem);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(getApplicationContext(),"MainActivity: "+value, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), NeighbourList.class);
                intent.putExtra("MyId",value);
                startActivity(intent);
            }
        });
    }


}

