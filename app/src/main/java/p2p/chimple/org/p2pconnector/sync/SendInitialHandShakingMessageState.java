package p2p.chimple.org.p2pconnector.sync;

import android.util.Log;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.NONE;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_DB_SYNC_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_HANDSHAKING_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_HANDSHAKING_INFORMATION;

public class SendInitialHandShakingMessageState implements P2PState {

    private static final String TAG = SendInitialHandShakingMessageState.class.getSimpleName();

    private P2PStateFlow.Transition cTransition;

    public SendInitialHandShakingMessageState() {
        this.cTransition = SEND_HANDSHAKING_INFORMATION;
    }

    public P2PStateFlow.Transition getTransition() {
        return this.cTransition;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, P2PSyncManager manager, String message) {
        //send handshaking message
        if (manager.getConnectedThread() != null) {
            AppDatabase db = AppDatabase.getInstance(manager.getContext());
            String initialMessage = new P2PDBApiImpl(db, manager.getContext()).serializeHandShakingMessage();
            manager.getConnectedThread().write(initialMessage.getBytes());
            Log.i(TAG, "initial handshaking message sent" + initialMessage);
            if(!p2PStateFlow.isHandShakingInformationSent()) {
                p2PStateFlow.setHandShakingInformationSent(true);
            }

        }
    }

    @Override
    public void onExit(P2PStateFlow.Transition newTransition) {
        Log.i(TAG, "EXIT SEND_HANDSHAKING_INFORMATION STATE to transition" + newTransition);
    }

    @Override
    public P2PStateFlow.Transition process(P2PStateFlow.Transition transition) {
        P2PStateFlow.Transition newTransition = null;
        switch (transition) {
            case RECEIVE_HANDSHAKING_INFORMATION: {
                newTransition = RECEIVE_HANDSHAKING_INFORMATION;
                break;
            }
            case RECEIVE_DB_SYNC_INFORMATION: {
                newTransition = RECEIVE_DB_SYNC_INFORMATION;
                break;
            }
            default: {
                newTransition = SEND_HANDSHAKING_INFORMATION;
                break;
            }
        }
        return newTransition;

    }

    public String getOutcome() {
        return null;
    }
}
