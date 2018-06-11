package p2p.chimple.org.p2pconnector.sync;

import p2p.chimple.org.p2pconnector.db.DBSyncManager;

public interface P2PState {
    public void onEnter(P2PStateFlow p2PStateFlow, DBSyncManager manager, String message);

    public void onExit(P2PStateFlow.Transition transition);

    public P2PStateFlow.Transition process(P2PStateFlow.Transition transition);

    public P2PStateFlow.Transition getTransition();

    public String getOutcome();
}


