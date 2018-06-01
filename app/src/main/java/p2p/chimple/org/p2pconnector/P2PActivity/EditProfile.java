package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

import p2p.chimple.org.p2pconnector.MainActivity;
import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.application.P2PApplication;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.DatabaseInitializer;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

public class EditProfile extends Activity {

    String userId;

    Button EditTakePhoto,EditSetProfilePic;
    ImageView editImageView;
    TextView editUserPhoto;
    Bitmap DefaultImage;
    private static final int CAMERA_PHOTO = 1;


    boolean defaultImg = true;
    Bitmap reducedSizeBitmap;
    String USERID_UUID;
    private Uri imageToUploadUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);


        editUserPhoto = (TextView) findViewById(R.id.editUserPhoto);
        EditTakePhoto=(Button) findViewById(R.id.EditTakePhoto);
        EditSetProfilePic=(Button) findViewById(R.id.EditSetProfilePic);
        editImageView=(ImageView) findViewById(R.id.editImageView);

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0);
        userId = pref.getString("USER_ID", null); // getting String
        editUserPhoto.setText(userId);

        File file = new File(getApplicationContext().getExternalFilesDir(null) + "/P2P_IMAGES", "profile-"+userId+".jpg");
        DefaultImage = BitmapFactory.decodeFile(file.getPath());
        editImageView.setImageBitmap(DefaultImage);

        EditTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                defaultImg=false;
                Intent chooserIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File folder = new File(getExternalFilesDir(null), "Cache");
                if (!folder.exists()){
                    folder.mkdirs();
                }
                File f = new File(folder, "DefaultImage.jpg");
                chooserIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                imageToUploadUri = Uri.fromFile(f);
                startActivityForResult(chooserIntent, CAMERA_PHOTO);

            }
        });

        EditSetProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] BYTE=null;

                if (defaultImg){
                    ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.photo, options);
                    bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, bytearrayoutputstream);
                    BYTE = bytearrayoutputstream.toByteArray();
                }else{
                    String value=getApplicationContext().getExternalFilesDir(null).getPath();
                    Log.i("SetProfilePic","do some action using the image path: "+value);
                    Toast.makeText(getApplicationContext(),value, Toast.LENGTH_LONG).show();
                    ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                    reducedSizeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytearrayoutputstream);
                    BYTE = bytearrayoutputstream.toByteArray();
                }

                DatabaseInitializer.populateWithTestData(AppDatabase.getInstance(P2PApplication.getContext()),getApplicationContext(),BYTE);


                String user = LoginActivity.userIdSelectedStatus.getText().toString();
                LoginActivity.userIdSelectedStatus.setText(user+" : "+userId);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult","requestCode: "+requestCode+" resultCode: "+resultCode+" data :"+data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PHOTO && resultCode == Activity.RESULT_OK) {
            if(imageToUploadUri != null){
                Uri selectedImage = imageToUploadUri;
                Log.i("selectedImage", String.valueOf(selectedImage));
                BitmapFactory.Options options = new BitmapFactory.Options();
                File file = new File(imageToUploadUri.getPath());
                //File Size compression
                if (file.length() < 1000000 ){
                    options.inSampleSize = 1;
                }else if(file.length() < 2000000){
                    options.inSampleSize = 2;
                }else {
                    options.inSampleSize = 3;
                }

                reducedSizeBitmap = BitmapFactory.decodeFile(imageToUploadUri.getPath(),options);
                Log.i("reducedSizeBitmap", String.valueOf(reducedSizeBitmap.toString()));
                if(reducedSizeBitmap != null){
                    editImageView.setImageBitmap(reducedSizeBitmap);
                }else{
                    Toast.makeText(this,"reducedSizeBitmap : Error while capturing Image",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(this,"imageToUploadUri: Error while capturing Image",Toast.LENGTH_LONG).show();
            }
        }

    }


}
