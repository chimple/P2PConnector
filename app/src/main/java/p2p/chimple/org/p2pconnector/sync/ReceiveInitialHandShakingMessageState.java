package p2p.chimple.org.p2pconnector.sync;

import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.RECEIVE_HANDSHAKING_INFORMATION;
import static p2p.chimple.org.p2pconnector.sync.P2PStateFlow.Transition.SEND_HANDSHAKING_INFORMATION;

public class ReceiveInitialHandShakingMessageState implements P2PState {

    private P2PStateFlow.Transition cTransition;

    private String outcome = null;


    public ReceiveInitialHandShakingMessageState() {
        this.cTransition = RECEIVE_HANDSHAKING_INFORMATION;
    }

    @Override
    public void onEnter(P2PStateFlow p2PStateFlow, P2PSyncManager manager, String readMessage) {
        if (manager.getConnectedThread() != null) {
            this.outcome = readMessage;
            manager.updateStatus("handShakingInformationReceived", this.outcome);
            p2PStateFlow.setHandShakingInformationReceived(true);
            if (!p2PStateFlow.isHandShakingInformationSent()) {
                p2PStateFlow.transit(SEND_HANDSHAKING_INFORMATION, this.outcome);
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
            case SEND_HANDSHAKING_INFORMATION: {
                newTransition = SEND_HANDSHAKING_INFORMATION;
                break;
            }
            default: {
                newTransition = RECEIVE_HANDSHAKING_INFORMATION;
                break;
            }
        }
        return newTransition;
    }
}
