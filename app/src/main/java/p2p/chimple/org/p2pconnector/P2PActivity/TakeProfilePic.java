package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import p2p.chimple.org.p2pconnector.MainActivity;
import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.application.P2PApplication;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.DatabaseInitializer;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;

import static p2p.chimple.org.p2pconnector.application.P2PApplication.getContext;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class TakeProfilePic extends Activity {

    Button TakePhoto,SetProfilePic;
    ImageView imageView;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;
    AppDatabase db = AppDatabase.getInstance(P2PApplication.getContext());

    String fileName=null;
    String userId=null;
    String deviceId=null;
    private static final int CAMERA_PHOTO = 1;
    private Uri imageToUploadUri;
    boolean defaultImage = true;
    Bitmap reducedSizeBitmap;
    String USERID_UUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_profile_pic);

        TakePhoto=(Button) findViewById(R.id.buttonTakePhoto);
        SetProfilePic=(Button) findViewById(R.id.buttonSetProfilePic);
        imageView=(ImageView) findViewById(R.id.image_view);

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0);
        userId = pref.getString("USER_ID", null); // getting String

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 3;
        Bitmap bit = BitmapFactory.decodeResource(getResources(), R.drawable.photo, options);
        imageView.setImageBitmap(bit);


        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = P2PDBApiImpl.getInstance(getApplicationContext());

        TakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                defaultImage=false;
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

        SetProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] BYTE=null;

                createShardProfilePreferences();

                if (defaultImage){
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

                DatabaseInitializer.populateWithTestData(db,getApplicationContext(),BYTE);


//                String user = LoginActivity.userIdSelectedStatus.getText().toString();
//                LoginActivity.userIdSelectedStatus.setText(user+" : "+USERID_UUID);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                intent.putExtra("MyId",userId);
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

                reducedSizeBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imageToUploadUri.getPath(), options), 512, 512);
                Log.i("reducedSizeBitmap", String.valueOf(reducedSizeBitmap.toString()));
                if(reducedSizeBitmap != null){
                    imageView.setImageBitmap(reducedSizeBitmap);
                }else{
                    Toast.makeText(this,"reducedSizeBitmap : Error while capturing Image",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(this,"imageToUploadUri: Error while capturing Image",Toast.LENGTH_LONG).show();
            }
        }

    }


    private void createShardProfilePreferences() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(P2P_SHARED_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        USERID_UUID = UUID.randomUUID().toString();
        Log.i("createShardProfilePref", "created UUID User:" + USERID_UUID);
        editor.putString("USER_ID", USERID_UUID);
        editor.putString("DEVICE_ID", UUID.randomUUID().toString());
        editor.commit(); // commit changes
    }
}
