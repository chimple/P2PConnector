package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import p2p.chimple.org.p2pconnector.R;

public class ActionTypeActivity extends Activity {

    Button chat,game;
//    String BuddyId=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_type);
        chat = (Button) findViewById(R.id.buttonChat);
        game = (Button) findViewById(R.id.buttonGame);

        Intent intent = getIntent();
//        BuddyId=intent.getStringExtra("NeighbourId");
//        Toast.makeText(getApplicationContext(),"ActionTypeActivity: "+BuddyId, Toast.LENGTH_SHORT).show();

        ((TextView) findViewById(R.id.MyId)).append(intent.getStringExtra("MyId"));
        ((TextView) findViewById(R.id.NeighbourId)).append(intent.getStringExtra("NeighbourId"));

        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("CHAT","clicked on chat button");

            }
        });

        game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("GAME","clicked on game button");

            }
        });
    }
}
