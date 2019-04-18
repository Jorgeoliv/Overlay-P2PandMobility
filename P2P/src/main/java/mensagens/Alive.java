package mensagens;

import java.io.Serializable;
import java.util.ArrayList;
import network.*;

public class Alive extends Header /*implements Serializable*/ {

    public ArrayList<Nodo> nbrN1 = new ArrayList<Nodo>(); //lista de vizinhos nivel 1
    public ArrayList<Nodo> nbrN2 = new ArrayList<Nodo>(); //lista de vizinhos de nivel 2

    public Alive(){
        super();
    }

    public Alive(String requestID, Nodo origin, ArrayList<Nodo> nbrN1, ArrayList<Nodo> nbrN2) {
        super(requestID, origin);
        this.nbrN1 = nbrN1;
        this.nbrN2 = nbrN2;
    }

/*    public String toString() {
        return "Alive{" +
                "nbrN1=" + nbrN1 +
                ", nbrN2=" + nbrN2 +
                ", requestID='" + requestID + '\'' +
                ", origin=" + origin +
                ", antecessor=" + antecessor +
                ", ttl=" + ttl +
                '}';
    }*/
}
