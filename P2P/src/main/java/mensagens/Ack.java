package mensagens;

import network.*;

public class Ack extends Header {

    String responseID;

    public Ack(String requestID, Nodo origin, String responseID) {
        super(requestID, origin);
        this.responseID = responseID;
    }
}
