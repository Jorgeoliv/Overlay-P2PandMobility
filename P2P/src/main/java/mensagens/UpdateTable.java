package mensagens;

import java.util.ArrayList;
import network.*;
import files.*;

public class UpdateTable extends Header{

    ArrayList<FileInfo> toAdd; //para informar dos ficheiros que vão ter de ser adicionados
    ArrayList<FileInfo> toRemove; //para informar os ficheiros que vão ter de ser removidos

    public UpdateTable(String requestID, Nodo origin, ArrayList<FileInfo> toAdd, ArrayList<FileInfo> toRemove) {
        super(requestID, origin);
        this.toAdd = toAdd;
        this.toRemove = toRemove;
    }
}
