package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AddNbrHandler implements Runnable{

    private boolean run = true;

    private int softcap;
    private int hardcap;

    private IDGen idGen;

    private Nodo myNode;

    private int ucp_AddNbr;
    private int ucp_NbrConfirmation;

    private NetworkTables nt;
    private NetworkHandler nh;

    private ArrayList<String> ids = new ArrayList<String>();
    private ScheduledExecutorService ses;
    private DatagramSocket ucs;

    public AddNbrHandler(int softcap, int hardcap, IDGen idGen, NetworkHandler nh, Nodo myNode, int ucp_AddNbr, int ucp_NbrConfirmation, NetworkTables nt){
        this.softcap = softcap;
        this.hardcap = hardcap;

        this.idGen = idGen;

        this.myNode = myNode;

        this.ucp_AddNbr = ucp_AddNbr;
        this.ucp_NbrConfirmation = ucp_NbrConfirmation;

        this.nh = nh;
        this.nt = nt;

        this.ses = Executors.newSingleThreadScheduledExecutor();
        try {
            this.ucs = new DatagramSocket(this.ucp_AddNbr);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    private void processAddNbr(AddNbr addnbr) {

        //printAddNbr(addNbr);
        if (this.nt.getNbrsN1().contains(addnbr.intermediary)){
            if(this.nh.registerNode(addnbr.requestID, addnbr.origin)) {
                this.nh.addInConv(addnbr.origin);
                sendNbrConfirmation(addnbr);
            }

        }
        else{
            System.out.println("NODO INTERMEDIÁRIO DESCONHECIDO");
        }
    }

    private void sendNbrConfirmation(AddNbr addNbr) {

        NbrConfirmation nc = new NbrConfirmation(this.idGen.getID(""), this.myNode, this.nt.getFileInfo(), addNbr.requestID, false,  this.nh.ft.getMyHash());


        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, nc);
        output.close();

        byte[] serializedNbrConfirmation = bStream.toByteArray();


        boolean twoPackets = true;
        int tries = 0;
        while(twoPackets && tries < 2) {
            try {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(addNbr.origin.ip), this.ucp_NbrConfirmation);

                ds.send(packet);
                tries++;
                Thread.sleep(50);
                ds.send(packet);
                twoPackets = true;
                Thread.sleep(50);
                ds.send(packet);

            } catch (IOException e) {
                System.out.println("\t=======>Network is unreachable");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //System.out.println("SENT NBRCONFIRMATION(ADDNBR)");
    }

    private void printAddNbr(AddNbr addNbr) {
        System.out.println("\nRECEBI O ADDNBR");
        System.out.println("\n|----------------------------------------");
        System.out.println("|>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n|");
        System.out.println("|TYPE:       AddNbr");
        System.out.println("|\tAddNbr ID => " + addNbr.requestID);
        System.out.println("|\tNodo origem => " + addNbr.origin);

        System.out.println("|\n|");
        System.out.println("|\tIntermediary => " + addNbr.intermediary);
        System.out.println("|\n|<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("|----------------------------------------\n");
    }

    private Runnable sendAddNbr = () -> {
        int numNN1 = this.nt.getNumVN1();

        if(numNN1 < this.softcap) {
            if (numNN1 > 0) {
                Nodo nN1 = this.nt.getRandomNN1();
                if (nN1 != null) {
                    Nodo nN2 = this.nt.getRandomNN2(nN1);
                    //System.out.println("QUEM É QUE ESCOLHI!!!!!!\n" + "\t" + nN2 + "\n\t" + nN2.equals(this.myNode));
                    if (!this.nh.isNodePresent(nN2)) {
                        String id = this.idGen.getID("");
                        AddNbr addNbr = new AddNbr(id, this.myNode, nN1);

                        this.nh.registerAddNbr(id);
                        this.nh.addInConv(nN2);

                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        Output output = new Output(bStream);

                        Kryo kryo = new Kryo();
                        kryo.writeClassAndObject(output, addNbr);
                        output.close();

                        byte[] serializedAddNbr = bStream.toByteArray();

                        boolean twoPackets = true;
                        int tries = 0;
                        while(twoPackets && tries < 2) {
                            try {
                                DatagramSocket ds = new DatagramSocket();
                                DatagramPacket packet = new DatagramPacket(serializedAddNbr, serializedAddNbr.length, InetAddress.getByName(nN2.ip), this.ucp_AddNbr);

                                ds.send(packet);
                                tries++;
                                Thread.sleep(50);
                                ds.send(packet);
                                twoPackets = true;
                                Thread.sleep(50);
                                ds.send(packet);

                            } catch (IOException e) {
                                System.out.println("\t=======>Network is unreachable");
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    };

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.ucs.close();
    }

    public void run() {
        try {
            Kryo kryo = new Kryo();


            this.ses.scheduleWithFixedDelay(sendAddNbr, 20, 10, TimeUnit.SECONDS);

            byte[] buf;
            DatagramPacket dp;

            while(this.run){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, buf.length);
                this.ucs.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if (!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof AddNbr) {
                        AddNbr addNbr = (AddNbr) header;
                        processAddNbr(addNbr);
                    }
                }
            }

        }catch (SocketException se){
            //System.out.println("\t=>ADDNBRHANDLER DATAGRAMSOCKET CLOSED");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
