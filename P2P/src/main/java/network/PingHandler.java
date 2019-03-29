package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.Header;
import mensagens.Ping;
import mensagens.Pong;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PingHandler implements Runnable{
    private int softcap;
    private int hardcap;

    private NetworkHandler nh;

    private Nodo myNode;
    private ArrayList<Nodo> myN1Nbrs;
    private ArrayList<Nodo> myN2Nbrs;

    private InetAddress groupIP;
    private int mcport;
    private int ucport;
    private int ttl;

    private MulticastSocket mcs;
    private DatagramSocket ucs;

    private NetworkTables nt; //Para obter os vizinhos
    private ArrayList<DatagramPacket> pingTray; //Tabuleiro de DatagramPackets

    private ReentrantLock trayLock;
    private IDGen idGen;

    public PingHandler(int softcap, int hardcap, NetworkHandler nh, IDGen idGen, Nodo myNode, InetAddress ip, int mcport, int ucport, int ttl, NetworkTables nt){
        this.softcap = softcap;
        this.hardcap = hardcap;

        this.nh = nh;
        this.idGen = idGen;

        this.myNode = myNode;
        this.myN1Nbrs = null;
        this.myN2Nbrs = null;

        this.groupIP = ip;
        this.mcport = mcport;
        this.ucport = ucport;
        this.ttl = ttl;

        this.nt = nt;

        this.pingTray = new ArrayList<DatagramPacket>();

        this.trayLock = new ReentrantLock();

        try {
            this.mcs = new MulticastSocket(this.mcport);
            this.ucs = new DatagramSocket();
        }
        catch(IOException e){
            e.printStackTrace();
        }


    }

    private Runnable sendPing = () -> {
        myN1Nbrs = nt.getNbrsN1();
        myN2Nbrs = nt.getNbrsN2();

        String id = this.idGen.getID();
        Ping ping = new Ping(id, myNode, ttl, myN1Nbrs, myN2Nbrs);

        this.nh.registerPing(id);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, ping);
        output.close();

        byte[] serializedPing = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, this.groupIP, this.mcport);


        try {
            new MulticastSocket().send(packet);
            System.out.println("PING "+ id + " ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private Runnable emptyPingTray = () -> {
        byte[] buf;
        Kryo kryo = new Kryo();

        this.trayLock.lock();

        for(DatagramPacket dp : this.pingTray){

            buf = dp.getData();
            ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
            Input input = new Input(bStream);
            Header header = (Header) kryo.readClassAndObject(input);
            input.close();

            if(header instanceof Ping){
                Ping ping = (Ping) header;

                if (analisePing(ping)) {
                    sendPong(ping);
                    this.nh.registerNode(ping.requestID, ping.origin);
                }
                else
                    System.out.println("NÃO PRECISO DE ENVIAR PONG");
            }
            else
                System.out.println("ERRO NO PARSE DO DATAGRAMPACKET (PINGHANDLER)");

        }
        this.pingTray.clear();
        this.trayLock.unlock();
    };

    private void sendPong(Ping ping) {

        Pong pong = new Pong(this.idGen.getID(), this.myNode, 64, ping.requestID, this.myN1Nbrs, this.myN2Nbrs);

        //System.out.println("Pong enviado\n\tsendTO: "+ ping.origin.ip + "\n\tmynode.ip => " + this.myNode.ip + "\n\tpong.requestID => " + pong.requestID +"\n\tpong.pingID => " +pong.pingID);
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, pong);
        output.close();

        byte[] serializedPong = bStream.toByteArray();


        try {
            DatagramPacket packet = new DatagramPacket(serializedPong, serializedPong.length, InetAddress.getByName(ping.origin.ip), this.ucport);
            this.ucs.send(packet);
            System.out.println("PONG ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean analisePing(Ping ping) {
        boolean decision = false;


        // condição 1
        if((!ping.nbrN1.contains(this.myNode)) && (!this.nh.contains(ping.origin))){
            // condição 2
            if((ping.nbrN1.size() + ping.nbrN2.size()) < this.softcap || (this.nt.getNumVN1() + this.nt.getNumVN2()) < this.softcap)
                decision = true;
            else{
                // condição 3
                if(!ping.nbrN2.contains(this.myNode)){
                    ArrayList <Nodo> myNbrs = new ArrayList<>(this.myN1Nbrs);
                    myNbrs.addAll(this.myN2Nbrs);
                    ArrayList <Nodo> pingNbrs = new ArrayList<>(ping.nbrN1);
                    pingNbrs.addAll(ping.nbrN2);

                    boolean interception = false;
                    // condição 4
                    for (Nodo n : myNbrs)
                        if (pingNbrs.contains(n))
                            interception = true;

                        decision = !interception;
                }
            }
        }
        else
            System.out.println("JÁ É MEU VIZINHO");

        return decision;
    }

    public void run(){
        try{
            this.mcs.joinGroup(this.groupIP);
            byte[] buf;
            DatagramPacket ping;

            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(sendPing, 0, 10, TimeUnit.SECONDS);
            ses.scheduleWithFixedDelay(emptyPingTray, 4, 10, TimeUnit.SECONDS);
            InetAddress myIP = InetAddress.getByName(this.myNode.ip);

            while(true){
                buf = new byte[1500];
                ping = new DatagramPacket(buf, 1500);
                mcs.receive(ping);
                //Filtragem por IP

                if (!ping.getAddress().equals(myIP)) {
                    this.trayLock.lock();
                    this.pingTray.add(ping);
                    this.trayLock.unlock();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
