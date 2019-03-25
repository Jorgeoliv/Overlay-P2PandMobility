package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.Header;
import mensagens.Ping;
import mensagens.Pong;

import javax.xml.crypto.Data;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PongHandler implements Runnable {
    public Nodo myNode;
    public int ucp;
    public NetworkTables nt;

    public ArrayList <DatagramPacket> pongTray;
    private ReentrantLock trayLock;

    public PongHandler(Nodo myNode, int ucp, NetworkTables nt){
        this.myNode = myNode;
        this.ucp = ucp;
        this.nt = nt;

        this.pongTray = new ArrayList<DatagramPacket>();
        this.trayLock = new ReentrantLock();
    }

    Runnable emptyTray = () -> {
        byte[] buf;
        Kryo kryo = new Kryo();

        this.trayLock.lock();
        if (this.pongTray.size() == 1){
            System.out.println("O TRAY SO TEM 1 DATAGRAMPACKET");
            //addNbr do que me mandou o Pong
            //Escolher vizinhos desse para ser meus vizinhos (Random?)
            // max 2
            //Ter em consideração o número de conexões ja existentes

            //for so vai ter 1 iteração
            for(DatagramPacket dp : this.pongTray){

                buf = dp.getData();
                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof Pong){
                    Pong pong = (Pong) header;

                    //Enviar addNBR ou NBRConfirmation?????????
                    System.out.println("ENVIAR NBR");

                    //Escolher no maximo 2 vizinhos para serem meus vizinhos (no minimo 1)
                    //Se o outro nodo não tiver vizinhos não se executa esta parte
                }
                else
                    System.out.println("ERRO NO PARSE DO DATAGRAMPACKET (PONGHANDLER)");
            }

        }
        else {
            System.out.println("O TRAY TEM 0 OU VARIOS DATAGRAMPACKETS");

            // Aceitar todos os pongs até  se chegar ao limite de vizinhos (SOFTCAP)
            // se não se chegar ao limite escolher vizinhos desses pontos para serem vizinhos

            //Verificar se o numero de pongs no tray é menor que o número de lugares restantes para vizinhos
                //Se FOR, enviar NBRConfirmations a todos
                //Escolher Vizinhos para preencher os lugares restantes (max 1 por pong, 3 Pongs => 3 vizinhos extra) Random??

                //Se for IGUAL enviar NBRConfirmations para esses

                //Se for MAIOR escolher ao calhas o número de lugares restantes
        }
        this.pongTray.clear();
        this.trayLock.unlock();
    };

    public void run(){
        try {
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(emptyTray, 0, 4, TimeUnit.SECONDS);

            DatagramSocket ucs = new DatagramSocket(this.ucp);
            byte[] buf;
            DatagramPacket pong;

            while (true){
                buf = new byte[1500];
                pong = new DatagramPacket(buf, 1500);
                ucs.receive(pong);

                this.trayLock.lock();
                this.pongTray.add(pong);
                this.trayLock.unlock();
                System.out.println("RECEBI O PONG");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
