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

    public QuitHandler(NetworkHandler nh, NetworkTables nt, int ucp_Quit, IDGen idGen, Nodo myNode){
        this.ucp_Quit = ucp_Quit;

        this.nh = nh;
        this.nt = nt;

        this.idGen = idGen;
        this.myNode = myNode;

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
            DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(node.ip), this.ucp_Quit);
            new DatagramSocket().send(packet);
            //System.out.println("QUIT ENVIADO PARA "+ node.ip + " ENVIADO\n");
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

    public void run() {

        try {
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            Kryo kryo = new Kryo();

            DatagramSocket ucs = new DatagramSocket(this.ucp_Quit);

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
                    ses.schedule(removeID, 5, TimeUnit.SECONDS);
                    if (header instanceof Quit) {
                        Quit quit = (Quit) header;
                        processQuit(quit);
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
