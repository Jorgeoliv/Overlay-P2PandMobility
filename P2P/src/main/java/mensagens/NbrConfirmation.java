package mensagens;

import files.FileInfo;
import network.Nodo;

import java.util.ArrayList;

public class NbrConfirmation extends Header {

    //tem de possuir uma lista sobre a meta-informação dos ficheiros que possui
    public ArrayList<FileInfo> fileInfos;
    public boolean added;

    public String IDresponse; //para saber qual é o pong a que este nbrconfirmation responde

    public NbrConfirmation(){super();};
    public NbrConfirmation (String requestID, Nodo origin, ArrayList<FileInfo> fi, String idresponse, boolean added) {
        super(requestID, origin);
        this.added = added;
        this.IDresponse = idresponse;
        this.fileInfos = fi;
    }

}
