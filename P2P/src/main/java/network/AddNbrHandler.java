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

    public void run() {
        try {

            DatagramSocket ucs = new DatagramSocket(this.ucp_AddNbr);
            byte[] buf;
            DatagramPacket addNbr;

            while (true){
                buf = new byte[1500];
                addNbr = new DatagramPacket(buf, buf.length);
                ucs.receive(addNbr);

                System.out.println("RECEBI O ADDNBR!!!!!!!!!!!!!!!!!!!!");

                processAddNbr(addNbr);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processAddNbr(DatagramPacket addnbr) {
        Kryo kryo = new Kryo();
        byte[] buf = addnbr.getData();

        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof AddNbr) {
            AddNbr addNbr = (AddNbr) header;

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
}
