package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import files.FileTables;
import mensagens.*;
import files.*;
import sun.reflect.generics.tree.Tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class UpdateHandler implements Runnable{

    private int ucp_Update;
    private Nodo myNode;
    private FileTables ft;
    private IDGen idGen;
    private TreeSet<String> updateRequests = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private NetworkHandler nh;

    public UpdateHandler(){}

    public UpdateHandler(int ucp_Update, Nodo myNode, FileTables ft, IDGen idGen, NetworkHandler nh) {
        this.ucp_Update = ucp_Update;
        this.myNode = myNode;
        this.ft = ft;
        this.idGen = idGen;
        this.nh = nh;
    }

    private synchronized boolean containsRequest(String id){
        System.out.println("VOU VERIFICAR SE JA CONTEM O REQUEST");
        return updateRequests.contains(id);
    }

    private synchronized void removeRequest(String id){
        System.out.println("ACABEI AGORA DE REMOVER UM REQUEST");
        updateRequests.remove(id);
        System.out.println(updateRequests.toString());
    }

    private synchronized void addRequest(String id){
        System.out.println("ACABEI AGORA DE ADICIONAR UM REQUEST");
        updateRequests.add(id);
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

    public boolean sendUpdate(UpdateTable ut){

        ArrayList<Nodo> myNbrs = nh.nt.getNbrsN1();

        Kryo kryo = new Kryo();

        System.out.println("VOU ENVIAR O SEGUINTE: ");
        System.out.println(ut.toString());

        for(Nodo n: myNbrs){
            try {
                DatagramSocket socket = new DatagramSocket();
                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);
                kryo.writeClassAndObject(output, ut);
                output.close();

                byte[] serializedMessage = bStream.toByteArray();

                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.ucp_Update);
                socket.send(packet);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(ucp_Update, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


            Kryo kryo = new Kryo();

            while(true){

                socket.receive(packet);
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof UpdateTable){
                    UpdateTable ut = (UpdateTable) header;
                    String nodeID = ut.origin.id;
                    String requestID = ut.requestID;
                    //Só vai processar senão estiver na lista
                    if(!this.containsRequest(requestID)) {
                        this.addRequest(requestID);
                        if (ft.getHash(nodeID).equals(ut.oldHash)) {
                            if (ut.toAdd != null)
                                ft.addContentForOneNbr(ut.toAdd, ut.origin, ut.newHash);
                            if (ut.toRemove != null)
                                ft.rmContentForOneNbr(ut.toRemove, ut.origin, ut.newHash);
                        }else{
                            System.out.println("ATENÇÃO ATENÇÃO ATENÇÃO");
                            System.out.println("\t Falta um pacote do UpdateHandler para o nodo: " + myNode.ip);
                        }
                        ses.schedule(delete(requestID), 10, TimeUnit.SECONDS);
                    }
                }

            }

        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
