package mensagens;

import network.*;

public class Ack extends Header {

    public String responseID;

    public Ack(){}

    public Ack(String requestID, Nodo origin, String responseID) {
        super(requestID, origin);
        this.responseID = responseID;
    }
}
