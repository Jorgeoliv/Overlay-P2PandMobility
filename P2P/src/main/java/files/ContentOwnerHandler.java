package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.ContentOwner;
import mensagens.Header;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentOwnerHandler implements Runnable{

    private boolean run = true;

    private FileHandler fileHandler;
    private int ucp_ContentOwnerHandler;

    private ScheduledExecutorService ses;

    private ArrayList<String> ids = new ArrayList<String>();
    private DatagramSocket ds;

    public ContentOwnerHandler(FileHandler fileHandler,int ucp_ContentOwnerHandler){
        this.fileHandler = fileHandler;
        this.ucp_ContentOwnerHandler = ucp_ContentOwnerHandler;
        this.ses = Executors.newSingleThreadScheduledExecutor();

        try {
            this.ds = new DatagramSocket(this.ucp_ContentOwnerHandler);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    private void processCO(ContentOwner co){

        //System.out.println("RECEBI UM CONTENT OWNER DE " + co.fileInfo.name);

        this.fileHandler.registerPair(co.cdRequestID, co.origin, co.fileInfo);

    }

    private Runnable removeID = () ->{
        if(!this.ids.isEmpty())
            this.ids.remove(0);
    };

    public void kill(){
        this.run = false;
        this.ses.shutdownNow();
        this.ds.close();
    }

    public void run() {
        try{
            byte[] buf;
            Kryo kryo = new Kryo();

            DatagramPacket dp;
            
            while(this.run){
                buf = new byte[1500];
                dp = new DatagramPacket(buf, buf.length);

                this.ds.receive(dp);

                ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
                Input input = new Input(bStream);
                Header header = (Header) kryo.readClassAndObject(input);
                input.close();

                if(!this.ids.contains(header.requestID)) {

                    this.ids.add(header.requestID);
                    this.ses.schedule(removeID, 60, TimeUnit.SECONDS);

                    if (header instanceof ContentOwner) {
                        ContentOwner contentOwner = (ContentOwner) header;
                        processCO(contentOwner);
                    }
                }
            }
        }
        catch (SocketException se){
            //System.out.println("\t=>CONTENTOWNERHANDLER DATAGRAMSOCKET CLOSED");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
