package p2p.chimple.org.p2pconnector.P2PActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
                reducedSizeBitmap = BitmapFactory.decodeFile(imageToUploadUri.getPath());
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
