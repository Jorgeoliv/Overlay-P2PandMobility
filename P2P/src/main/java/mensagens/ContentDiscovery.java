package mensagens;

import network.*;

public class ContentDiscovery extends Header{

    public String fileName; //nome do ficheiro para descobrir

    public ContentDiscovery(){
        super();
    }

    public ContentDiscovery(String requestID, Nodo origin, Nodo antecessor, long ttl, String fileName) {
        super(requestID, origin, antecessor, ttl);
        this.fileName = fileName;
    }

    public ContentDiscovery(String requestID, Nodo origin, Nodo antecessor, String fileName) {
        super(requestID, origin, antecessor);
        this.fileName = fileName;
    }

    public ContentDiscovery(String requestID, Nodo origin, long ttl, String fileName) {
        super(requestID, origin, ttl);
        this.fileName = fileName;
    }

    public ContentDiscovery(String requestID, Nodo origin, String fileName) {
        super(requestID, origin);
        this.fileName = fileName;
    }
}
