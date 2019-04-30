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
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NbrConfirmationHandler implements Runnable {

    private NetworkHandler nh;

    private Nodo myNode;

    private int ucp_NbrConfirmation;
    private int ucp_Alive;

    private NetworkTables nt;

    private ArrayList<String> ids = new ArrayList<String>();

    public NbrConfirmationHandler(NetworkHandler nh, Nodo myNode, int ucp_NbrConfirmation, int ucp_Alive, NetworkTables nt){

        this.nh = nh;

        this.myNode = myNode;

        this.ucp_NbrConfirmation = ucp_NbrConfirmation;
        this.ucp_Alive = ucp_Alive;

        this.nt = nt;
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
            System.out.println("=============================================>New NBR Added");
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
            System.out.println("COMBINAÇÃO ID NODO INEXISTENTE");
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

        try {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            kryo.writeClassAndObject(output, ealive);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_Alive);

            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            //System.out.println("EMERGENCY ALIVE ENVIADO\n");
        }
        catch (Exception e) {
            e.printStackTrace();
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

        try {
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_NbrConfirmation);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            //System.out.println("NBRCONFIRMATION RESPONSE ENVIADO\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void run (){
        try {
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            Kryo kryo = new Kryo();

            DatagramSocket ucs = new DatagramSocket(this.ucp_NbrConfirmation);
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

                if(!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    ses.schedule(removeID, 5, TimeUnit.SECONDS);

                    if (header instanceof NbrConfirmation) {
                        NbrConfirmation nbrc = (NbrConfirmation) header;
                        processNbrConfirmation(nbrc);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

