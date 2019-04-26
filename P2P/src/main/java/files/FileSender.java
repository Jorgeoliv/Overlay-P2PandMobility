package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FilePush;
import network.Nodo;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class FileSender implements Runnable{

    private Nodo myNode;

    private int timeToSleep;
    private int portToSend;
    private ArrayList<FileChunk> fcToSend;

    private String id;
    private String hash;

    private DatagramSocket ds;
    private InetAddress ipToSend;
    private int ucp_FilePushHandler;

    public FileSender(int portToSend, ArrayList<FileChunk> fc, int timeToSleep, String id, String hash, Nodo myNode, String ip, int ucp_FilePushHandler){
        this.timeToSleep = timeToSleep;

        this.portToSend = portToSend;
        this.fcToSend = fc;
        this.id = id;
        this.hash = hash;
        this.myNode = myNode;
        this.ucp_FilePushHandler = ucp_FilePushHandler;

        try {
            this.ipToSend = InetAddress.getByName(ip);
            this.ds = new DatagramSocket();
            this.ds.setSendBufferSize(3000000);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendFileChunk(FileChunk fileChunk) {
        FilePush fp = new FilePush(this.id, this.myNode, fileChunk, this.hash);
        System.out.println("1");

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();

        kryo.writeClassAndObject(output, fp);
        output.close();

        byte[] serializedPing = bStream.toByteArray();
        try{
            DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, this.ipToSend, this.portToSend);
            System.out.println("2");

            this.ds.send(packet);
            System.out.println("Enviei");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run() {
        int i;
        try {
            for (i = 0; i < this.fcToSend.size(); i++) {
                sendFileChunk(this.fcToSend.get(i));

                Thread.sleep(this.timeToSleep);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
