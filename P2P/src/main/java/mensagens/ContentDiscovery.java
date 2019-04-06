package mensagens;

import network.*;

import java.util.ArrayList;

public class ContentDiscovery extends Header{

    public String fileName; //nome do ficheiro para descobrir
    public ArrayList<String> route = new ArrayList<>(); //rota => apenas temos a identificação do nodo

    public ContentDiscovery(){
        super();
    }

    public ContentDiscovery(String requestID, Nodo origin, Nodo antecessor, long ttl, String fileName, ArrayList<String> route) {
        super(requestID, origin, antecessor, ttl);
        this.fileName = fileName;
        this.route = route;
    }

    public ContentDiscovery(String requestID, Nodo origin, Nodo antecessor, String fileName, ArrayList<String> route) {
        super(requestID, origin, antecessor);
        this.fileName = fileName;
        this.route = route;
    }

    public ContentDiscovery(String requestID, Nodo origin, long ttl, String fileName, ArrayList<String> route) {
        super(requestID, origin, ttl);
        this.fileName = fileName;
        this.route = route;
    }

    public ContentDiscovery(String requestID, Nodo origin, String fileName, ArrayList<String> route) {
        super(requestID, origin);
        this.fileName = fileName;
        this.route = route;
    }
}
