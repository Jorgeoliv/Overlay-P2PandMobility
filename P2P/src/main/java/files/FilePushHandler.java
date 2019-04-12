package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.FilePush;
import mensagens.Header;
import network.IDGen;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FilePushHandler implements Runnable {

    private int ucp_File;
    private IDGen idGen;

    private ArrayList<DatagramPacket> filePushTray;
    private ReentrantLock trayLock;
    private HashMap<String, Ficheiro> ficheiros;
    private HashMap<String, Boolean> ficheirosReady;


    public FilePushHandler(){}
    public FilePushHandler(int ucp_File, IDGen idGen){
        this.ucp_File = ucp_File;
        this.idGen = idGen;

        this.filePushTray = new ArrayList<DatagramPacket>();

    }

    private Runnable emptyFilePushTray = () -> {
        byte[] buf;
        Kryo kryo = new Kryo();
        ArrayList <DatagramPacket> aux;

        this.trayLock.lock();
        aux = (ArrayList<DatagramPacket>) this.filePushTray.clone();
        this.filePushTray.clear();
        this.trayLock.unlock();

        Ficheiro fich;
        boolean done;

        for(DatagramPacket dp : aux){

            buf = dp.getData();
            ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
            Input input = new Input(bStream);
            Header header = (Header) kryo.readClassAndObject(input);
            input.close();

            if(header instanceof FilePush) {
                FilePush fp = (FilePush) header;
                if(this.ficheiros.containsKey(fp.name) && !this.ficheirosReady.get(fp.name)){
                    fich = this.ficheiros.get(fp.name);
                    done = fich.addFileChunk(fp.fc);
                    this.ficheiros.put(fp.name, fich);
                    if(done){
                        ficheirosReady.put(fp.name, true);
                    }
                }
                else{
                    System.out.println("!?!?!??!?!?!?!??!?!??!?!??!?!??!!??!?!?!?!??!?!");
                }
            }
        }
    };

    //setReceiveBufferSize
    public void run() {

        try{
            DatagramSocket ds = new DatagramSocket(this.ucp_File);
            byte[] buf = new byte[1500];
            DatagramPacket file;
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

            ses.scheduleWithFixedDelay(emptyFilePushTray, 0, 5, TimeUnit.SECONDS);

            while(true){
                file = new DatagramPacket(buf, 1500);
                ds.receive(file);

                this.filePushTray.add(file);
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
