package p2p.chimple.org.p2pconnector;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import p2p.chimple.org.p2pconnector.P2PActivity.LoginActivity;
import p2p.chimple.org.p2pconnector.P2PActivity.TakeProfilePic;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity that = this;

    Button regUser, newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        regUser = (Button) findViewById(R.id.regUser);
        newUser = (Button) findViewById(R.id.newUser);


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

    }

}