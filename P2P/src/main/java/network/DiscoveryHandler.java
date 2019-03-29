package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import files.*;
import mensagens.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscoveryHandler {

    private final int port = 6004;
    private FileTables ft;
    private NetworkTables nt;
    private Nodo me;
    private TreeSet<String> discoveryRequest = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public DiscoveryHandler(){}

    public DiscoveryHandler(FileTables ft, NetworkTables nt, Nodo me){
        this.ft = ft;
        this.nt = nt;
        this.me = me;
    }

    private synchronized boolean containsRequest(String id){
        return discoveryRequest.contains(id);
    }

    private synchronized void removeRequest(String id){
        discoveryRequest.remove(id);
    }

    private synchronized void addRequest(String id){
        discoveryRequest.add(id);
    }

    private Runnable delete(final String id){
        Runnable ret = new Runnable() {
            @Override
            public void run() {
                removeRequest(id);
            }
        };

        return ret;
    }

    private void sendDiscovery(ContentDiscovery cd, Nodo n){

        try {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, cd);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.port);
            socket.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


            Kryo kryo = new Kryo();

            while (true) {

                socket.receive(packet);
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof ContentDiscovery){
                    ContentDiscovery cd = (ContentDiscovery) header;

                    String requestID = cd.requestID;
                    if(!containsRequest(requestID)){
                        addRequest(requestID);

                        String filename = cd.fileName;
                        if(this.ft.itsMyFile(filename)){
                            //VAMOS TER DE VER O QUE FAZEMOS A SEGUIR!
                            //AQUI
                            //AQUI
                            //AQUI
                        }else{
                            HashSet<Nodo> fileInNbr = this.ft.nbrWithFile(filename);
                            //VOu me colocar a mim como antecessor
                            cd.antecessor = me;
                            if(fileInNbr != null){
                                //Só preciso de enviar o DiscoveryContent para os meus vizinhos do hashset ...
                                cd.ttl = 1;
                                for(Nodo n: fileInNbr){
                                    this.sendDiscovery(cd, n);
                                }
                            }else{
                                //Preciso de enviar o discoveryContent para todos os vizinhos desde que o ttl ainda seja maior do que zero...
                                cd.ttl--;
                                if(cd.ttl != 0){
                                    ArrayList<Nodo> nodos = nt.getNbrsN1();
                                    for(Nodo n: nodos)
                                        this.sendDiscovery(cd, n);
                                }
                            }
                        }

                        ses.schedule(delete(requestID), 60, TimeUnit.SECONDS);
                    }

                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
