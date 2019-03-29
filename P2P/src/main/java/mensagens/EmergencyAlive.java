package mensagens;

import network.Nodo;

import java.util.ArrayList;

public class EmergencyAlive extends Header {
    public ArrayList<Nodo> nbrN1;
    public boolean updated;

    public String IDresponse; //para saber qual é o pong a que este nbrconfirmation responde

    public EmergencyAlive(){super();}
    public EmergencyAlive(String requestID, Nodo origin, ArrayList<Nodo> nbrN1, String IDresponse, boolean updated){
        super(requestID, origin);
        this.nbrN1 = nbrN1;
        this.updated = updated;

        this.IDresponse = IDresponse;
    }
}
