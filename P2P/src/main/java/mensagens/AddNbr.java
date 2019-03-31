package mensagens;

import network.Nodo;

public class AddNbr extends Header {

    public Nodo intermediary; //nodo que conhece os 2 nodos que estão a comunicar

    public AddNbr(){
        super();
    }
    public AddNbr(String requestID, Nodo origin, Nodo intermediary){
        super(requestID, origin);
        this.intermediary = intermediary;
    }
}
