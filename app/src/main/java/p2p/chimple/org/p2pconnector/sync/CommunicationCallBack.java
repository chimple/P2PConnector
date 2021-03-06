package p2p.chimple.org.p2pconnector.sync;

import java.net.Socket;

public interface CommunicationCallBack {
    public void Connected(Socket socket);

    public void GotConnection(Socket socket);

    public void ConnectionFailed(String reason);

    public void ListeningFailed(String reason);

}
