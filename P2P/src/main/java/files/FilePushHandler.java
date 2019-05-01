package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FilePull;
import network.IDGen;
import network.Nodo;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class FilePushHandler implements Runnable{

    private boolean run = true;

    private Nodo myNode;

    private int timeoutTime = 2500;

    private int ucp_FilePushHandler;
    private int ucp_FilePullHandler;
    private IDGen idGen;

    private HashMap<String, Nodo> fileOwners;
    private HashMap<String, Ficheiro> ficheiros;
    private HashMap<String, FileInfo> fileInfos;

    private HashMap<String, ArrayList <FileReceiver>> fileReceivers;
    private HashMap<String, ArrayList <Thread>> fileReceiversThreads;
    private HashMap<String, Integer> timeouts;

    private int TimeOutpps = 100;

    private FileTables ft;

    public FilePushHandler(int ucp_FilePushHandler, int ucp_FilePullHandler, FileTables ft, IDGen idGen, Nodo myNode){
        this.myNode = myNode;

        this.ucp_FilePushHandler = ucp_FilePushHandler;
        this.ucp_FilePullHandler = ucp_FilePullHandler;
        this.idGen = idGen;

        this.fileOwners = new HashMap<String, Nodo>();
        this.ficheiros = new HashMap<String, Ficheiro>();
        this.fileInfos = new HashMap<String, FileInfo>();


        this.fileReceivers = new HashMap<String, ArrayList<FileReceiver>>();
        this.fileReceiversThreads = new HashMap<String, ArrayList<Thread>>();
        this.timeouts = new HashMap<String, Integer>();

        this.ft = ft;
    }

    public void sendFile(FilePull fp) {

        String id = this.idGen.getID("");

        Ficheiro f = this.ft.getFicheiro(fp.fi.name);
        System.out.println("NUMERO DE FILECHUNKS " + f.getNumberOfChunks() + "\n\n");

        int numOfFileChunks;
        FileChunk[] fc = null;
        ArrayList<FileChunk> fcArray = null;
        if(fp.missingFileChunks == null) {
            fc = f.getFileChunks();
            System.out.println("TIVE QUE IR BUSCAR OS FILECHUNKS TODOS");
            numOfFileChunks = f.getNumberOfChunks();
        }
        else {
            ArrayList<Integer> aux = new ArrayList<Integer>();

            for(int s : fp.missingFileChunks)
                aux.add(s);

            fcArray = f.getMissingFileChunks(aux);
            System.out.println("\t\tTIVE QUE IR BUSCAR ALGUNS DOS FILECHUNKS");
            numOfFileChunks = aux.size();
        }
        ArrayList<ArrayList<FileChunk>> fileChunks = new ArrayList<ArrayList<FileChunk>>();

        FileSender fsPointer;
        Thread t;
        //verificar o numero de threads

        int i, nfcReceivers = fp.portas.length;

        if(fp.missingFileChunks == null) {
            //mais que 1 thread pois são muitos pacotes
            for (i = 0; i < nfcReceivers; i++)
                fileChunks.add(new ArrayList<FileChunk>());

            for (i = 0; i < numOfFileChunks; i++)
                fileChunks.get(i % nfcReceivers).add(fc[i]);

            i = 0;
            for (int port : fp.portas) {
                fsPointer = new FileSender(port, fileChunks.get(i++), fp.pps, id, fp.fi.hash, this.myNode, fp.origin.ip);
                t = new Thread(fsPointer);
                t.start();
            }
        }
        else{
            //menos threads porque são pacotes que estao a ser pedidos novamente (max 250 para ja)
            Random rand = new Random();
            int pos = rand.nextInt(fp.portas.length);
            int porta = fp.portas[pos];

            fsPointer = new FileSender(porta, fcArray, fp.pps, id, fp.fi.hash, this.myNode, fp.origin.ip);
            t = new Thread(fsPointer);
            t.start();
        }
    }

    public void registerFile(FileInfo fi, Nodo node){
        Ficheiro f = new Ficheiro(fi.numOfFileChunks, this.myNode.id, fi.name);
        this.ficheiros.put(fi.hash, f);
        this.fileInfos.put(fi.hash, fi);
        this.fileOwners.put(fi.hash, node);
        this.timeouts.put(fi.hash, 0);
    }

    public ArrayList<Integer> getPorts(String id, int numOfFileChunks) {

        ArrayList<FileReceiver> fR = new ArrayList<FileReceiver>();
        ArrayList<Integer> fRPorts = new ArrayList<Integer>();

        int numOfReceivers = numOfFileChunks / 3000;

        if(numOfReceivers == 0)
            numOfReceivers = 1;
        if(numOfReceivers >= 30)
            numOfReceivers = 30;

        int i = 0;
        FileReceiver pointer;
        while(i < numOfReceivers) {
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
        /*for(Thread t : this.fileReceiversThreads.get(h))
            t.interrupt();*/

        for(FileReceiver fr : this.fileReceivers.get(h))
            fr.kill();


        this.fileReceivers.remove(h);
        this.fileReceiversThreads.remove(h);
        this.timeouts.remove(h);
        this.ficheiros.remove(h);
        this.fileInfos.remove(h);
    }

    private void sendTimeoutPackets(String h) {
        //INFO RELATIVA AO FICHEIRO
        Ficheiro f = this.ficheiros.get(h);
        ArrayList<Integer> mfc = f.getMissingFileChunks();
        int totalNumOfFileChunks = f.getNumberOfChunks();

        //PERCENTAGEM DE PACOTES QUE TENHO
        int percentage = (totalNumOfFileChunks - mfc.size()) * 100 / totalNumOfFileChunks;
        System.out.println("TENHO ESTA PERCENTAGEM  =======> " + percentage + "%");

        //OBTER AS PORTAS QUE ESTAO A RECEBER O FICHEIRO
        int[] portas = new int[this.fileReceivers.get(h).size()];
        int j = 0;
        for (FileReceiver fr : this.fileReceivers.get(h)) {
            portas[j++] = fr.port;
        }

        if(percentage < 40){
            //CASO NAO TENHA PELO MENOS ESTA PERCENTAGEM IREI PEDIR O FICHEIRO TODO DE NOVO
            FilePull fp = new FilePull(this.idGen.getID(""), this.myNode, this.fileInfos.get(h), portas, this.TimeOutpps, null);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            Output output = new Output(bStream);

            Kryo kryo = new Kryo();
            kryo.writeClassAndObject(output, fp);
            output.close();

            byte[] serializedTimeoutPacket = bStream.toByteArray();

            try {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(serializedTimeoutPacket, serializedTimeoutPacket.length, InetAddress.getByName(this.fileOwners.get(h).ip), this.ucp_FilePullHandler);

                ds.send(packet);
                Thread.sleep(50);
                ds.send(packet);
                Thread.sleep(50);
                ds.send(packet);
                System.out.println("ENVIEI O TIMEOUTPACKET " + "\n\t" + this.fileOwners.get(h).ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + this.fileInfos.get(h).hash + "\n\t" + "mfc.size = null");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {

            int[] mfcGroup;
            int mfcGroupSize = 425;

            while (!mfc.isEmpty()) {

                int mfcSize = mfc.size();
                if (mfcSize > mfcGroupSize)
                    mfcGroup = new int[mfcGroupSize];
                else
                    mfcGroup = new int[mfcSize];

                for (int i = 0; (i < mfcGroupSize) && !mfc.isEmpty(); i++) {
                    mfcGroup[i] = mfc.get(0);
                    mfc.remove(0);
                }

                System.out.println("FALTAM " + this.ficheiros.get(h).getNumberOfMissingFileChunks() + " de " + this.ficheiros.get(h).getNumberOfChunks());

                FilePull fp = new FilePull(this.idGen.getID(""), this.myNode, this.fileInfos.get(h), portas, this.TimeOutpps, mfcGroup);

                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);

                Kryo kryo = new Kryo();
                kryo.writeClassAndObject(output, fp);
                output.close();

                byte[] serializedTimeoutPacket = bStream.toByteArray();

                try {

                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(serializedTimeoutPacket, serializedTimeoutPacket.length, InetAddress.getByName(this.fileOwners.get(h).ip), this.ucp_FilePullHandler);

                    ds.send(packet);
                    Thread.sleep(50);
                    ds.send(packet);
                    Thread.sleep(50);
                    ds.send(packet);

                    System.out.println("ENVIEI O TIMEOUTPACKET " + "\n\t" + this.fileOwners.get(h).ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + this.fileInfos.get(h).hash + "\n\t" + "mfc.size = " + mfcGroup.length + " => " + serializedTimeoutPacket.length + " bytes");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void kill(){
        this.run = false;
    }

    public void run(){
        Ficheiro filePointer;
        ArrayList<FileReceiver> fRPointer;
        ArrayList<FileChunk> fCPointer;
        ArrayList<String> toRemove = new ArrayList<String>();

        int packets = 0, to, i, tam;

        while(this.run){
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
                        System.out.println("TIMEOUT NA TRANSFERÊNCIA DE " + filePointer.getFileName() + "\n\tPackets: " + packets + "\n\tFULL? " + filePointer.getFull());
                    }
                    else {
                        if (filePointer.getFull()) {
                            System.out.println("TRANSFERÊNCIA DE " + filePointer.getFileName() + " CONCLUIDA\n");
                            toRemove.add(h);
                        }
                        else
                            this.timeouts.put(h, 0);
                    }

                    if(this.timeouts.containsKey(h) && !toRemove.contains(h)) {
                        to = this.timeouts.get(h);
                        if (to >= 3) {
                            System.out.println("ENVIAR MENSAGENS DE TIMEOUT");
                            sendTimeoutPackets(h);
                            System.out.println("MENSAGENS DE TIMEOUT ENVIADAS");

                        }

                        if (to >= 10) {
                            System.out.println("TRANSFERÊNCIA FALHADA");
                            toRemove.add(h);
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
