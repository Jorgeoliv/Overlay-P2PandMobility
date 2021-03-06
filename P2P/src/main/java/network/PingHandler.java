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
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PingHandler implements Runnable{

    private boolean run = true;

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
    private ArrayList<Ping> pingTray; //Tabuleiro de DatagramPackets

    private ReentrantLock trayLock;
    private IDGen idGen;

    private ArrayList<String> ids = new ArrayList<String>();
    private ScheduledExecutorService ses;

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

        this.pingTray = new ArrayList<Ping>();

        this.trayLock = new ReentrantLock();

        try {
            this.mcs = new MulticastSocket(this.mcport);
            this.ucs = new DatagramSocket();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        this.ses = Executors.newSingleThreadScheduledExecutor();
    }

    private Runnable sendPing = () -> {

        String id = this.idGen.getID("");
        this.nh.registerPing(id);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();

        myN1Nbrs = nt.getNbrsN1();
        myN2Nbrs = nt.getNbrsN2();
        Ping ping = new Ping(id, myNode, ttl, myN1Nbrs, myN2Nbrs);

        kryo.writeClassAndObject(output, ping);
        output.close();

        byte[] serializedPing = bStream.toByteArray();


        boolean twoPackets = false;
        int tries = 0;
        int failures = 0;

        while(!twoPackets && tries < 2 && failures < 2) {
            try {
                MulticastSocket ms = new MulticastSocket();
                DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, this.groupIP, this.mcport);

                ms.send(packet);
                tries++;
                Thread.sleep(100);
                ms.send(packet);
                twoPackets = true;

            } catch (IOException e) {
                System.out.println("\t=======>Network is unreachable");
                failures++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    //ex.printStackTrace();
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        try {


            //System.out.println("PING "+ id + " ENVIADO\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    private Runnable emptyPingTray = () -> {


        this.trayLock.lock();
        ArrayList <Ping> auxPing = (ArrayList<Ping>) this.pingTray.clone();
        this.pingTray.clear();
        this.trayLock.unlock();

        for(Ping ping : auxPing) {

            //printPing(ping);
            if (analisePing(ping)) {
                if(this.nh.registerNode(ping.requestID, ping.origin)) {
                    this.nh.addInConv(ping.origin);
                    sendPong(ping);
                }
            }
        }

    };

    private void printPing(Ping ping) {
        System.out.println("\nRECEBI O PING");
        System.out.println("\n|----------------------------------------");
        System.out.println("|>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n|");
        System.out.println("|TYPE:       Ping");
        System.out.println("|\tPing ID => " + ping.requestID);
        System.out.println("|\tNodo origem => " + ping.origin);

        System.out.println("|\n|");
        System.out.println("|\tNBR N1 => " + ping.nbrN1);
        System.out.println("|\tNBR N2 => " + ping.nbrN2);
        System.out.println("|\n|<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("|----------------------------------------\n");
    }

    private void sendPong(Ping ping) {

        Pong pong = new Pong(this.idGen.getID(""), this.myNode, 64, ping.requestID, this.myN1Nbrs, this.myN2Nbrs);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, pong);
        output.close();

        byte[] serializedPong = bStream.toByteArray();


        boolean twoPackets = false;
        int tries = 0;
        int failures = 0;

        while(!twoPackets && tries < 2 && failures < 10) {
            try {
                DatagramPacket packet = new DatagramPacket(serializedPong, serializedPong.length, InetAddress.getByName(ping.origin.ip), this.ucport);
                this.ucs.send(packet);
                tries++;
                Thread.sleep(50);
                this.ucs.send(packet);
                twoPackets = true;
                Thread.sleep(50);
                this.ucs.send(packet);
            } catch (IOException e) {
                System.out.println("\t=======>Network is unreachable");
                failures++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    //ex.printStackTrace();
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
    }

    private boolean analisePing(Ping ping) {
        boolean decision = false;
        int myNN1 = this.nt.getNumVN1() + this.nh.getInPingConvSize() ;

        // condição 1 VIZINHOS DIRETOS OU EM PROCESSO DE SER
        if((!ping.nbrN1.contains(this.myNode)) && (!this.nt.nbrN1Contains(ping.origin) && (!this.nh.contains(ping.origin)))){
            // condição 2 FALTA DE VIZINHOS??
            if((ping.nbrN1.size() < this.softcap) || (myNN1 < this.softcap)){
                decision = true;
            }
            else{
                // condição 3 SOU VIZINHO DE N2?
                if(!ping.nbrN2.contains(this.myNode)){
                    ArrayList <Nodo> myNbrs = new ArrayList<>(this.myN1Nbrs);
                    myNbrs.addAll(this.myN2Nbrs);
                    ArrayList <Nodo> pingNbrs = new ArrayList<>(ping.nbrN1);
                    pingNbrs.addAll(ping.nbrN2);

                    boolean interception = false;
                    // condição 4 INTERCEÇAO ENTRE OS VIZINHOS DE N1 e N2? MESMA REDE? É PRECISO MANDAR ALGUM QUIT??
                    for (Nodo n : myNbrs)
                        if (pingNbrs.contains(n)) {
                            interception = true;
                            break;
                        }

                        decision = !interception;
                }
            }
        }

        if(decision && myNN1+1 > this.hardcap) {
            this.nh.sendQuit();
        }

        return decision;
    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
          this.ids.remove(0);
    };

    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.mcs.close();
    }

    public void run(){
        try{
            this.mcs.joinGroup(this.groupIP);
            byte[] buf;
            Kryo kryo = new Kryo();
            DatagramPacket dp;

            this.ses.scheduleWithFixedDelay(sendPing, 0, 4, TimeUnit.SECONDS);
            this.ses.scheduleWithFixedDelay(emptyPingTray, 3, 4, TimeUnit.SECONDS);
            InetAddress myIP = InetAddress.getByName(this.myNode.ip);

            while(this.run){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, 1500);
                this.mcs.receive(dp);

                //Filtragem por IP

                if (!dp.getAddress().equals(myIP)) {
                    ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                    Input input = new Input(bStream);
                    Header header = (Header) kryo.readClassAndObject(input);
                    input.close();

                    if(!this.ids.contains(header.requestID)) {
                        this.ids.add(header.requestID);
                        this.ses.schedule(removeID,60,TimeUnit.SECONDS);
                        if (header instanceof Ping) {
                            Ping ping = (Ping) header;
                            this.trayLock.lock();
                            this.pingTray.add(ping);
                            this.trayLock.unlock();
                        } else
                            System.out.println("ERRO NO PARSE DO DATAGRAMPACKET (PINGHANDLER)");
                    }
                }
            }
        }
        catch (SocketException se){
            //System.out.println("\t=>PING MULTICASTSOCKET CLOSED");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
