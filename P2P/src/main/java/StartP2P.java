import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import files.*;
import jdk.nashorn.internal.parser.JSONParser;
import mensagens.*;
import network.*;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;

public class StartP2P {

    final static int port = 6000;
    final static int mcp = 6789;
    final static int ucp_Pong = 9876;
    final static int ucp_NbrConfirmation = 9877;

    final static int SOFTCAP = 3;
    final static int HARDCAP = 6;

    public static void main(String[] args) throws UnknownHostException {

        //Vai ter de começar a iniciar o multicast com o Ping
        FileTables ft = new FileTables();

        NetworkHandler nh = new NetworkHandler(ft);

        Thread t = new Thread(nh);
        t.start();
        System.out.println("NETWORKHANDLER CRIADO");

    }

}
