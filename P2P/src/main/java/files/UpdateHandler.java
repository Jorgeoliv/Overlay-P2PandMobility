package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.*;
import network.IDGen;
import network.NetworkHandler;
import network.NetworkTables;
import network.Nodo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class SupportUpdate{

    ArrayList<Nodo> nbrs = new ArrayList<>();
    UpdateTable ut = null;
    long time;

    public SupportUpdate(){}

    public SupportUpdate(ArrayList<Nodo> nbrs, UpdateTable ut, long time) {
        this.nbrs = nbrs;
        this.ut = ut;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportUpdate that = (SupportUpdate) o;
        return ut.equals(that.ut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ut);
    }
}

public class UpdateHandler implements Runnable{

    private int ucp_Update;
    private Nodo myNode;
    private FileTables ft;
    private IDGen idGen;
    private TreeSet<String> updateRequests = new TreeSet<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private NetworkTables nt;

    //Para tratar dos updates tables que possam não chegar
    private ArrayList<SupportUpdate> updateSent = new ArrayList<>();
    private ReentrantLock lockUpdate = new ReentrantLock();

    public UpdateHandler(){}

    public UpdateHandler(int ucp_Update, Nodo myNode, FileTables ft, IDGen idGen, NetworkTables nt) {
        this.ucp_Update = ucp_Update;
        this.myNode = myNode;
        this.ft = ft;
        this.idGen = idGen;
        this.nt = nt;
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


    //Necessario para limpar os acks que já estão terminados ou então para reenviar os updates
    private Runnable inspectAck = () -> {
        long atualTime = System.currentTimeMillis();
        ArrayList<SupportUpdate> toRemove = new ArrayList<>();
        ArrayList<SupportUpdate> toRemoveAndAdd = new ArrayList<>();

        System.out.println("VOU ANALISR OS UPDATES!!! " + atualTime);
        try {
            lockUpdate.lock();
            for (SupportUpdate su : this.updateSent) {
                System.out.println("Deixa só ver o tempo do su: " + su.time);
                System.out.println("Resultado: " + (atualTime - su.time));
                if (su.nbrs.size() != 0) {
                    if ((atualTime - su.time) > 60000) {
                        //Quer dizer que já passou mais de um minuto e que nem todos receberam os acks
                        System.out.println("Vou ter de enviar novamente o update: " + su.ut);
                        System.out.println("Para os vizinhos: " + su.nbrs);
                        sendUpdate(su.ut, su.nbrs);
                        su.time = System.currentTimeMillis();
                        toRemoveAndAdd.add(su);
                    } else {
                        //Quer dizer que daqui para a frente todos vão ter tempos menores do que um minuto pelo que não é preciso continuar a procurar
                        break;
                    }
                } else {
                    System.out.println("Todos os acks recebidos para: " + su.ut);
                    toRemove.add(su);
                }
            }

            this.updateSent.removeAll(toRemove);
            this.updateSent.removeAll(toRemoveAndAdd);
            this.updateSent.addAll(toRemoveAndAdd);
        }finally {
            lockUpdate.unlock();
        }
    };

    //Quando deixo de ter um vizinho preciso de eliminar daqui também
    //Unica forma de fazer, já que a network table nao tem acesso a isto ...
    private Runnable deleteNbrs = () ->{

        ArrayList<Nodo> myNbrs = this.nt.getNbrsN1();
        System.out.println("Vamos la ver se tenho de eliminar alguem! " + myNbrs.size());

        try {
            lockUpdate.lock();
            for (int i = 0; i < this.updateSent.size(); i++) {
                SupportUpdate su = this.updateSent.get(i);
                System.out.println("O su antes: " + su.nbrs.size());
                su.nbrs.removeIf(n -> !myNbrs.contains(n));
                System.out.println("O su depois: " + su.nbrs.size());
            }
        }finally {
            lockUpdate.unlock();
        }
    };

    private void sendAck(Ack ack, Nodo dest){
        Kryo kryo = new Kryo();

        try {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            kryo.writeClassAndObject(output, ack);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(dest.ip), this.ucp_Update);
            socket.send(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUpdate(UpdateTable ut, ArrayList<Nodo> myNbrs){
        Kryo kryo = new Kryo();

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
    }

    public boolean sendUpdate(UpdateTable ut){

        ArrayList<Nodo> myNbrs = this.nt.getNbrsN1();

        Kryo kryo = new Kryo();

        System.out.println("VOU ENVIAR O SEGUINTE: ");
        System.out.println(ut.toString());

        SupportUpdate su = new SupportUpdate(myNbrs, ut, System.currentTimeMillis());
        //Vou adicionar à lista dos updates enviados
        lockUpdate.lock();
        updateSent.add(su);
        lockUpdate.unlock();
        System.out.println("Vou enviar um update para os meus vizinhos: " + myNbrs.size());

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

    private void sendAgainUpdateTable(Nodo n){
        lockUpdate.lock();
        ArrayList<SupportUpdate> auxUpdate = (ArrayList<SupportUpdate> ) this.updateSent.clone();
        lockUpdate.unlock();

        for(SupportUpdate su: auxUpdate){
            if(su.nbrs.contains(n)){
                //se continver quer dizer que não recebeu o ack, logo poderá ser o pacote que falta receber ...
                try {
                    Kryo kryo = new Kryo();
                    DatagramSocket socket = new DatagramSocket();
                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                    Output output = new Output(bStream);
                    kryo.writeClassAndObject(output, su.ut);
                    output.close();

                    byte[] serializedMessage = bStream.toByteArray();

                    System.out.println("A mandar um EMERGENCYUPDATE para o nodo: " + n.ip);
                    DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.ucp_Update);
                    socket.send(packet);

                    //Para "garantir" que vai por ordem ...
                    Thread.sleep(200);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendEmergencyUpdate(UpdateTable ut){

        EmergencyUpdate eu = new EmergencyUpdate(this.idGen.getID(), myNode);

        try {
            Kryo kryo = new Kryo();
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            kryo.writeClassAndObject(output, eu);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(ut.origin.ip), this.ucp_Update);
            socket.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(ucp_Update, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            ses.scheduleWithFixedDelay(inspectAck, 60, 60, TimeUnit.SECONDS);
            ses.scheduleWithFixedDelay(deleteNbrs, 30, 30, TimeUnit.SECONDS);

            Kryo kryo = new Kryo();

            while(true){

                socket.receive(packet);
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof UpdateTable){
                    System.out.println("Recebi um update table");
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

                            //Vou agora tratar de enviar um ack a dizer que recebi
                            Ack ack = new Ack(this.idGen.getID(), this.myNode, ut.requestID);
                            sendAck(ack, ut.origin);
                        }else{
                            System.out.println("ATENÇÃO ATENÇÃO ATENÇÃO");
                            System.out.println("\t Falta um pacote do UpdateHandler para o nodo: " + myNode.ip);
                            this.sendEmergencyUpdate(ut);
                        }
                        //Para dar tempo no caso de recebermos repetidos ..
                        ses.schedule(delete(requestID), 300, TimeUnit.SECONDS);
                    }else{
                        //Se chega aqui indica que já recebemos o update table no entanto o nodo não recebeu o nosso ack, temos de enviá-lo de novo ...
                        //Depois podemos pôr algo a enviar uma sequência de acks espaçados no tempo só para ter a certeza
                        Ack ack = new Ack(this.idGen.getID(), this.myNode, ut.requestID);
                        sendAck(ack, ut.origin);
                    }
                }else{
                    System.out.println("Recebi um ack!!!");
                    if(header instanceof Ack){
                        System.out.println("Recebi efetivamente um ack!!!");
                        Ack ack = (Ack) header;
                        try {
                            lockUpdate.lock();
                            for (int i = 0; i < this.updateSent.size(); i++) {
                                SupportUpdate su = this.updateSent.get(i);
                                if (ack.responseID.equals(su.ut.requestID)) {
                                    System.out.println("Antes de eliminar da lista: " + su.nbrs);
                                    su.nbrs.remove(ack.origin);
                                    System.out.println("La se foi o ack e o gajo correspondete: " + su.nbrs);
                                    break;
                                }
                            }
                        }finally {
                            lockUpdate.unlock();
                        }
                    }else{
                       if(header instanceof EmergencyUpdate){
                           System.out.println("RECEBI UM EMERGENCY UPDATE!!");
                           EmergencyUpdate eu = (EmergencyUpdate) header;
                           this.sendAgainUpdateTable(eu.origin);
                       }
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
