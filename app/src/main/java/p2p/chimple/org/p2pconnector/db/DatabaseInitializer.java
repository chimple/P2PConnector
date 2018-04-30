package p2p.chimple.org.p2pconnector.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import p2p.chimple.org.p2pconnector.sync.P2PSyncManager;

import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

public class DatabaseInitializer {

    private static final String TAG = DatabaseInitializer.class.getName();
    private P2PDBApi p2PDBApi;

    public static void populateAsync(@NonNull final AppDatabase db, @NonNull final Context context, @NonNull P2PDBApi api) {
        PopulateDbAsync task = new PopulateDbAsync(db, context, api);
        task.execute();
    }

    private static void populateWithTestData(AppDatabase db, Context context) {
        SharedPreferences pref = context.getSharedPreferences(P2P_SHARED_PREF, 0);
        String generateUserId = pref.getString("USER_ID", null); // getting String
        Log.i(TAG, "generateUserId" + generateUserId);
        P2PSyncManager.createProfilePhoto(generateUserId, "hello - welcome".getBytes(), context);
        Log.i(TAG, "generateUserId" + generateUserId);
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("database.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        // message contains userId, deviceId, recipientUserId, message, messageType

        String line = "";
        db.beginTransaction();
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] columns = line.split(",");

                if (columns.length < 1) {
                    Log.d(TAG + "AppDatabase", "Skipping bad row");
                }

                String userId = columns[0];
                String deviceId = columns[1];
                String recipientUserId = columns[2];
                String message = columns[3];
                String messageType = columns[4];

                new P2PDBApiImpl(db, context).persistMessage(userId, deviceId, recipientUserId, message, messageType);
            }

            new P2PDBApiImpl(db, context).upsertProfile();
            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final AppDatabase mDb;
        private Context context;
        private P2PDBApi api;

        PopulateDbAsync(AppDatabase db, Context context, P2PDBApi api) {
            mDb = db;
            this.context = context;
            this.api = api;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            populateWithTestData(mDb, this.context);
            return null;
        }

    }
}