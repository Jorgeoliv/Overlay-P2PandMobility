package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.EmergencyAlive;
import mensagens.Header;
import mensagens.NbrConfirmation;

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

public class NbrConfirmationHandler implements Runnable {

    private boolean run = true;

    private NetworkHandler nh;

    private Nodo myNode;

    private int ucp_NbrConfirmation;
    private int ucp_Alive;

    private NetworkTables nt;

    private ArrayList<String> ids = new ArrayList<String>();
    private ScheduledExecutorService ses;
    private DatagramSocket ucs;

    public NbrConfirmationHandler(NetworkHandler nh, Nodo myNode, int ucp_NbrConfirmation, int ucp_Alive, NetworkTables nt){

        this.nh = nh;

        this.myNode = myNode;

        this.ucp_NbrConfirmation = ucp_NbrConfirmation;
        this.ucp_Alive = ucp_Alive;

        this.nt = nt;

        this.ses = Executors.newSingleThreadScheduledExecutor();
        try {
            this.ucs = new DatagramSocket(this.ucp_NbrConfirmation);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    private void processNbrConfirmation(NbrConfirmation nbrc) {

        boolean valid = false;


        //printNbrConfirmation(nbrc);

        if(this.nh.isNodeValid(nbrc.requestID, nbrc.origin) || (valid = this.nh.isAddNbrValid(nbrc.IDresponse))){
            //System.out.println("NBRCONF => " + nbrc.origin.ip + " VS MYNODE => " + this.myNode.ip + "\n\n");
            //System.out.println("RESULTADO DO EQUALS: " + nbrc.origin.equals(this.myNode));
            this.nt.addNbrN1(nbrc.origin);
            this.nh.remInConv(nbrc.origin);

            //Vou adicionar os ficheiros dos vizinhos bem como a hash da sua tabela
            this.nh.ft.addContentForOneNbr(nbrc.fileInfos, nbrc.origin, nbrc.hash);

            //falta adicionar o conteudo do vizinho
            System.out.println("=============================================>New NBR Added\n\tID => " + nbrc.origin.id + "\n\tIP => " + nbrc.origin.ip);
            if(nbrc.added){
                sendEmergencyAlive(nbrc);
            }
            else{
                sendNbrConfirmation(nbrc);
            }

            if(valid) {
                //System.out.println("VOU REGISTAR O ADDNBR E REMOVER DA LISTA DE NBR VÁLIDOS");
                this.nh.registerNode(nbrc.requestID, nbrc.origin);
                this.nh.removeAddNbr(nbrc.IDresponse);
            }
        }
        else
            System.out.println("COMBINAÇÃO ID NODO INEXISTENTE (NBRCONFIRMATION)\n\tID => " + nbrc.origin.id + "\n\tIP => " + nbrc.origin.ip);
    }

    private void printNbrConfirmation(NbrConfirmation nbrc) {
        System.out.println("\nRECEBI O NBRCONFIRMATION");
        System.out.println("\n|----------------------------------------");
        System.out.println("|>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n|");
        System.out.println("|TYPE:       NbrConfirmation");
        System.out.println("|\tNbrConfirmation ID => " + nbrc.requestID);
        System.out.println("|\tNodo origem => " + nbrc.origin);

        System.out.println("|\n|");
        System.out.println("|\tResponse ID => " + nbrc.IDresponse);
        System.out.println("|\tAdded => " + nbrc.added);
        System.out.println("|\tFileInfos => " + nbrc.fileInfos);
        System.out.println("|\n|<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("|----------------------------------------\n");
    }

    private void sendEmergencyAlive(NbrConfirmation nbrc){
        Kryo kryo = new Kryo();

        EmergencyAlive ealive = new EmergencyAlive(nbrc.IDresponse, this.myNode, this.nt.getNbrsN1(),nbrc.requestID, false);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            kryo.writeClassAndObject(output, ealive);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

        boolean twoPackets = true;
        int tries = 0;
        while(twoPackets && tries < 2) {
            try {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_Alive);

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

    private void sendNbrConfirmation(NbrConfirmation nbrc) {
        NbrConfirmation nc = new NbrConfirmation(nbrc.IDresponse, this.myNode, this.nt.getFileInfo(), nbrc.requestID, true, this.nh.ft.getMyHash());

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
                DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_NbrConfirmation);

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

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.ucs.close();
    }

    public void run (){
        try {

            Kryo kryo = new Kryo();

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

                if(!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof NbrConfirmation) {
                        NbrConfirmation nbrc = (NbrConfirmation) header;
                        processNbrConfirmation(nbrc);
                    }
                }
            }

        }catch (SocketException se){
            System.out.println("\t=>NBRCONFIRMATIONHANDLER DATAGRAMSOCKET CLOSED");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}

