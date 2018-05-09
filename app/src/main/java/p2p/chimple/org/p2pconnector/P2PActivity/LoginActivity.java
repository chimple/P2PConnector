package p2p.chimple.org.p2pconnector.P2PActivity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import p2p.chimple.org.p2pconnector.MainActivity;
import p2p.chimple.org.p2pconnector.R;

public class LoginActivity extends AppCompatActivity {

    Button regUser,newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        regUser = (Button) findViewById(R.id.regUser);
        newUser = (Button) findViewById(R.id.newUser);

        regUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
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
