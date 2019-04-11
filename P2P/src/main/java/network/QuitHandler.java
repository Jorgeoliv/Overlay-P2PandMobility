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

public class QuitHandler implements Runnable {

    private int ucp_Quit;

    private NetworkHandler nh;
    private NetworkTables nt;

    private IDGen idGen;
    private Nodo myNode;

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
        this.nt.rmNbrN1(node);
        //Remover Vizinhos N2
        this.nt.rmNbrN2(node);
        //Remover conteúdos
        //?????????????????????????????????????????????????

        Quit quit = new Quit(this.idGen.getID(), this.myNode);
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();


        kryo.writeClassAndObject(output, quit);
        output.close();

        byte[] serializedPing = bStream.toByteArray();

        try {
            DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(node.ip), this.ucp_Quit);
            new DatagramSocket().send(packet);
            System.out.println("QUIT ENVIADO PARA "+ node.ip + " ENVIADO\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void processQuit(DatagramPacket quitdp) {

        Kryo kryo = new Kryo();

        byte [] buf = quitdp.getData();
        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof Quit) {
            Quit quit = (Quit) header;

            if(this.nt.nbrN1Contains(quit.origin)){
               //Remover vizinho
                this.nt.rmNbrN1(quit.origin);
               //Remover Vizinhos N2
                this.nt.rmNbrN2(quit.origin);
               //Remover conteúdos
                //?????????????????????????????????????????????????

            }
            else
                System.out.println("COMBINAÇÃO ID NODO INEXISTENTE");

        }
    }

    public void run() {

        try {
            DatagramSocket ucs = new DatagramSocket(this.ucp_Quit);
            byte[] buf;
            DatagramPacket quit;

            while (true){
                buf = new byte[1500];
                quit = new DatagramPacket(buf, buf.length);
                ucs.receive(quit);

                processQuit(quit);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
