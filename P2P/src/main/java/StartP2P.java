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

    public static void main(String[] args) throws UnknownHostException {

        Nodo me = new Nodo("a", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

        System.out.println("O meu ip é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
        System.out.println("Na string é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).toString());
        System.out.println("No me é: " + me.ip);

        //Vai ter de começar a iniciar o multicast com o Ping
        FileTables ft = new FileTables();
        NetworkTables nt = new NetworkTables(ft);

        System.out.println("CRIAR O PINGHANDLER");
        PingHandler pingHandler = new PingHandler(me,InetAddress.getByName("224.0.2.14"), 6789, 9876, 1, nt, 5);
        PongHandler pongHandler = new PongHandler(me, 9876, nt);
        Thread t = new Thread(pingHandler);
        t.start();
        System.out.println("PINGHANDLER CRIADO");

        t = new Thread(pongHandler);
        t.start();
        System.out.println("PONGHANDLER CRIADO");

    }

}
