package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FilePull;
import mensagens.Header;
import network.IDGen;
import network.Nodo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FilePullHandler implements Runnable{

    private boolean run = true;

    private ScheduledExecutorService ses;
    private Nodo myNode;

    private FilePushHandler fph;
    private IDGen idGen;
    private int ucp_FilePullHandler;

    private int pps = 200;
    private int len = 4000;

    private ArrayList<String> ids = new ArrayList<String>();
    private DatagramSocket ds;
    private HashMap<String, PairNodoFileInfo> pulling;

    public FilePullHandler(int ucp_FilePull, FilePushHandler fph, IDGen idGen, Nodo myNode){
        this.myNode = myNode;

        this.ucp_FilePullHandler = ucp_FilePull;
        this.fph = fph;
        this.idGen = idGen;
        this.ses = Executors.newSingleThreadScheduledExecutor();

        this.pulling = new HashMap<String, PairNodoFileInfo>();

        try {
            this.ds = new DatagramSocket(this.ucp_FilePullHandler);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    private void processFPH(FilePull fp) {

        //System.out.println("RECEBI O FILEPULL " + "\n\t" + fp.fi.name + "\n\t" + fp.fi.hash);
        this.fph.sendFile(fp);
    }

    public void sendToMultipleNodes(String hash, int[] portas){
        Random rand = new Random();

        PairNodoFileInfo choice = this.pulling.get(hash);

        ArrayList<Nodo> nodesToSend;

        int numOfNodes = (int)Math.ceil(choice.fileInfo.numOfFileChunks / this.len);


        if (numOfNodes == 0) {
            numOfNodes = 1;
        }
        if (numOfNodes > choice.nodo.size()) {
            numOfNodes = choice.nodo.size();
        }

        int packetsPerNode = (int) Math.ceil(choice.fileInfo.numOfFileChunks / numOfNodes);

        int pos;

        if(numOfNodes < choice.nodo.size()){
            nodesToSend = new ArrayList<Nodo>(choice.nodo);
            while(nodesToSend.size() > numOfNodes) {
                pos = rand.nextInt(nodesToSend.size());
                nodesToSend.remove(pos);
            }
        }
        else
            nodesToSend = choice.nodo;
        FilePull fp;
        for(int i = 0; i < numOfNodes; i++){

            if(numOfNodes == 1)
                fp = new FilePull(this.idGen.getID(""), this.myNode, choice.fileInfo, portas, this.pps, null);
            else
                fp = new FilePull(this.idGen.getID(""), this.myNode, choice.fileInfo, portas, this.pps, i*packetsPerNode, packetsPerNode);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);

            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, fp);
            output.close();

            byte[] serializedPing = bStream.toByteArray();
            //System.out.println("É ISTO QUE QUERO VER!!!!!!!!!!!!!!!!!!!!!!" + serializedPing.length);

            boolean twoPackets = false;
            int tries = 0;
            int failures = 0;

            while (!twoPackets && tries < 2 && failures < 10) {
                try {
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(nodesToSend.get(i).ip), this.ucp_FilePullHandler);

                    ds.send(packet);
                    tries++;
                    Thread.sleep(50);
                    ds.send(packet);
                    twoPackets = true;
                    Thread.sleep(50);
                    ds.send(packet);

                    //System.out.println("ENVIEI O FILEPULL " + "\n\t" + choice.nodo.ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + choice.fileInfo.hash);
                    this.fph.startReceivers(choice.fileInfo.hash);
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
    }

    public void send(PairNodoFileInfo choice) {
        this.pulling.put(choice.fileInfo.hash, choice);

        this.fph.registerFile(choice.fileInfo, choice.nodo);

        ArrayList<Integer> ports = this.fph.getPorts(choice.fileInfo.hash, choice.fileInfo.numOfFileChunks);

        int [] portas = new int[ports.size()];

        for(int i = 0; i < ports.size(); i++)
            portas[i] = ports.get(i);

        if(choice.nodo.size()> 1) {
            sendToMultipleNodes(choice.fileInfo.hash, portas);
        }
        else {
            FilePull fp = new FilePull(this.idGen.getID(""), this.myNode, choice.fileInfo, portas, this.pps, null);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);

            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, fp);
            output.close();

            byte[] serializedPing = bStream.toByteArray();
            //System.out.println("É ISTO QUE QUERO VER!!!!!!!!!!!!!!!!!!!!!!" + serializedPing.length);

            boolean twoPackets = false;
            int tries = 0;
            int failures = 0;

            while (!twoPackets && tries < 2 && failures < 10) {
                try {
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(choice.nodo.get(0).ip), this.ucp_FilePullHandler);

                    ds.send(packet);
                    tries++;
                    Thread.sleep(50);
                    ds.send(packet);
                    twoPackets = true;
                    Thread.sleep(50);
                    ds.send(packet);

                    //System.out.println("ENVIEI O FILEPULL " + "\n\t" + choice.nodo.ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + choice.fileInfo.hash);
                    this.fph.startReceivers(choice.fileInfo.hash);
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
    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };


    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.ds.close();
    }

    public void run() {
        try{

            Kryo kryo = new Kryo();
            byte[] buf;
            DatagramPacket dp;

            while(this.run){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, buf.length);

                this.ds.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof FilePull) {
                        FilePull filepull = (FilePull) header;
                        processFPH(filepull);
                    }
                }
            }
        }
        catch (SocketException se){
            //System.out.println("\t=>FILEPULLHANDLER DATAGRAMSOCKET CLOSED");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
