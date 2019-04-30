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
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AddNbrHandler implements Runnable{

    private int softcap;
    private int hardcap;

    private IDGen idGen;

    private Nodo myNode;

    private int ucp_AddNbr;
    private int ucp_NbrConfirmation;

    private NetworkTables nt;
    private NetworkHandler nh;

    private ArrayList<String> ids = new ArrayList<String>();

    public AddNbrHandler(int softcap, int hardcap, IDGen idGen, NetworkHandler nh, Nodo myNode, int ucp_AddNbr, int ucp_NbrConfirmation, NetworkTables nt){
        this.softcap = softcap;
        this.hardcap = hardcap;

        this.idGen = idGen;

        this.myNode = myNode;

        this.ucp_AddNbr = ucp_AddNbr;
        this.ucp_NbrConfirmation = ucp_NbrConfirmation;

        this.nh = nh;
        this.nt = nt;
    }

    private void processAddNbr(AddNbr addnbr) {

        //printAddNbr(addNbr);
        if (this.nt.getNbrsN1().contains(addnbr.intermediary)){
            this.nh.addInConv(addnbr.origin);
            sendNbrConfirmation(addnbr);
        }
        else{
            System.out.println("NODO INTERMEDIÁRIO DESCONHECIDO");
        }
    }

    private void sendNbrConfirmation(AddNbr addNbr) {

        NbrConfirmation nc = new NbrConfirmation(this.idGen.getID(""), this.myNode, this.nt.getFileInfo(), addNbr.requestID, false,  this.nh.ft.getMyHash());

        this.nh.registerNode(addNbr.requestID, addNbr.origin);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, nc);
        output.close();

        byte[] serializedNbrConfirmation = bStream.toByteArray();


        try {
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(addNbr.origin.ip), this.ucp_NbrConfirmation);

            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            //System.out.println("NBRCONFIRMATION ENVIADO\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        try {
                            DatagramSocket ds = new DatagramSocket();
                            DatagramPacket packet = new DatagramPacket(serializedAddNbr, serializedAddNbr.length, InetAddress.getByName(nN2.ip), this.ucp_AddNbr);
                            ds.send(packet);
                            Thread.sleep(50);
                            ds.send(packet);
                            Thread.sleep(50);
                            ds.send(packet);
                            //System.out.println("ADDNBR ENVIADO PARA " + nN2.ip + "\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        System.out.println("NÃO PRECISO DE ADICIONAR ESTE NODO COMO VIZINHO!!");
                } else
                    System.out.println("SEM VIZINHOS DE NÍVEL 2!!");
            }
        }
    };

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void run() {
        try {
            Kryo kryo = new Kryo();

            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(sendAddNbr, 20, 20, TimeUnit.SECONDS);

            DatagramSocket ucs = new DatagramSocket(this.ucp_AddNbr);
            byte[] buf;
            DatagramPacket dp;

            while (true){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, buf.length);
                ucs.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if (!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof AddNbr) {
                        AddNbr addNbr = (AddNbr) header;
                        processAddNbr(addNbr);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
