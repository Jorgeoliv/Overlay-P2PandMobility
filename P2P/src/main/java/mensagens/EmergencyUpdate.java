package mensagens;

import network.Nodo;

public class EmergencyUpdate extends Header {

    public EmergencyUpdate(){}

    public EmergencyUpdate(String requestID, Nodo origin) {
        super(requestID, origin);
    }
}
