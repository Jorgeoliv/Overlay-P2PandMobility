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

    private void processAddNbr(DatagramPacket addnbr) {
        Kryo kryo = new Kryo();
        byte[] buf = addnbr.getData();

        System.out.println("RECEBI UM ADDNBR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof AddNbr) {
            AddNbr addNbr = (AddNbr) header;
            printAddNbr(addNbr);

            if (this.nt.getNbrsN1().contains(addNbr.intermediary)){
                //Confirmar se o nodo intermediário tem o nodo originário desta msg como vizinho?? PROBLEMAS??
                sendNbrConfirmation(addNbr);
            }
            else{
                System.out.println("NODO INTERMEDIÁRIO DESCONHECIDO");
            }
        }
        else
            System.out.println("ERRO NO PARSE DO ADDNBR");
    }

    private void sendNbrConfirmation(AddNbr addNbr) {

        NbrConfirmation nc = new NbrConfirmation(this.idGen.getID(), this.myNode, this.nt.getFileInfo(), addNbr.requestID, false);

        this.nh.registerNode(addNbr.requestID, addNbr.origin);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, nc);
        output.close();

        byte[] serializedNbrConfirmation = bStream.toByteArray();


        try {
            DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(addNbr.origin.ip), this.ucp_NbrConfirmation);
            (new DatagramSocket()).send(packet);
            System.out.println("NBRCONFIRMATION ENVIADO\n");
        } catch (IOException e) {
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

                    String id = this.idGen.getID();
                    AddNbr addNbr = new AddNbr(id, this.myNode, nN1);

                    this.nh.registerAddNbr(id);

                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                    Output output = new Output(bStream);

                    Kryo kryo = new Kryo();
                    kryo.writeClassAndObject(output, addNbr);
                    output.close();

                    byte[] serializedAddNbr = bStream.toByteArray();
                    try {
                        DatagramPacket packet = new DatagramPacket(serializedAddNbr, serializedAddNbr.length, InetAddress.getByName(nN2.ip), this.ucp_AddNbr);
                        (new DatagramSocket()).send(packet);
                        System.out.println("ADDNBR ENVIADO PARA " + nN2.ip + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                    System.out.println("SEM VIZINHOS DE NÍVEL 2!!");
            }
        }
    };

    public void run() {
        try {

            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(sendAddNbr, 20, 20, TimeUnit.SECONDS);

            DatagramSocket ucs = new DatagramSocket(this.ucp_AddNbr);
            byte[] buf;
            DatagramPacket addNbr;

            while (true){
                buf = new byte[1500];
                addNbr = new DatagramPacket(buf, buf.length);
                ucs.receive(addNbr);

                processAddNbr(addNbr);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
