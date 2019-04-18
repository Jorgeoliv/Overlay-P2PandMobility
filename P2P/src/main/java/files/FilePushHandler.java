package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.FilePull;
import mensagens.FilePush;
import mensagens.Header;
import network.IDGen;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class FilePushHandler {

    private int ucp_File;
    private IDGen idGen;

    private ArrayList<DatagramPacket> filePushTray;
    private ReentrantLock trayLock;
    private HashMap<String, Ficheiro> ficheiros;
    private HashMap<String, Boolean> ficheirosReady;

    private HashMap<String, ArrayList <FileReceiver>> fileReceivers;
    private HashMap<String, ArrayList <Thread>> fileReceiversThreads;

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
                if(this.ficheiros.containsKey(fp.hash) && !this.ficheirosReady.get(fp.hash)){
                    fich = this.ficheiros.get(fp.hash);
                    done = fich.addFileChunk(fp.fc);
                    this.ficheiros.put(fp.hash, fich);
                    if(done){
                        ficheirosReady.put(fp.hash, true);
                    }
                }
                else{
                    System.out.println("!?!?!??!?!?!?!??!?!??!?!??!?!??!!??!?!?!?!??!?!");
                }
            }
        }
    };


    public void sendFile(FilePull header) {
    }

    //setReceiveBufferSize

    public void getPorts(String id) {

    }
}
