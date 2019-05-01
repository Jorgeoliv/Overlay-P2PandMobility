package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.Header;
import mensagens.Quit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuitHandler implements Runnable {

    private int ucp_Quit;

    private NetworkHandler nh;
    private NetworkTables nt;

    private IDGen idGen;
    private Nodo myNode;

    private ArrayList<String> ids = new ArrayList<String>();
    private ScheduledExecutorService ses;
    private DatagramSocket ucs;

    public QuitHandler(NetworkHandler nh, NetworkTables nt, int ucp_Quit, IDGen idGen, Nodo myNode){
        this.ucp_Quit = ucp_Quit;

        this.nh = nh;
        this.nt = nt;

        this.idGen = idGen;
        this.myNode = myNode;

        this.ses = Executors.newSingleThreadScheduledExecutor();
        try {
            this.ucs = new DatagramSocket(this.ucp_Quit);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void broadCastQuit(){
        for(Nodo n : this.nt.getNbrsN1())
            sendQuit(n);
    }

    public void sendQuit(Nodo node){

        //Remover vizinho
        this.nt.rmNbrN1(node.id);
        //Remover Vizinhos N2
        this.nt.rmNbrN2(node.id);
        //Remover conteúdos
        this.nh.ft.rmNbr(node.id);

        Quit quit = new Quit(this.idGen.getID(""), this.myNode);
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();


        kryo.writeClassAndObject(output, quit);
        output.close();

        byte[] serializedPing = bStream.toByteArray();

        try {

            DatagramSocket ds = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(node.ip), this.ucp_Quit);

            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);
            Thread.sleep(50);
            ds.send(packet);

            System.out.println("QUIT ENVIADO PARA "+ node.ip + " ENVIADO\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void processQuit(Quit quit) {

        if(this.nt.nbrN1Contains(quit.origin)){
           //Remover vizinho
            this.nt.rmNbrN1(quit.origin.id);
           //Remover Vizinhos N2
            this.nt.rmNbrN2(quit.origin.id);
           //Remover conteúdos
            this.nh.ft.rmNbr(quit.origin.id);
        }
        else
            System.out.println("COMBINAÇÃO ID NODO INEXISTENTE");
    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void kill(){
        this.ses.shutdownNow();
        this.ucs.close();
    }

    public void run() {

        try {
            Kryo kryo = new Kryo();

            byte[] buf;
            DatagramPacket dp;

            while (true){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, buf.length);
                this.ucs.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if (!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 5, TimeUnit.SECONDS);
                    if (header instanceof Quit) {
                        Quit quit = (Quit) header;
                        processQuit(quit);
                    }
                }
            }
        }
        catch (SocketException se){
            System.out.println("\t=>QUITHANDLER DATAGRAMSOCKET CLOSED");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
