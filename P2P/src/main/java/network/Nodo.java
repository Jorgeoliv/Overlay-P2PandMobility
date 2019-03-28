package network;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class Nodo /*implements Serializable*/ {

    public String id; //representa o id do Nodo => Posteriormente o nome (para já é o ID?)
    public String ip;

    //MAIS ALGUMA COISA???

    public Nodo(){

    }

    public Nodo(String id, String ip) {
        this.id = id;
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodo nodo = (Nodo) o;
        return this.id.equals(nodo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Nodo{" +
                "id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
