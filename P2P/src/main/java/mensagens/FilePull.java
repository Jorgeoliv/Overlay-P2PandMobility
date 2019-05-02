package mensagens;

import files.FileInfo;
import network.Nodo;

public class FilePull extends Header {

    public FileInfo fi;
    public FCIDStruct missingFileChunks;
    public int[] portas;
    public int pps;

    public FilePull(){super();}

    public FilePull(String requestID, Nodo origin, FileInfo fi, int[] portas, int pps, FCIDStruct missingFileChunks) {
        super(requestID, origin);
        this.fi = fi;
        this.portas = portas;
        this.pps = pps;
        this.missingFileChunks = missingFileChunks;
    }
}
