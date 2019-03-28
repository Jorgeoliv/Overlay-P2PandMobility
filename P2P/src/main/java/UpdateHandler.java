import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import files.FileTables;
import mensagens.*;
import files.*;
import sun.reflect.generics.tree.Tree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class UpdateHandler implements Runnable{

    final int port = 6003;
    private FileTables ft;
    private TreeSet<String> updateRequests = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public UpdateHandler(){}

    public UpdateHandler(FileTables ft){
        this.ft = ft;
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

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


            Kryo kryo = new Kryo();

            kryo.register(FileInfo.class);
            kryo.register(ArrayList.class);

            while(true){

                socket.receive(packet);
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof UpdateTable){
                    UpdateTable ut = (UpdateTable) header;
                    System.out.println("RECEBI: " + ut.toString());
                    String nodeID = ut.origin.id;
                    String requestID = ut.requestID;
                    System.out.println("OLHA O MEU IP: " + ut.origin.ip);
                    //Só vai processar senão estiver na lista
                    if(!this.containsRequest(requestID)) {
                        this.addRequest(requestID);
                        if (ft.getHash(nodeID).equals(ut.oldHash)) {
                            if (ut.toAdd != null)
                                ft.addContentForOneNbr(ut.toAdd, ut.origin);
                            if (ut.toRemove != null)
                                ft.rmContentForOneNbr(ut.toRemove, ut.origin);
                        }
                        ft.updateHash(nodeID, ut.newHash);
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
