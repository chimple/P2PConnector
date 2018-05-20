package p2p.chimple.org.p2pconnector.P2PActivity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;

import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class NeighbourList extends AppCompatActivity {

    ListView listView;
    String[] listItem=null;
    ArrayAdapter<String> adapter=null;

//    private P2PSyncManager syncManager=instance;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    String MyId=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neighbour_list);

        listView = (ListView) findViewById(R.id.neighbourList);

        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = P2PDBApiImpl.getInstance(getApplicationContext());

        Intent intent=getIntent();
        MyId=intent.getStringExtra("MyId");
        Log.i("my",intent.getStringExtra("MyId"));


//        if(instance!=null) {
//            Map<String, WifiDirectService> users = instance.getNeighbours();
//            if (users != null) {
//                Log.i("Neighbours list", String.valueOf(users));
//                listItem = users.keySet().toArray(new String[0]);
//            }
//        }else{
//            String temp="no users found";
//            Log.i("Neighbours list","No users has been found");
//            listItem[0] = temp;
//        }
//
//        }
//        adapter = new ArrayAdapter<String>(getBaseContext(),
//                android.R.layout.simple_list_item_1, listItem);
//        listView.setAdapter(adapter);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                // TODO Auto-generated method stub
//                String value=adapter.getItem(position);
//                Toast.makeText(getApplicationContext(),"NeighbourList: "+value, Toast.LENGTH_SHORT).show();
//
//                Intent intent = new Intent(getApplicationContext(), ActionTypeActivity.class);
//                intent.putExtra("MyId",MyId);
//                intent.putExtra("NeighbourId",value);
//                startActivity(intent);
//            }
//        });

    }
}
