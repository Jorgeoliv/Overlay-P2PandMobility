package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.*;
import network.NetworkTables;
import network.Nodo;
import org.boon.json.JsonParser;
import org.boon.json.JsonParserFactory;

import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliveHandler implements Runnable {

    private int ucp_Alive; //Porta para escutar
    private Nodo myNode;
    private NetworkHandler nh;
    private NetworkTables nt;
    private IDGen idGen;

    private ArrayList <Alive> aliveTray;

    public AliveHandler(NetworkHandler nh, NetworkTables nt, Nodo id, int ucp_Alive, IDGen idGen) {
        this.nh = nh;
        this.nt = nt;
        this.myNode = id;

        this.ucp_Alive = ucp_Alive;

        this.idGen = idGen;

        this.aliveTray = new ArrayList<Alive>();
    }
    private Runnable emptyAliveTray = () ->{
        if(this.aliveTray.size() > 0){
            for(Alive alive : this.aliveTray) {
                this.nt.reset(alive.origin.id);
                this.nt.updateNbrN2(alive.origin.id, alive.nbrN1);
            }
        }
    };

    private Runnable sendAlive = () -> {

        ArrayList<Nodo> myNbrs = nt.getNbrsN1();
        ArrayList<Nodo> myNbrsN2 = nt.getNbrsN2();

        System.out.println("Vou enviar as coisas para " + myNbrs.size() + " vizinhos!!!");

        /**
         * ISTO DEPPIS VAI PARA O CREATE PACKET!!!
         */
        Alive a = new Alive(this.idGen.getID(), this.myNode, myNbrs,  myNbrsN2);
        Kryo kryo = new Kryo();

        for(Nodo n: myNbrs){
            try {
                DatagramSocket socket = new DatagramSocket();
                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);
                kryo.writeClassAndObject(output, a);
                output.close();

                byte[] serializedMessage = bStream.toByteArray();

                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(n.ip), this.ucp_Alive);
                socket.send(packet);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable upAll = () -> {
        System.out.println("Vou incrementar tudo!!!");
        this.nt.inc();
    };


    private void processEmergencyAlive(EmergencyAlive ealive) {
        if(this.nh.contains(ealive.origin)){
            //confirmar ids!!!!
            if(this.nh.isNodeValid(ealive.requestID, ealive.origin)){
                //adicionar vizinhos
                this.nt.addNbrN1(ealive.nbrN1);

                //reset das variáveis
                this.nt.reset(ealive.origin.id);
                this.nt.updateNbrN2(ealive.origin.id, ealive.nbrN1);

                if(!ealive.updated){
                    //enviar emergencyalive!!!
                    sendEmergencyAlive(ealive);
                }
            }
            else
                System.out.println("NÃO EXISTE RELAÇÃO ENTRE O ID E O NODO DA MSG");
        }
        else
            System.out.println("Nodo Desconhecido");
    }

    private void sendEmergencyAlive(EmergencyAlive ea){
        Kryo kryo = new Kryo();

        EmergencyAlive ealive = new EmergencyAlive(ea.IDresponse, this.myNode, this.nt.getNbrsN1(),ea.requestID, true);

        try {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);
            kryo.writeClassAndObject(output, ealive);
            output.close();

            byte[] serializedMessage = bStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(ea.origin.ip), this.ucp_Alive);
            (new DatagramSocket()).send(packet);
            System.out.println("EMERGENCY ALIVE ENVIADO");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            DatagramSocket ucs_Alive = new DatagramSocket(this.ucp_Alive);

            byte[] buffer;
            DatagramPacket dp;

            //schedulers
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
            ses.scheduleAtFixedRate(sendAlive, 0, 20, TimeUnit.SECONDS);
            ses.scheduleAtFixedRate(emptyAliveTray, 1, 20, TimeUnit.SECONDS);
            ses.scheduleAtFixedRate(upAll, 59, 20, TimeUnit.SECONDS);

            Kryo kryo = new Kryo();

            while(true) {
                buffer = new byte[1500];
                dp = new DatagramPacket(buffer, buffer.length);

                ucs_Alive.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);

                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if (header instanceof Alive)
                    this.aliveTray.add((Alive) header);
                else
                    if(header instanceof EmergencyAlive) {
                        processEmergencyAlive((EmergencyAlive) header);
                        System.out.println("EMERCENCY ALIVE RECEBIDO");
                    }
                    else
                        System.out.println("ERRO AO PROCESSAR ALIVE");
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}