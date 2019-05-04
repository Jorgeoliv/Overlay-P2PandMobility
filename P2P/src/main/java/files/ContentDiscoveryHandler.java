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
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentDiscoveryHandler implements Runnable{

    private boolean run = true;

    private int ucp_Discovery;
    private int ucp_ContentOwner;
    private FileTables ft;
    private NetworkTables nt;
    private Nodo myNodo;
    private FileHandler fi;
    private IDGen idGen;
    private TreeSet<String> discoveryRequest = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    private ArrayList<String> ids = new ArrayList<String>();
    private DatagramSocket socket;

    public ContentDiscoveryHandler(FileHandler fi, NetworkTables nt, Nodo myNodo, int ucp_Discovery, int ucp_ContentOwner, IDGen idGen){
        this.fi = fi;
        this.nt = nt;
        this.ft = this.nt.ft;
        this.myNodo = myNodo;
        this.ucp_Discovery = ucp_Discovery;
        this.ucp_ContentOwner = ucp_ContentOwner;
        this.idGen = idGen;
        try {
            this.socket = new DatagramSocket(ucp_Discovery);
        } catch (SocketException e) {
            e.printStackTrace();
        }

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

            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.ucp_Discovery);

            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);

            //System.out.println("ENVIEI FOCUS DISCOVERY");
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
        this.ses.schedule(delete(cd.requestID), 60, TimeUnit.SECONDS);

    }

    private void sendOwner(ContentOwner co, Nodo dest) {
        try {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, co);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(dest.ip), this.ucp_ContentOwner);

            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);

            //System.out.println("ENVIEI OWNER");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.socket.close();
    }

    public void run() {
        try {

            byte[] buffer;
            DatagramPacket packet;


            Kryo kryo = new Kryo();

            while(this.run) {
                buffer = new byte[1500];
                packet = new DatagramPacket(buffer, buffer.length);

                this.socket.receive(packet);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();
                if(!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof ContentDiscovery) {
                        ContentDiscovery cd = (ContentDiscovery) header;

                        String requestID = cd.requestID;
                        if (!containsRequest(requestID)) {
                            addRequest(requestID);

                            //System.out.println("Recebi um content discovery que pede o ficheiro: " + cd.fileName);

                            String filename = cd.fileName;
                            //Primeiro verificar se eu tenho o ficheiro <- Depois podemos sacar o ficheiro logo e verificar se é null
                            if (this.ft.itsMyFile(filename)) {
                                //System.out.println("\tSou eu o Nodo " + this.myNodo + " que tem o ficheiro: " + filename);
                                FileInfo fileToSend = this.ft.getMyFile(filename);
                                ContentOwner co = new ContentOwner(this.idGen.getID(""), this.myNodo, fileToSend, cd.requestID);
                                this.sendOwner(co, cd.requester);
                            } else {
                                //Depois vou verificar se algum dos meus vizinhos tem o ficheiro
                                HashSet<Nodo> fileInNbr = this.ft.nbrWithFile(filename);
                                //VOu me colocar a mim como antecessor
                                cd.antecessor = myNodo;
                                cd.route.add(myNodo.id);

                                if (fileInNbr != null) {
                                    //Só preciso de enviar o DiscoveryContent para os meus vizinhos do hashset ...
                                    //System.out.println("\tO ficheiro está num dos meus vizinhos ...");
                                    cd.ttl = 1;
                                    for (Nodo n : fileInNbr) {
                                        this.sendFocusDiscovery(cd, n);
                                    }
                                } else {
                                    //Preciso de enviar o discoveryContent para todos os vizinhos desde que o ttl ainda seja maior do que zero...
                                    cd.ttl--;
                                    //System.out.println("Não sei onde é que está o ficheiro ...");
                                    if (cd.ttl != 0) {
                                        ArrayList<Nodo> nodos = nt.getNbrsN1();
                                        for (Nodo n : nodos)
                                            this.sendFocusDiscovery(cd, n);
                                    }
                                }
                            }

                            this.ses.schedule(delete(requestID), 60, TimeUnit.SECONDS);
                        }
                    }
                }
            }
        }
        catch (SocketException se){
            //System.out.println("\t=>CONTENTDISCOVERYHANDLER DATAGRAMSOCKET CLOSED");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
