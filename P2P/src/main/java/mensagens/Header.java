package mensagens;

import network.*;

public class Header {

    String requestID; //id referente ao pedido
    /**
     * VAMOS POR O TIPO DE MENSAGEM?
     */
    Nodo origin; //Info do nodo origem
    Nodo antecessor; //Info do nodo que fez "root"
    long ttl = 1; //time to live de uma pacote, por defeito tem o valor 1

    public Header(String requestID, Nodo origin, Nodo antecessor, long ttl) {
        this.requestID = requestID;
        this.origin = origin;
        this.antecessor = antecessor;
        this.ttl = ttl;
    }

    public Header(String requestID, Nodo origin, Nodo antecessor) {
        this.requestID = requestID;
        this.origin = origin;
        this.antecessor = antecessor;
    }

    public Header(String requestID, Nodo origin, long ttl) {
        this.requestID = requestID;
        this.origin = origin;
        this.ttl = ttl;
        this.antecessor = origin;
    }

    public Header(String requestID, Nodo origin) {
        this.requestID = requestID;
        this.origin = origin;
        this.antecessor = origin; //Quando não indica o antecessor assumimos que é o nodo origem
    }
}
