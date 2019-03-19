package mensagens;

import network.*;

public class Quit extends Header{

    public Quit(String requestID, Nodo origin) {
        super(requestID, origin);
    }
}
