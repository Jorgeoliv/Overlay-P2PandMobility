package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.Header;
import mensagens.NbrConfirmation;
import mensagens.Pong;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PongHandler implements Runnable {
    private NetworkHandler nh;

    private int softcap;
    private int hardcap;

    private Nodo myNode;
    private int ucp_Pong;
    private int ucp_NbrConfirmation;
    private int ucp_AddNbr;
    private NetworkTables nt;

    private IDGen idGen;

    private ArrayList <DatagramPacket> pongTray;
    private ReentrantLock trayLock;

    private DatagramSocket ucs;

    public PongHandler(int softcap, int hardcap, NetworkHandler nh, IDGen idGen, Nodo myNode, int ucp_Pong, int ucp_NbrConfirmation, int ucp_AddNbr, NetworkTables nt){
        this.nh = nh;

        this.softcap = softcap;
        this.hardcap= hardcap;

        this.myNode = myNode;
        this.ucp_Pong = ucp_Pong;
        this.ucp_NbrConfirmation = ucp_NbrConfirmation;
        this.ucp_AddNbr = ucp_AddNbr;
        this.nt = nt;

        this.idGen = idGen;

        this.pongTray = new ArrayList<DatagramPacket>();
        this.trayLock = new ReentrantLock();

        try {
            this.ucs = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Runnable emptyPongTray = () -> {
        byte[] buf;
        Kryo kryo = new Kryo();


        this.trayLock.lock();
        ArrayList<DatagramPacket> auxPong = (ArrayList<DatagramPacket>) this.pongTray.clone();
        int n_pongs = this.pongTray.size();
        int nNbrs = this.nt.getNumVN1();
        int vagas = this.softcap - nNbrs;
        this.pongTray.clear();
        this.trayLock.unlock();



        if(n_pongs <= vagas) {
            //System.out.println("PRIMEIRA ESCOLHA n_pongs " + n_pongs + " vagas " + vagas);
            //Verificar se o numero de pongs no tray é menor que o número de lugares restantes para vizinhos
            //Se FOR, enviar NBRConfirmations a todos
            for(DatagramPacket dp : auxPong){

                buf = dp.getData();
                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof Pong){
                    Pong pong = (Pong) header;
                    //printPong(pong);
                    if(this.nh.isPingValid(pong.pingID)) {
                        //Enviar NBRConfirmation
                        this.nh.addInConv(pong.origin);
                        sendNbrConfirmation(pong);
                        System.out.println("ENVIOU NBRCONFIRMATION");
                    }
                    else
                        System.out.println("PING INVÁLIDO");

                }
                else
                    System.out.println("ERRO NO PARSE DO DATAGRAMPACKET (PONGHANDLER)");

            }
        }
        else{
            //Se for MAIOR escolher ao calhas o número de lugares restantes
            //System.out.println("SEGUNDA ESCOLHA n_pongs " + n_pongs + " vagas " + vagas);

            Random random = new Random();
            for(int i = 0; i < vagas; i++){
                System.out.println("ESCOLHA DE UM VIZINHO Nº " + i + "/" + vagas);
                DatagramPacket dp = auxPong.get(random.nextInt(auxPong.size()));
                auxPong.remove(dp);

                buf = dp.getData();
                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof Pong){
                    Pong pong = (Pong) header;
                    if(this.nh.isPingValid(pong.pingID)) {
                        //Enviar NBRConfirmation
                        sendNbrConfirmation(pong);
                    }
                    else {
                        System.out.println("PING INVÁLIDO");
                        i--;
                    }
                }
                else
                    System.out.println("ERRO NO PARSE DO DATAGRAMPACKET (PONGHANDLER)");
            }
        }
    };

    private void printPong(Pong pong) {
        System.out.println("\nRECEBI O PONG");
        System.out.println("\n|----------------------------------------");
        System.out.println("|>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n|");
        System.out.println("|TYPE:       Pong");
        System.out.println("|\tPong ID => " + pong.requestID);
        System.out.println("|\tNodo origem => " + pong.origin);

        System.out.println("|\n|");
        System.out.println("|\tResponse ID => " + pong.pingID);
        System.out.println("|\tNBR N1 => " + pong.nbrN1);
        System.out.println("|\tNBR N2 => " + pong.nbrN2);
        System.out.println("|\n|<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("|----------------------------------------\n");
    }

    private void sendNbrConfirmation(Pong pong) {
        NbrConfirmation nc = new NbrConfirmation(pong.pingID, this.myNode, this.nt.getFileInfo(), pong.requestID, false,  this.nh.ft.getMyHash());

        this.nh.registerNode(pong.requestID, pong.origin);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, nc);
        output.close();

        byte[] serializedNbrConfirmation = bStream.toByteArray();


        try {
            DatagramPacket packet = new DatagramPacket(serializedNbrConfirmation, serializedNbrConfirmation.length, InetAddress.getByName(pong.origin.ip), this.ucp_NbrConfirmation);
            this.ucs.send(packet);
            System.out.println("NBRCNFIRMATION ENVIADO\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(){
        try {
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(emptyPongTray, 0, 2, TimeUnit.SECONDS);

            DatagramSocket ucs = new DatagramSocket(this.ucp_Pong);
            byte[] buf;
            DatagramPacket pong;

            while (true){
                buf = new byte[1500];
                pong = new DatagramPacket(buf, 1500);
                ucs.receive(pong);

                this.trayLock.lock();
                this.pongTray.add(pong);
                this.trayLock.unlock();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
