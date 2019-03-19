package mensagens;

import java.util.ArrayList;
import network.*;

public class Alive extends Header{

    ArrayList<Nodo> nbrN1 = new ArrayList<Nodo>(); //lista de vizinhos nivel 1
    ArrayList<Nodo> nbrN2 = new ArrayList<Nodo>(); //lista de vizinhos de nivel 2

    public Alive(String requestID, Nodo origin, ArrayList<Nodo> nbrN1, ArrayList<Nodo> nbrN2) {
        super(requestID, origin);
        this.nbrN1 = nbrN1;
        this.nbrN2 = nbrN2;
    }
}
