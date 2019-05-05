package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.FilePush;
import mensagens.Header;
import network.Nodo;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

public class FileReceiver implements Runnable {

    public int port;
    private DatagramSocket ds;

    private ArrayList<FileChunk> fc;

    private ReentrantLock lock;

    private boolean run = true;
    public TreeSet<Nodo> nodes;

    public FileReceiver(){
        this.port = -1;
        boolean b = true;

        this.lock = new ReentrantLock();
        this.fc = new ArrayList<FileChunk>();

        this.nodes = new TreeSet<Nodo>();

        Random rand = new Random();
        while(b) {
            try {
                this.port = rand.nextInt(40000) + 10000;
                ds = new DatagramSocket(this.port);
                ds.setReceiveBufferSize(3000000);
                b = false;
            }
            catch (Exception e) {
                System.out.println("ESCOLHI UMA PORTA JÁ EM USO => " + this.port);
            }
        }
    }

    public void kill(){
        this.run = false;
        this.ds.close();
    }

    public void run() {

        try{
            byte[] buffer;
            DatagramPacket dp;
            while (this.run){
                buffer = new byte[1500];
                dp = new DatagramPacket(buffer, buffer.length);

                this.ds.receive(dp);
                Kryo kryo = new Kryo();
                ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof FilePush) {
                    this.lock.lock();
                    nodes.add(header.origin);
                    this.fc.add(((FilePush) header).fc);
                    this.lock.unlock();
                }
            }
        }
        catch (SocketException se){
            //System.out.println("\t=>FILERECEIVER DATAGRAMSOCKET CLOSED");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<FileChunk> getFileChunks() {
        this.lock.lock();
        ArrayList<FileChunk> fcPointer = (ArrayList<FileChunk>) this.fc.clone();
        this.fc.clear();
        this.lock.unlock();

        return fcPointer;
    }

    public TreeSet<Nodo> getNodes(){
        TreeSet<Nodo> res;
        this.lock.lock();
        res = new TreeSet<Nodo>(this.nodes);
        this.nodes.clear();
        this.lock.unlock();

        return res;
    }
}
