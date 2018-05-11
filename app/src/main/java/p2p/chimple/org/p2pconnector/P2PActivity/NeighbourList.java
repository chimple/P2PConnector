package p2p.chimple.org.p2pconnector.P2PActivity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;
import p2p.chimple.org.p2pconnector.db.entity.P2PSyncInfo;

import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class NeighbourList extends AppCompatActivity {

    ListView listView;
    String[] listItem=null;
    ArrayAdapter<String> adapter=null;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    String MyId=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neighbour_list);

        listView = (ListView) findViewById(R.id.neighbourList);

        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = new P2PDBApiImpl(db,getApplicationContext());

        Intent intent=getIntent();
        MyId=intent.getStringExtra("MyId");
        Log.i("NeighbourList",intent.getStringExtra("MyId"));

        List<String> users = p2pdbapi.getUsers();
        for (int i = 0; i < users.size(); i++) {
            Log.i("NeighbourList AllUsers", users.get(i));
            if (!users.get(i).equals(MyId)){
                Log.i("NList AllUsers loop", users.get(i));
                listItem = users.toArray(new String[i]);
            }

        }
        adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_list_item_1, listItem);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(getApplicationContext(),"NeighbourList: "+value, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), ActionTypeActivity.class);
                intent.putExtra("MyId",MyId);
                intent.putExtra("NeighbourId",value);
                startActivity(intent);
            }
        });

    }
}