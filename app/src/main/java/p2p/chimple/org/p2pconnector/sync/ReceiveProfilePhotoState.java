package p2p.chimple.org.p2pconnector.sync;

import android.util.Log;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_PROFILE_PHOTO;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_PROFILE_PHOTO;

public class ReceiveProfilePhotoState implements P2PState {
    private static final String TAG = ReceiveProfilePhotoState.class.getSimpleName();

    private P2PStateFlow.Transition cTransition;

    private String outcome = null;


    public ReceiveProfilePhotoState() {
        this.cTransition = RECEIVE_PROFILE_PHOTO;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, P2PSyncManager manager, String readMessage) {
        if (p2PStateFlow.getThread() != null) {
            P2PDBApiImpl.getInstance(manager.getContext()).persistProfileMessage(readMessage);
            p2PStateFlow.setProfilePhotoReceived(true);
            if (!p2PStateFlow.isProfilePhotoSent()) {
                p2PStateFlow.transit(SEND_PROFILE_PHOTO, null);
            } else {
                p2PStateFlow.allMessagesExchanged();
            }
        }
    }

    public String getOutcome() {
        return this.outcome;
    }

    @Override
    public void onExit(P2PStateFlow.Transition transition) {

    }

    @Override
    public P2PStateFlow.Transition getTransition() {
        return this.cTransition;
    }

    @Override
    public P2PStateFlow.Transition process(P2PStateFlow.Transition transition) {
        P2PStateFlow.Transition newTransition = null;
        switch (transition) {
            case SEND_PROFILE_PHOTO: {
                newTransition = SEND_PROFILE_PHOTO;
                break;
            }
            default: {
                newTransition = RECEIVE_PROFILE_PHOTO;
                break;
            }
        }
        return newTransition;
    }
}
