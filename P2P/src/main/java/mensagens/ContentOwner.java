package mensagens;

import network.*;
import files.*;

public class ContentOwner extends Header{

    FileInfo fileInfo; //informação sobre o ficheiro

    /**
     * IMPORTANTE:
     * -> Aqui vamos assumir que quem envia o "contentOwner" então indica que tem o ficheiro
     */

    public ContentOwner(String requestID, Nodo origin, FileInfo fileInfo) {
        super(requestID, origin);
        this.fileInfo = fileInfo;
    }
}
