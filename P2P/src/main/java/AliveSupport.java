import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.*;
import network.NetworkTables;
import network.Nodo;
import org.boon.json.JsonParser;
import org.boon.json.JsonParserFactory;

import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliveSupport implements Runnable {

    NetworkTables nt;
    private final int port = 6002; //Porta para escutar
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    long contador = 0;
    Nodo id;

    public AliveSupport(NetworkTables nt, Nodo id) {
        this.nt = nt;
        this.id = id;
    }

    Runnable sentAlive = () -> {

        ArrayList<Nodo> myNbrs = nt.getNbrsN1();
        ArrayList<Nodo> myNbrsN2 = nt.getNbrsN2();

        System.out.println("Vou enviar as coisas para " + myNbrs.size() + " vizinhos!!!");

        /**
         * ISTO DEPPIS VAI PARA O CREATE PACKET!!!
         */
        Alive a = new Alive(contador++ + this.id.id, id, myNbrs,  myNbrsN2);

        for(Nodo n: myNbrs){
            try {
                DatagramSocket socket = new DatagramSocket();
                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);
                Kryo kryo = new Kryo();
                kryo.writeClassAndObject(output, a);
                output.close();

                byte[] serializedMessage = bStream.toByteArray();

                DatagramPacket packet
                        = new DatagramPacket(serializedMessage, serializedMessage.length, n.ip, this.port);
                socket.send(packet);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //ses.schedule(sentAlive, 10, TimeUnit.SECONDS);
    };

    Runnable upAll = () -> {
        System.out.println("Vou incrementar tudo!!!");
        this.nt.inc();
    };

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            //ses.schedule(sentAlive, 1, TimeUnit.SECONDS);
            ses.scheduleAtFixedRate(sentAlive, 20, 20, TimeUnit.SECONDS);
            ses.scheduleAtFixedRate(upAll, 60, 60, TimeUnit.SECONDS);

            Kryo kryo = new Kryo();

            while(true){

                socket.receive(packet);
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof Alive){
                    Alive alive = (Alive) header;
                    System.out.println("Sou instancia do alive");
                    System.out.println("Tamanho recebido: " + buffer.length);
                    System.out.println("ALIVE:::: " + alive);

                    nt.reset(alive.origin.id);
                    nt.updateNbrN2(alive.origin.id, alive.nbrN1);
                }




                /*socket.receive(packet);
                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(buffer));
                String json = (String) iStream.readObject();
                iStream.close();

                JsonParser parser = new JsonParserFactory().create();
                Alive testMe = parser.parse(Alive.class, json);

                System.out.println("O nodo é: " + testMe);*/

                /*socket.receive(packet);

                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(buffer));
                Header alive = (Header) iStream.readObject();
                iStream.close();
                System.out.println("Tamanho recebido: " + buffer.length);

                //System.out.println(alive.toString());



                //Fazer um reset ao packet
                //buffer = null;
                packet.setLength(buffer.length);
                System.out.println("O tamanho do packet é: " + buffer.length);*/

            }

        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } /*catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }
}
