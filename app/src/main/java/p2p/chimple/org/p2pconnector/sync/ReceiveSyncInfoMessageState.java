package p2p.chimple.org.p2pconnector.sync;

import p2p.chimple.org.p2pconnector.db.AppDatabase;
import p2p.chimple.org.p2pconnector.db.DBSyncManager;
import p2p.chimple.org.p2pconnector.db.P2PDBApiImpl;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_DB_SYNC_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_DB_SYNC_INFORMATION;

public class ReceiveSyncInfoMessageState implements P2PState {

    private P2PStateFlow.Transition cTransition;

    private String outcome = null;


    public ReceiveSyncInfoMessageState() {
        this.cTransition = RECEIVE_DB_SYNC_INFORMATION;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, DBSyncManager manager, String readMessage) {
        if (p2PStateFlow.getThread() != null) {
            this.outcome = readMessage;
            AppDatabase db = AppDatabase.getInstance(manager.getContext());
            P2PDBApiImpl.getInstance(manager.getContext()).persistP2PSyncInfos(readMessage);
            p2PStateFlow.setAllSyncInformationReceived(true);
            if (!p2PStateFlow.isAllSyncInformationSent()) {
                p2PStateFlow.transit(SEND_DB_SYNC_INFORMATION, this.outcome);
            } else  {
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
            case SEND_DB_SYNC_INFORMATION: {
                newTransition = SEND_DB_SYNC_INFORMATION;
                break;
            }
            default: {
                newTransition = RECEIVE_DB_SYNC_INFORMATION;
                break;
            }
        }
        return newTransition;
    }
}
