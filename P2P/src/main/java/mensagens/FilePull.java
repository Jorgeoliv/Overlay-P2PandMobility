package mensagens;

import files.FileInfo;
import network.Nodo;
import java.util.HashMap;

public class FilePull extends Header {

    public FileInfo fi;
    public HashMap <Integer, Integer> ports_packetPerSecond;

    public FilePull(){super();}

    public FilePull(String requestID, Nodo origin, FileInfo fi, HashMap <Integer, Integer> ppps) {
        super(requestID, origin);
        this.fi = fi;
        this.ports_packetPerSecond = ppps;
    }
}
