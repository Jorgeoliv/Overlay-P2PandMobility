package mensagens;

import files.FileChunk;
import network.Nodo;

public class FilePush extends Header {

    public FileChunk fc;
    public String hash;

    public FilePush() {
        super();
    }

    public FilePush(String requestID, Nodo origin, FileChunk fc, String hash) {
        super(requestID, origin);
        this.fc = fc;
        this.hash = hash;
    }
}
