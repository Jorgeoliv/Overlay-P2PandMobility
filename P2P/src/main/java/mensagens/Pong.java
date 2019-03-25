package mensagens;

import java.util.ArrayList;
import network.*;

public class Pong extends Header {

    String pingID; //para saber qual é o ping a que este pong responde
    ArrayList<Nodo> nbrN1 = new ArrayList<>(); //lista de vizinhos nivel 1
    ArrayList<Nodo> nbrN2 = new ArrayList<>(); //lista de vizinhos de nivel 2

    public Pong(){super();}

    public Pong(String requestID, Nodo origin, long ttl, String pingID, ArrayList<Nodo> nbrN1, ArrayList<Nodo> nbrN2) {
        super(requestID, origin, ttl);
        this.pingID = pingID;
        this.nbrN1 = nbrN1;
        this.nbrN2 = nbrN2;
    }
}
