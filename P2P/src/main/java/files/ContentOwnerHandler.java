package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.ContentOwner;
import mensagens.FilePush;
import mensagens.Header;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentOwnerHandler implements Runnable{
    private FileHandler fileHandler;
    private int ucp_ContentOwnerHandler;

    public ContentOwnerHandler(FileHandler fileHandler,int ucp_ContentOwnerHandler){
        this.fileHandler = fileHandler;
        this.ucp_ContentOwnerHandler = ucp_ContentOwnerHandler;
    }

    private void processCO(DatagramPacket dp){
        byte[] buf;
        Kryo kryo = new Kryo();
        buf = dp.getData();
        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof ContentOwner) {
            ContentOwner co = (ContentOwner) header;

            this.fileHandler.registerPair(co.cdRequestID, co.origin, co.fileInfo);

        }
    }

    public void run() {
        try{


            byte[] buf;
            DatagramPacket contentOwner;

            DatagramSocket ds = new DatagramSocket(this.ucp_ContentOwnerHandler);

            while(true){
                buf = new byte[1500];
                contentOwner = new DatagramPacket(buf, buf.length);

                ds.receive(contentOwner);

                processCO(contentOwner);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
