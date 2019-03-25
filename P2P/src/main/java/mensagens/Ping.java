package mensagens;

import java.util.ArrayList;
import network.*;

public class Ping extends Header {

    public ArrayList<Nodo> nbrN1 = new ArrayList<>(); //lista de vizinhos nivel 1
    public ArrayList<Nodo> nbrN2 = new ArrayList<>(); //lista de vizinhos de nivel 2

    /**
     * IMPORTANTE:
     * -> Se a lista for vazia indica que ele depois tem de escolher outros vizinhos que pertençam aos seus viznhos
     */
    public Ping(){super();}

    public Ping(String requestID, Nodo origin, long ttl, ArrayList<Nodo> nbrN1, ArrayList<Nodo> nbrN2) {
        super(requestID, origin, ttl);
        this.nbrN1 = nbrN1;
        this.nbrN2 = nbrN2;
    }
}
