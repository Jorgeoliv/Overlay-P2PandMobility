package mensagens;

import network.*;

public class AddNBR extends Header{

    //tem de possuir uma lista sobre a meta-informação dos ficheiros que possui


    public AddNBR(String requestID, Nodo origin) {
        super(requestID, origin);
    }
}