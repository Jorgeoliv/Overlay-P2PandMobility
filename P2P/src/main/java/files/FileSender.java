package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FilePush;
import network.Nodo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class FileSender implements Runnable{

    private Nodo myNode;

    private Ficheiro f;

    private int pps;
    private int portToSend;
    private ArrayList<FileChunk> fcToSend;

    private String id;
    private String hash;

    private int startID;
    private int len;
    private int pPT;
    private ArrayList<Integer> mfc;

    private DatagramSocket ds;
    private InetAddress ipToSend;

    public FileSender(Ficheiro f, int portToSend, int startID, int len, int pPT, ArrayList<Integer> mfc, int pps, String id, String hash, Nodo myNode, String ip){
        this.myNode = myNode;

        this.f = f;

        this.pps = pps;
        this.portToSend = portToSend;
        this.fcToSend = new ArrayList<FileChunk>();

        this.id = id;
        this.hash = hash;

        this.startID = startID;
        this.len = len;
        this.pPT = pPT;
        this.mfc = mfc;

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

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);
        Kryo kryo = new Kryo();

        kryo.writeClassAndObject(output, fp);
        output.close();

        byte[] serializedfc = bStream.toByteArray();

        try{
            DatagramPacket packet = new DatagramPacket(serializedfc, serializedfc.length, this.ipToSend, this.portToSend);

            this.ds.send(packet);
        }
        catch (IOException e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run() {
        boolean flag = true;
        int counter = 0;
        int read = 0;

        ArrayList<FileChunk> tmp;

        if(this.mfc == null) {
            if(this.len < this.pPT) {
                this.fcToSend = this.f.getFileChunks(this.startID, this.len);
                this.startID += len;
                read += len;
            }
            else{
                this.fcToSend = this.f.getFileChunks(this.startID, this.pPT);
                this.startID = this.pPT;
                read += this.pPT;
            }
        }
        else {
            this.fcToSend = this.f.getMissingFileChunks(this.mfc);
        }

        int tam = fcToSend.size();

        while (this.fcToSend.size() > 0){
            try {
                sendFileChunk(this.fcToSend.get(0));
                counter++;
                Thread.sleep(1000 / this.pps);

                if ((this.mfc != null) && (tam < 1500)) {
                    sendFileChunk(this.fcToSend.get(0));
                    counter++;
                    Thread.sleep(1000 / this.pps);
                }

                this.fcToSend.remove(0);

                if((this.mfc == null) && flag &&  (this.fcToSend.size() < 500)){

                    if(read + this.len < this.pPT){
                        //System.out.println("VOU LER " + this.len + " | " + this.startID);
                        tmp = this.f.getFileChunks(startID, this.len);
                        this.startID += this.len;
                        read += this.len;
                    }
                    else {
                        //System.out.println("VOU LER " + (this.pPT - read) + " | " + this.startID);
                        tmp = this.f.getFileChunks(startID, this.pPT - read);
                        read += this.pPT - read;
                        this.startID += this.pPT - read;
                        flag = false;
                    }
                    this.fcToSend.addAll(tmp);
                    tmp = null;
                }
            }
             catch (Exception e){
                e.printStackTrace();
            }
        }
        //System.out.println("Enviei " + counter + " FILECHUNKS");
    }
}
