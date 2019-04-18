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

public class FileReceiver implements Runnable {

    public int port;
    private DatagramSocket ds;

    private ArrayList<FilePush> fp;
    private Kryo kryo;

    public FileReceiver(){
        this.port = -1;
        boolean b = true;
        this.kryo = new Kryo();

        Random rand = new Random();
        while(b) {
            try {
                this.port = rand.nextInt(40000) + 10000;
                ds = new DatagramSocket(this.port);
                b = false;
            }
            catch (Exception e) {
                e.printStackTrace();
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
                    this.fp.add((FilePush) header);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
