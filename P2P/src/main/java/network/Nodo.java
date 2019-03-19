package network;

import java.util.Objects;

public class Nodo {

    String id; //representa o id do Nodo => Posteriormente o nome (para já é o ID?)
    String ip;

    //MAIS ALGUMA COISA???


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
}
