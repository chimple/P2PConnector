package p2p.chimple.org.p2pconnector.sync;

import android.util.Log;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_DB_SYNC_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_HANDSHAKING_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_PROFILE_PHOTO;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_DB_SYNC_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_HANDSHAKING_INFORMATION;

public class SendSyncInfoMessageState implements P2PState {
    private static final String TAG = SendInitialHandShakingMessageState.class.getSimpleName();

    private P2PStateFlow.Transition cTransition;

    public SendSyncInfoMessageState() {
        this.cTransition = SEND_DB_SYNC_INFORMATION;
    }

    public P2PStateFlow.Transition getTransition() {
        return this.cTransition;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, P2PSyncManager manager, String message) {
        //send handshaking message
        if (manager.getConnectedThread() != null) {
            AppDatabase db = AppDatabase.getInstance(manager.getContext());
            String handShakingInformationReceived = p2PStateFlow.getStateResult(P2PStateFlow.Transition.RECEIVE_HANDSHAKING_INFORMATION);
            Log.i(TAG, "handShakingInformationReceived in SendSyncInfoMessageState" + handShakingInformationReceived);
            String syncInformation = new P2PDBApiImpl(db, manager.getContext()).buildAllSyncMessages(handShakingInformationReceived);
            final String updatedSyncInformation = "START" + syncInformation + "END";
            manager.getConnectedThread().write(updatedSyncInformation.getBytes(), 0, updatedSyncInformation.length());
            Log.i(TAG, "syncInformation message sent" + syncInformation);
            p2PStateFlow.setAllSyncInformationSent(true);
        }
    }

    @Override
    public void onExit(P2PStateFlow.Transition newTransition) {
        Log.i(TAG, "EXIT SEND_DB_SYNC_INFORMATION STATE to transition" + newTransition);
    }

    @Override
    public P2PStateFlow.Transition process(P2PStateFlow.Transition transition) {
        P2PStateFlow.Transition newTransition = null;
        switch (transition) {
            case RECEIVE_DB_SYNC_INFORMATION: {
                newTransition = RECEIVE_DB_SYNC_INFORMATION;
                break;
            }
            case RECEIVE_PROFILE_PHOTO: {
                newTransition = RECEIVE_PROFILE_PHOTO;
                break;
            }

            default: {
                newTransition = SEND_DB_SYNC_INFORMATION;
                break;
            }
        }
        return newTransition;

    }

    public String getOutcome() {
        return null;
    }
}
