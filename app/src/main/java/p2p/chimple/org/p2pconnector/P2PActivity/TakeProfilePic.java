package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.P2PDBApi;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

public class TakeProfilePic extends Activity {

    Button button;
    ImageView imageView;

    P2PDBApiImpl p2pdbapi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_profile_pic);

        button=(Button) findViewById(R.id.buttonTakePhoto);
        imageView=(ImageView) findViewById(R.id.image_view);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                File file = getTempFile(getApplicationContext(),"");
//                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
                startActivityForResult(camera_intent,1);


            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult","requestCode:"+requestCode+"resultCode"+resultCode+"data"+data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bitmap);
                String photojson=getStringFromBitmap(bitmap);
                boolean status = p2pdbapi.persistProfileMessage(photojson);
                if (status){
                    Log.i("onActivityResult","successfull");
                }else{
                    Log.i("onActivityResult","Failure");
                }

            }
        }
    }

    private String getStringFromBitmap(Bitmap bitmapPicture) {
        /*
         * This functions converts Bitmap picture to a string which can be
         * JSONified.
         * */
        final int COMPRESSION_QUALITY = 100;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }
}
