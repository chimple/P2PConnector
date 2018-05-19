package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import p2p.chimple.org.p2pconnector.R;
import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApi;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;
import p2p.chimple.org.p2pconnector.db.dao.P2PSyncInfoDao;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;
import static p2p.chimple.org.p2pconnector.application.P2PApplication.db;

public class TakeProfilePic extends Activity {

    Button TakePhoto,SetProfilePic;
    ImageView imageView;

    private P2PDBApiImpl p2pdbapi = null;
    private P2PSyncInfoDao p2PSyncInfoDao;

    String fileName=null;
    String userId=null;
    String deviceId=null;
    private static final int CAMERA_PHOTO = 1;
    private Uri imageToUploadUri;
    boolean defaultImage = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_profile_pic);

        TakePhoto=(Button) findViewById(R.id.buttonTakePhoto);
        SetProfilePic=(Button) findViewById(R.id.buttonSetProfilePic);
        imageView=(ImageView) findViewById(R.id.image_view);

        SharedPreferences pref = getSharedPreferences(P2P_SHARED_PREF, 0);
        fileName = pref.getString("PROFILE_PHOTO", null); // getting String
        userId = pref.getString("USER_ID", null); // getting String
        deviceId = pref.getString("DEVICE_ID", null); // getting String
        Log.i("buttonAllUsers:","PROFILE_PHOTO filename :"+fileName+", USER_ID :  "+userId+", DEVICE_ID :  "+deviceId);

        p2PSyncInfoDao = db.p2pSyncDao();
        p2pdbapi = P2PDBApiImpl.getInstance(db,getApplicationContext());

        TakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defaultImage=false;
                Intent chooserIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File folder = new File(getExternalFilesDir(null), "P2P_IMAGES");
                if (!folder.exists()){
                    folder.mkdirs();
                }
                File f = new File(folder, userId+".jpg");
                chooserIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                imageToUploadUri = Uri.fromFile(f);
                startActivityForResult(chooserIntent, CAMERA_PHOTO);
            }
        });

        SetProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap bm = BitmapFactory.decodeResource( getResources(), R.drawable.photo);

                String value=getApplicationContext().getExternalFilesDir(null).getPath();
                Log.i("SetProfilePic","do some action using the image path: "+value);
                Toast.makeText(getApplicationContext(),value, Toast.LENGTH_LONG).show();
                if (defaultImage){
                    try {
                        File file = new File(getApplicationContext().getExternalFilesDir(null)+"/P2P_IMAGES",userId+".jpg" );
                        FileOutputStream outStream = null;
                        outStream = new FileOutputStream(file);
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Intent intent = new Intent(getApplicationContext(), NeighbourList.class);
                intent.putExtra("MyId",userId);
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
                getContentResolver().notifyChange(selectedImage, null);
                Bitmap reducedSizeBitmap = getBitmap(imageToUploadUri.getPath());
                Log.i("reducedSizeBitmap", String.valueOf(reducedSizeBitmap));
                if(reducedSizeBitmap != null){
                    imageView.setImageBitmap(reducedSizeBitmap);
//                    Button uploadImageButton = (Button) findViewById(R.id.uploadUserImageButton);
//                    uploadImageButton.setVisibility(View.VISIBLE);
                }else{
                    Toast.makeText(this,"reducedSizeBitmap : Error while capturing Image",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(this,"imageToUploadUri: Error while capturing Image",Toast.LENGTH_LONG).show();
            }
        }

    }

    private Bitmap getBitmap(String path) {
        Log.i("image path ",path);

        Uri uri = Uri.fromFile(new File(path));
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();


            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d("", "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                Log.d("", "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            Log.d("", "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return b;
        } catch (IOException e) {
            Log.e("", e.getMessage(), e);
            return null;
        }
    }
}
