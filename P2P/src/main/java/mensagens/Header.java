package mensagens;

import network.Nodo;

import java.util.Objects;

public class Header {

    public String requestID; //id referente ao pedido
    /**
     * VAMOS POR O TIPO DE MENSAGEM?
     */
    public Nodo origin; //Info do nodo origem
    public Nodo antecessor; //Info do nodo que fez "root"
    public long ttl = 1; //time to live de uma pacote, por defeito tem o valor 1

    public Header(){

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return requestID.equals(header.requestID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestID);
    }
}
