package p2p.chimple.org.p2pconnector.sync;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_PROFILE_PHOTO;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_PROFILE_PHOTO;
import static p2p.chimple.org.p2pconnector.sync.P2PSyncManager.P2P_SHARED_PREF;

public class SendProfilePhotoState implements P2PState {
    private static final String TAG = SendProfilePhotoState.class.getSimpleName();

    private P2PStateFlow.Transition cTransition;

    public SendProfilePhotoState() {
        this.cTransition = SEND_PROFILE_PHOTO;
    }

    public P2PStateFlow.Transition getTransition() {
        return this.cTransition;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, P2PSyncManager manager, String message) {
        // READ PHOTO FILE NAME FROM PROFILE
        AppDatabase db = AppDatabase.getInstance(manager.getContext());
        String photoFileName = new P2PDBApiImpl(db, manager.getContext()).readProfilePhoto();
        SharedPreferences pref = manager.getContext().getSharedPreferences(P2P_SHARED_PREF, 0);
        String userId = pref.getString("USER_ID", null); // getting String
        String deviceId = pref.getString("DEVICE_ID", null); // getting String

        if (manager.getConnectedThread() != null) {
            // SEND PHOTO
            Log.i(TAG, "sending photo from " + photoFileName);
            byte[] contents = P2PSyncManager.getProfilePhotoContents(photoFileName, manager.getContext());
            String photoInformation = "START" + new P2PDBApiImpl(db, manager.getContext()).serializeProfileMessage(userId, deviceId, photoFileName, contents) + "END";
            manager.getConnectedThread().write(photoInformation.getBytes());
            Log.i(TAG, "photo message sent" + photoInformation);
            p2PStateFlow.setProfilePhotoSent(true);
            if (p2PStateFlow.isProfilePhotoReceived()) {
                p2PStateFlow.allMessagesExchanged();
            }
        }
    }

    @Override
    public void onExit(P2PStateFlow.Transition newTransition) {
        Log.i(TAG, "EXIT SEND_PROFILE_PHOTO STATE to transition" + newTransition);
    }

    @Override
    public P2PStateFlow.Transition process(P2PStateFlow.Transition transition) {
        P2PStateFlow.Transition newTransition = null;
        switch (transition) {
            case RECEIVE_PROFILE_PHOTO: {
                newTransition = RECEIVE_PROFILE_PHOTO;
                break;
            }
            default: {
                newTransition = SEND_PROFILE_PHOTO;
                break;
            }
        }
        return newTransition;

    }

    public String getOutcome() {
        return null;
    }
}
