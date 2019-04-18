package mensagens;

import java.util.ArrayList;
import network.*;
import files.*;

public class UpdateTable extends Header{

    public ArrayList<FileInfo> toAdd = new ArrayList<>(); //para informar dos ficheiros que vão ter de ser adicionados
    public ArrayList<FileInfo> toRemove = new ArrayList<>(); //para informar os ficheiros que vão ter de ser removidos
    public String oldHash;
    public String newHash;

    public UpdateTable(){ super(); }

    /*public UpdateTable(String requestID, Nodo origin) {
        super(requestID, origin);
    }*/


    public UpdateTable(String requestID, Nodo origin, ArrayList<FileInfo> toAdd, ArrayList<FileInfo> toRemove, String oldHash, String newHash) {
        super(requestID, origin);
        this.toAdd = toAdd;
        this.toRemove = toRemove;
        this.oldHash = oldHash;
        this.newHash = newHash;
    }

/*    @Override
    public String toString() {
        return "UpdateTable{" +
                "toAdd=" + toAdd +
                ", toRemove=" + toRemove +
                ", oldHash='" + oldHash + '\'' +
                ", newHash='" + newHash + '\'' +
                ", requestID='" + requestID + '\'' +
                ", origin=" + origin +
                '}';
    }*/

    public boolean equals(Object o){
        return super.equals(o);
    }
}
