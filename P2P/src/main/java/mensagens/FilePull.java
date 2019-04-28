package mensagens;

import files.FileInfo;
import network.Nodo;

import java.util.ArrayList;
import java.util.HashMap;

public class FilePull extends Header {

    public FileInfo fi;
    public HashMap <Integer, Integer> ports_packetPerSecond;
    public ArrayList<Integer> missingFileChunks;

    public FilePull(){super();}

    public FilePull(String requestID, Nodo origin, FileInfo fi, HashMap <Integer, Integer> ppps, ArrayList<Integer> missingFileChunks) {
        super(requestID, origin);
        this.fi = fi;
        this.ports_packetPerSecond = ppps;
        this.missingFileChunks = missingFileChunks;
    }
}
