package mensagens;

import files.FileInfo;
import network.Nodo;

public class FilePull extends Header {

    public FileInfo fi;

    public FilePull(){super();}

    public FilePull(String requestID, Nodo origin, FileInfo fi) {
        super(requestID, origin);
        this.fi = fi;
    }
}
