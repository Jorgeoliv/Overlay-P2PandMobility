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

        //Neste ponto já está na rede (mesmo que esteja sozinho) e tem de se juntar ao grupo multicast


        /**
         * Iniciação de threads:
         *
         * -> Criação das estruturas partilhadas
         * -> Uma thread para tratar de enviar periodicamente os PINGs
         * -> Uma thread que vai estar à escuta de PINGs (numa porta especifica) e que vai responder com PONGs
         * -> Uma thread para escutar os "alive" (numa porta especifica) (testar com a mesma thread colocar um timeout para verificar todos os alives e também para enviar um alive para a lista de vizinhos nivel 1)
         * -> Uma thread para ouvir os restantes pedidos (consoante o pedido pode <<convocar>> novas threads)
         *
         */

        //Vai estar a correr num "while(true)" até ser interrompida quer por saida brusca ou então por indicação do utilizador

        //Código:

        //Criação das estruturas partilhadas do proprio nodo

        FileTables ft = new FileTables();
        NetworkTables nt = new NetworkTables(ft);
        RequestTables rt = new RequestTables();

        (new Thread(new AliveSupport(nt, me))).start();

        //Entrada na rede

        //CODIGO!!!

        try {
            //Vou criar um socket que fica a escuta na porta 6000 e com o endereço do meu HOST
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


            Nodo n1 = new Nodo("b", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            Nodo n2 = new Nodo("c", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            Nodo n3 = new Nodo("d", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            ArrayList<Nodo> an = new ArrayList<>();
            for(int i = 0; i<1; i++)
                an.add(n1);

            ArrayList<Nodo> ann = new ArrayList<>();
            for(int i=0; i<1; i++)
                ann.add(n2);


            nt.addNbrN1(an);
            //nt.addNbrN1(new ArrayList<Nodo>(){{add(n3);}});
            //System.out.println(ann);
            nt.addNbrN2(n1.id, ann);
            //nt.addNbrN2(n3.id, new ArrayList<Nodo>(){{add(n1); add(n2);}});

            Alive a = new Alive("a", me, an, ann);

            ft.addNbrContent("a", an);
            ft.addNbrContent("b", ann);
            ft.addNbrContent("c", an);
            ft.addNbrContent("c", ann);

            //KYRO

            /*ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, a);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();
            System.out.println("Tamanho do byte: " +  serializedMessage.length);

            DatagramPacket packetSend
                    = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), 6002);
            socket.send(packetSend);*/

            //Json Serialization

            /*JsonSerializer mapper =  new JsonSerializerFactory().create();
            System.out.println(a == null);
            System.out.println(a.toString());
            System.out.println(mapper == null);
            System.out.println(an == null);
            System.out.println(ann == null);
            String json = mapper.serialize(a).toString();

            System.out.println("A string é: " + json.length());

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(json);
            oo.close();

            byte[] serializedMessage = bStream.toByteArray();
            System.out.println("Tamanho do byte: " +  serializedMessage.length);

            DatagramPacket packetSend
                    = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), 6002);
            socket.send(packetSend);*/

            /*for(int i = 0; i<1; i++) {

                //Java serialization

                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                ObjectOutput oo = new ObjectOutputStream(bStream);
                oo.writeObject(a);
                oo.close();

                byte[] serializedMessage = bStream.toByteArray();
                System.out.println("Tamanho do byte: " + serializedMessage.length);

                DatagramPacket packetSend
                        = new DatagramPacket(serializedMessage, serializedMessage.length, me.ip, 6002);
                socket.send(packetSend);
            }*/

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
