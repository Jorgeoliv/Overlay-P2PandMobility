package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.FilePush;
import mensagens.Header;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class FileReceiver implements Runnable {

    public int port;
    private DatagramSocket ds;

    private ArrayList<FileChunk> fc;
    private Kryo kryo;

    private ReentrantLock lock;

    public FileReceiver(){
        this.port = -1;
        boolean b = true;
        this.kryo = new Kryo();

        this.lock = new ReentrantLock();
        this.fc = new ArrayList<FileChunk>();

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

    public void run() {

        try{
            byte[] buffer;
            byte[] buf;
            DatagramPacket dp;

            while (true){
                buffer = new byte[1500];
                dp = new DatagramPacket(buffer, buffer.length);

                this.ds.receive(dp);

                buf = dp.getData();
                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(header instanceof FilePush) {
                    this.lock.lock();
                    this.fc.add(((FilePush) header).fc);
                    this.lock.unlock();
                }
            }
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
}
