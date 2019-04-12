package mensagens;

import files.FileChunk;
import network.Nodo;

public class FilePush extends Header {

    public FileChunk fc;
    public String name;

    public FilePush() {
        super();
    }

    public FilePush(String requestID, Nodo origin, FileChunk fc, String name) {
        super(requestID, origin);
        this.fc = fc;
        this.name = name;
    }
}
