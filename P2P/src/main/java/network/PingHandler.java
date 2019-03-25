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
    public Nodo myNode;
    public ArrayList<Nodo> myN1Nbrs;
    public ArrayList<Nodo> myN2Nbrs;

    public InetAddress groupIP;
    public int mcport;
    public int ucport;
    public int ttl;

    public MulticastSocket mcs;
    private DatagramSocket ucs;

    public NetworkTables nt; //Para obter os vizinhos
    public ArrayList<DatagramPacket> pingTray; //Tabuleiro de DatagramPackets

    public int standartNumNbrs;
    private ReentrantLock trayLock;

    public PingHandler(Nodo myNode, InetAddress ip, int mcport, int ucport, int ttl, NetworkTables nt, int standartNumNbrs){
        this.myNode = myNode;
        this.myN1Nbrs = null;
        this.myN2Nbrs = null;

        this.groupIP = ip;
        this.mcport = mcport;
        this.ucport = ucport;
        this.ttl = ttl;

        this.nt = nt;

        this.pingTray = new ArrayList<DatagramPacket>();

        this.standartNumNbrs = standartNumNbrs;
        this.trayLock = new ReentrantLock();

        try {
            this.mcs = new MulticastSocket(this.mcport);
            this.ucs = new DatagramSocket();
        }
        catch(IOException e){
            e.printStackTrace();
        }


    }

    Runnable sendPing = () -> {
        myN1Nbrs = nt.getNbrsN1();
        myN2Nbrs = nt.getNbrsN2();

        Ping ping = new Ping("teste", myNode, ttl, myN1Nbrs, myN2Nbrs);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, ping);
        output.close();

        byte[] serializedPing = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, this.groupIP, this.mcport);


        try {
            new MulticastSocket().send(packet);
            System.out.println("PING ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    Runnable emptyTray = () -> {
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

                if (analisePing(ping))
                    sendPong(ping);

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
        Pong pong = new Pong("resposta", this.myNode, 64, ping.requestID, this.myN1Nbrs, this.myN2Nbrs);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, pong);
        output.close();

        byte[] serializedPong = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedPong, serializedPong.length, ping.origin.ip, this.ucport);

        try {
            this.ucs.send(packet);
            System.out.println("PONG ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean analisePing(Ping ping) {
        boolean decision = false;


        // condição 1
        if(!ping.nbrN1.contains(this.myNode)){
            // condição 2
            if((ping.nbrN1.size() + ping.nbrN2.size()) < this.standartNumNbrs || (this.myN1Nbrs.size() + this.myN2Nbrs.size()) < this.standartNumNbrs)
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

        return decision;
    }

    public void run(){
        try{
            this.mcs.joinGroup(this.groupIP);
            byte[] buf;
            DatagramPacket ping;

            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(sendPing, 0, 4, TimeUnit.SECONDS);
            ses.scheduleWithFixedDelay(emptyTray, 2, 4, TimeUnit.SECONDS);

            while(true){
                buf = new byte[1500];
                ping = new DatagramPacket(buf, 1500);
                mcs.receive(ping);
                if (!ping.getAddress().equals(this.myNode.ip)) {
                    this.trayLock.lock();
                    this.pingTray.add(ping);
                    this.trayLock.unlock();
                    System.out.println("PING RECEBIDO\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
