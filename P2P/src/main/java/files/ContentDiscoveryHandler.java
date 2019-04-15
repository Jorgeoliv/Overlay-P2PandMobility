package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.*;
import network.IDGen;
import network.NetworkTables;
import network.Nodo;

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

public class ContentDiscoveryHandler implements Runnable{

    private int ucp_Discovery;
    private int ucp_ContentOwner;
    private FileTables ft;
    private NetworkTables nt;
    private Nodo myNodo;
    private FileHandler fi;
    private IDGen idGen;
    private TreeSet<String> discoveryRequest = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public ContentDiscoveryHandler(){}

    public ContentDiscoveryHandler(FileHandler fi, NetworkTables nt, Nodo myNodo, int ucp_Discovery, int ucp_ContentOwner, IDGen idGen){
        this.fi = fi;
        this.nt = nt;
        this.ft = this.nt.ft;
        this.myNodo = myNodo;
        this.ucp_Discovery = ucp_Discovery;
        this.ucp_ContentOwner = ucp_ContentOwner;
        this.idGen = idGen;
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
            public void run() {
                removeRequest(id);
            }
        };

        return ret;
    }

    private void sendFocusDiscovery(ContentDiscovery cd, Nodo n){

        try {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);

            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, cd);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.ucp_Discovery);
            new DatagramSocket().send(packet);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDiscovery(ContentDiscovery cd) {

        addRequest(cd.requestID);
        HashSet<Nodo> fileInNbr = this.ft.nbrWithFile(cd.fileName);

        if(fileInNbr == null){
            //Envio para todos os vizinhos
            ArrayList<Nodo> nodos = this.nt.getNbrsN1();
            for(Nodo n: nodos)
                this.sendFocusDiscovery(cd, n);
        }
        else{
            //Envio apenas para os vizinhos que têm o ficheiro
            cd.ttl = 1;
            for(Nodo n: fileInNbr){
                this.sendFocusDiscovery(cd, n);
            }
        }
        ses.schedule(delete(cd.requestID), 60, TimeUnit.SECONDS);

    }

    private void sendOwner(ContentOwner co, Nodo dest) {

        try {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, co);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(dest.ip), this.ucp_ContentOwner);
            socket.send(packet);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(ucp_Discovery);

            byte[] buffer;
            DatagramPacket packet;


            Kryo kryo = new Kryo();

            while (true) {
                buffer = new byte[1500];
                packet = new DatagramPacket(buffer, buffer.length);

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

                        System.out.println("Recebi um content discovery que pede o ficheiro: " + cd.fileName);

                        String filename = cd.fileName;
                        //Primeiro verificar se eu tenho o ficheiro <- Depois podemos sacar o ficheiro logo e verificar se é null
                        if(this.ft.itsMyFile(filename)){
                            System.out.println("\tSou eu o Nodo " + this.myNodo + " que tem o ficheiro: " + filename);
                            FileInfo fileToSend = this.ft.getMyFile(filename);
                            ContentOwner co = new ContentOwner(this.idGen.getID(), this.myNodo, fileToSend);
                            this.sendOwner(co, cd.requester);
                        }
                        else{
                            //Depois vou verificar se algum dos meus vizinhos tem o ficheiro
                            HashSet<Nodo> fileInNbr = this.ft.nbrWithFile(filename);
                            //VOu me colocar a mim como antecessor
                            cd.antecessor = myNodo;
                            cd.route.add(myNodo.id);

                            if(fileInNbr != null){
                                //Só preciso de enviar o DiscoveryContent para os meus vizinhos do hashset ...
                                System.out.println("\tO ficheiro está num dos meus vizinhos ...");
                                cd.ttl = 1;
                                for(Nodo n: fileInNbr){
                                    this.sendFocusDiscovery(cd, n);
                                }
                            }
                            else{
                                //Preciso de enviar o discoveryContent para todos os vizinhos desde que o ttl ainda seja maior do que zero...
                                cd.ttl--;
                                System.out.println("Não sei onde é que está o ficheiro ...");
                                if(cd.ttl != 0){
                                    ArrayList<Nodo> nodos = nt.getNbrsN1();
                                    for(Nodo n: nodos)
                                        this.sendFocusDiscovery(cd, n);
                                }
                            }
                        }

                        ses.schedule(delete(requestID), 60, TimeUnit.SECONDS);
                    }

                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
