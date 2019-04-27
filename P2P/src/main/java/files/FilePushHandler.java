package files;

import mensagens.FilePull;
import network.IDGen;
import network.Nodo;

import java.util.ArrayList;
import java.util.HashMap;
//setReceiveBufferSize

public class FilePushHandler implements Runnable{
    private Nodo myNode;

    private int numOfReceivers;
    private int timeoutTime = 10000;

    private int ucp_FilePushHandler;
    private IDGen idGen;

    private HashMap<String, Nodo> fileOwners;
    private HashMap<String, Ficheiro> ficheiros;

    private HashMap<String, ArrayList <FileReceiver>> fileReceivers;
    private HashMap<String, ArrayList <Thread>> fileReceiversThreads;
    private HashMap<String, Integer> timeouts;

    private FileTables ft;
    public FilePushHandler(int ucp_FilePushHandler, FileTables ft, IDGen idGen, Nodo myNode){
        this.myNode = myNode;

        this.numOfReceivers = 20;

        this.ucp_FilePushHandler = ucp_FilePushHandler;
        this.idGen = idGen;

        this.fileOwners = new HashMap<String, Nodo>();
        this.ficheiros = new HashMap<String, Ficheiro>();

        this.fileReceivers = new HashMap<String, ArrayList<FileReceiver>>();
        this.fileReceiversThreads = new HashMap<String, ArrayList<Thread>>();
        this.timeouts = new HashMap<String, Integer>();

        this.ft = ft;
    }

    public void sendFile(FilePull fp) {
        String id = this.idGen.getID();

        Ficheiro f = this.ft.getFicheiro(fp.fi.name);
        System.out.println("NUMERO DE FILECHUNKS " + f.getNumberOfChunks() + "\n\n");

        FileChunk[] fc = f.getFileChunks();
        ArrayList<ArrayList<FileChunk>> fileChunks = new ArrayList<ArrayList<FileChunk>>();

        FileSender fsPointer;
        Thread t;

        int i, nfc = fp.ports_packetPerSecond.size();

        for(i = 0; i < nfc; i++)
            fileChunks.add(new ArrayList<FileChunk>());

        for(i = 0; i < f.getNumberOfChunks(); i++)
            fileChunks.get(i%nfc).add(fc[i]);

        i = 0;
        for(int port : fp.ports_packetPerSecond.keySet()){
            fsPointer = new FileSender(port, fileChunks.get(i++),fp.ports_packetPerSecond.get(port),id, fp.fi.hash,this.myNode, fp.origin.ip, this.ucp_FilePushHandler);
            t = new Thread(fsPointer);
            t.start();
        }
    }

    public void registerFile(FileInfo fi, Nodo node){
        Ficheiro f = new Ficheiro(fi.numOfFileChunks, this.myNode.id, fi.name);
        this.ficheiros.put(fi.hash, f);
        this.fileOwners.put(fi.hash, node);
        this.timeouts.put(fi.hash, 0);

    }

    public ArrayList<Integer> getPorts(String id) {

        ArrayList<FileReceiver> fR = new ArrayList<FileReceiver>();
        ArrayList<Integer> fRPorts = new ArrayList<Integer>();

        int i = 0;
        FileReceiver pointer;
        while(i < this.numOfReceivers) {
            pointer = new FileReceiver();
            fR.add(pointer);
            fRPorts.add(pointer.port);
            i++;
        }

        this.fileReceivers.put(id, fR);
        return fRPorts;
    }

    public void startReceivers (String id){
        ArrayList<FileReceiver> aux = this.fileReceivers.get(id);
        ArrayList<Thread> threads = new ArrayList<Thread>();
        Thread t;

        for(FileReceiver fr: aux){
            t = new Thread(fr);
            t.start();
            threads.add(t);
        }

        this.fileReceiversThreads.put(id, threads);
    }

    public void clean(String h){
        for(Thread t : this.fileReceiversThreads.get(h))
            t.stop();

        for(FileReceiver fr : this.fileReceivers.get(h))
            fr = null;

        this.fileReceivers.remove(h);
        this.fileReceiversThreads.remove(h);
        this.timeouts.remove(h);
        this.ficheiros.remove(h);
    }

    private void sendTimeoutPacket(String h) {
    }

    public void run(){
        Ficheiro filePointer;
        ArrayList<FileReceiver> fRPointer;
        ArrayList<FileChunk> fCPointer;
        ArrayList<String> toRemove = new ArrayList<String>();

        int packets = 0, to, i, tam;

        while(true){
            try {
                for (String h : this.ficheiros.keySet()) {
                    filePointer = this.ficheiros.get(h);
                    fRPointer = this.fileReceivers.get(h);

                    for (FileReceiver fr : fRPointer) {
                        fCPointer = fr.getFileChunks();
                        if (fCPointer.size() > 0)
                            filePointer.addFileChunks(fCPointer);
                        packets += fCPointer.size();
                    }

                    if (packets == 0 && !filePointer.getFull()) {
                        to = this.timeouts.get(h) + 1;
                        this.timeouts.put(h, to);
                        System.out.println("TIMEOUT");
                    }
                    else {
                        if (filePointer.getFull()) {
                            System.out.println("FICHEIRO COMPLETO!!!!!!!!!!!!!!!!!!\n");
                            toRemove.add(h);
                        }
                        else
                            this.timeouts.put(h, 0);
                    }

                    if(this.timeouts.containsKey(h)) {
                        to = this.timeouts.get(h);
                        if (to == 5) {
                            System.out.println("ENVIAR MENSAGEM DE TIMEOUT");
                            sendTimeoutPacket(h);
                        } else {
                            if (to >= 10) {
                                System.out.println("TRANSFERENCIA FALHADA");
                                clean(h);
                            }
                        }
                    }

                    packets = 0;
                }
                tam = toRemove.size();
                for(i = 0; i < tam; i++)
                    clean(toRemove.get(i));

                toRemove.clear();
                Thread.sleep(this.timeoutTime);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
