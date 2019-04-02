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

public class NbrConfirmationHandler implements Runnable {

    private NetworkHandler nh;

    private Nodo myNode;

    private int ucp_NbrConfirmation;
    private int ucp_Alive;

    private NetworkTables nt;


    public NbrConfirmationHandler(NetworkHandler nh, Nodo myNode, int ucp_NbrConfirmation, int ucp_Alive, NetworkTables nt){

        this.nh = nh;

        this.myNode = myNode;

        this.ucp_NbrConfirmation = ucp_NbrConfirmation;
        this.ucp_Alive = ucp_Alive;

        this.nt = nt;
    }

    private void processNbrConfirmation(DatagramPacket nbrConfirmation) {
        Kryo kryo = new Kryo();
        boolean valid = false;

        byte [] buf = nbrConfirmation.getData();
        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof NbrConfirmation) {
            NbrConfirmation nbrc = (NbrConfirmation) header;
            //printNbrConfirmation(nbrc);

            if(this.nh.isNodeValid(nbrc.requestID, nbrc.origin) || (valid = this.nh.isAddNbrValid(nbrc.IDresponse))){
                this.nt.addNbrN1(nbrc.origin);

                //falta adicionar o conteudo do vizinho
                System.out.println("=============================================>New NBR Added");
                if(nbrc.added){
                    sendEmergencyAlive(nbrc);
                }
                else{
                    sendNbrConfirmation(nbrc);
                }

                if(valid) {
                    System.out.println("VOU REGISTAR O ADDNBR E REMOVER DA LISTA DE NBR VÁLIDOS");
                    this.nh.registerNode(nbrc.requestID, nbrc.origin);
                    this.nh.removeAddNbr(nbrc.IDresponse);
                }
            }
            else
                System.out.println("COMBINAÇÃO ID NODO INEXISTENTE");

        }
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

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_Alive);
            (new DatagramSocket()).send(packet);
            System.out.println("EMERGENCY ALIVE ENVIADO\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNbrConfirmation(NbrConfirmation nbrc) {
        NbrConfirmation nc = new NbrConfirmation(nbrc.IDresponse, this.myNode, this.nt.getFileInfo(), nbrc.requestID, true);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, nc);
        output.close();

        byte[] serializedNbrConfirmation = bStream.toByteArray();

        try {
            DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(nbrc.origin.ip), this.ucp_NbrConfirmation);
            (new DatagramSocket()).send(packet);
            System.out.println("NBRCONFIRMATION RESPONSE ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run (){
        try {

            DatagramSocket ucs = new DatagramSocket(this.ucp_NbrConfirmation);
            byte[] buf;
            DatagramPacket nbrConfirmation;

            while (true){
                buf = new byte[1500];
                nbrConfirmation = new DatagramPacket(buf, buf.length);
                ucs.receive(nbrConfirmation);

                // Adicionar o vizinho e o seu conteudo
                    //Preciso do lock para mexer na tabela de vizinhos e tabela de conteudos
                processNbrConfirmation(nbrConfirmation);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

