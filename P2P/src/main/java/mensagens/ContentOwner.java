package mensagens;

import network.*;
import files.*;

public class ContentOwner extends Header{

    public FileInfo fileInfo; //informação sobre o ficheiro
    public String cdRequestID;

    /**
     * IMPORTANTE:
     * -> Aqui vamos assumir que quem envia o "contentOwner" então indica que tem o ficheiro
     */

    public ContentOwner(){}

    public ContentOwner(String requestID, Nodo origin, FileInfo fileInfo, String cdResquestID) {
        super(requestID, origin);
        this.fileInfo = fileInfo;
        this.cdRequestID = cdResquestID;
    }
}
