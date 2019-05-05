package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FCIDStruct;
import mensagens.FilePull;
import network.IDGen;
import network.Nodo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class FilePushHandler implements Runnable{

    private boolean run = true;

    private Nodo myNode;

    private int timeoutTime = 2500;

    private int ucp_FilePushHandler;
    private int ucp_FilePullHandler;
    private IDGen idGen;

    private FilePullHandler fph;

    private HashMap<String, ArrayList<Nodo>> fileOwners;
    private HashMap<String, Ficheiro> ficheiros;
    private HashMap<String, FileInfo> fileInfos;

    private HashMap<String, ArrayList <FileReceiver>> fileReceivers;
    private HashMap<String, ArrayList <Thread>> fileReceiversThreads;
    private HashMap<String, Integer> timeouts;
    private HashMap<String, HashMap<String, Integer>> nodeTimeOut;

    private int TimeOutpps = 200;
    private int fileChunksToRead = 1000;

    private FileTables ft;
    private final FileHandler fh;

    public FilePushHandler(int ucp_FilePushHandler, int ucp_FilePullHandler, FileTables ft, FileHandler fh, IDGen idGen, Nodo myNode){
        this.myNode = myNode;

        this.ucp_FilePushHandler = ucp_FilePushHandler;
        this.ucp_FilePullHandler = ucp_FilePullHandler;
        this.idGen = idGen;

        this.fph = fph;

        this.fileOwners = new HashMap<String, ArrayList<Nodo>>();
        this.ficheiros = new HashMap<String, Ficheiro>();
        this.fileInfos = new HashMap<String, FileInfo>();


        this.fileReceivers = new HashMap<String, ArrayList<FileReceiver>>();
        this.fileReceiversThreads = new HashMap<String, ArrayList<Thread>>();
        this.timeouts = new HashMap<String, Integer>();
        this.nodeTimeOut = new HashMap<String, HashMap<String, Integer>>();

        this.ft = ft;
        this.fh = fh;
    }

    public void setFPLH(FilePullHandler filePullHandler) {
        this.fph = filePullHandler;
    }

    private ArrayList<Integer> getIDsFromFCIDStruct(FCIDStruct fcIDS){
        ArrayList<Integer> res = new ArrayList<Integer>();

        int currentID = fcIDS.referenceID;
        byte[] toAdd = fcIDS.toAdd;
        byte[] inc = fcIDS.inc;

        int maxValue = Byte.MAX_VALUE - Byte.MIN_VALUE;

        res.add(currentID);
        int i = 0;
        int toAddAux = 0;
        int pointer;

        for (byte ta : toAdd){
            toAddAux = (int)ta - Byte.MIN_VALUE;
            //System.out.println("THIS HERE => " + toAddAux);

            for(pointer = 0; pointer < 8 && i < inc.length; pointer ++) {
                if (toAddAux % 2 == 0) {
                    //System.out.println("TO ADD ERA PAR");
                    currentID += maxValue;
                }
                else {
                    //System.out.println("TO ADD ERA IMPAR |" + "inc[" + i + "] = " + inc[i]);
                    currentID += ((int) inc[i++]) - Byte.MIN_VALUE;
                    res.add(currentID);
                }
                toAddAux /=2;
            }
/*            if(pointer == 8)
                System.out.println("POINTER");
            else
                System.out.println("INC SIZE | POINTER => " + pointer);*/

        }


        //System.out.println(res);
        return res;
    }

    public void sendFile(FilePull fp) {

        String id = this.idGen.getID("");

        Ficheiro f = this.ft.getFicheiro(fp.fi.name);
        //System.out.println("NUMERO DE FILECHUNKS " + f.getNumberOfChunks() + "\n\n");

        FileSender fsPointer;
        Thread t;

        if(fp.missingFileChunks == null) {
            int nfcReceivers = fp.portas.length;
            if(fp.len < 0) {
                int packetsPerThread = (int) Math.ceil(fp.fi.numOfFileChunks / nfcReceivers);

                int startPointer = 0, portPointer = 0;

                while (startPointer < nfcReceivers) {

                    fsPointer = new FileSender(f, fp.portas[portPointer++], startPointer * packetsPerThread, this.fileChunksToRead, packetsPerThread, null, fp.pps, id, fp.fi.hash, this.myNode, fp.origin.ip);
                    t = new Thread(fsPointer);
                    t.start();
                    startPointer++;
                }
            }
            else{
                Random rand = new Random();

                int numOfThreads = (int) Math.ceil((fp.len / 4000) + 0.5);
                ArrayList<Integer> ports = new ArrayList<Integer>();

                for(int a : fp.portas)
                    ports.add(a);

                if(numOfThreads == 0) {
                    numOfThreads = 1;
                }
                if(numOfThreads > fp.portas.length) {
                    numOfThreads = fp.portas.length;
                }
                else{
                    int pos;
                    for(int i = numOfThreads; i < fp.portas.length; i++){
                        pos = rand.nextInt(ports.size());
                        ports.remove(pos);
                    }
                }

                int packetsPerThread = fp.len / numOfThreads;

                int startPointer = 0, portPointer = 0, startingID = fp.startingID;
                int read = 0;

                while(startPointer < numOfThreads){
                    if((startPointer) * packetsPerThread > fp.len) {
                        read = fp.len - (startPointer * packetsPerThread);
                    }
                    else {
                        read = packetsPerThread;
                    }
                    System.out.println("READ => " + read + " | " + packetsPerThread);
                    fsPointer = new FileSender(f, ports.get(startPointer), startingID + startPointer * packetsPerThread, this.fileChunksToRead, read, null, fp.pps, id, fp.fi.hash, this.myNode, fp.origin.ip);
                    t = new Thread(fsPointer);
                    t.start();
                    startPointer++;
                }

            }
        }
        else{
            //menos threads porque são pacotes que estao a ser pedidos novamente (max 250 para ja)
            ArrayList<Integer> aux = getIDsFromFCIDStruct(fp.missingFileChunks);
            Random rand = new Random();
            int pos = rand.nextInt(fp.portas.length);
            int porta = fp.portas[pos];

            fsPointer = new FileSender(f, porta, 0, 0, 0, aux, fp.pps, id, fp.fi.hash, this.myNode, fp.origin.ip);
            t = new Thread(fsPointer);
            t.start();
        }
    }

    public void registerFile(FileInfo fi, ArrayList<Nodo> nodes){
        Ficheiro f = new Ficheiro(fi.numOfFileChunks, this.myNode.id, fi.name);
        this.ficheiros.put(fi.hash, f);
        this.fileInfos.put(fi.hash, fi);
        this.fileOwners.put(fi.hash, nodes);
        this.timeouts.put(fi.hash, 0);

        HashMap<String, Integer> aux = new HashMap<String, Integer>();

        for(Nodo n : nodes)
            aux.put(n.id, 0);

        this.nodeTimeOut.put(fi.hash, aux);
    }

    public ArrayList<Integer> getPorts(String id, int numOfFileChunks) {

        ArrayList<FileReceiver> fR = new ArrayList<FileReceiver>();
        ArrayList<Integer> fRPorts = new ArrayList<Integer>();

        int numOfReceivers = numOfFileChunks / 4000;

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

        for(FileReceiver fr : this.fileReceivers.get(h))
            fr.kill();


        this.fileReceivers.remove(h);
        this.fileReceiversThreads.remove(h);
        this.timeouts.remove(h);
        this.fileOwners.remove(h);
        this.ficheiros.remove(h);
        this.fileInfos.remove(h);
        this.fph.removePullRequest(h);
    }

    private FCIDStruct getFCIDStruct(ArrayList<Integer> mfcGroup){

        //System.out.println(mfcGroup);

        int referenceID = mfcGroup.get(0);
        int currentID = referenceID;
        int counter = 0;
        ArrayList<Integer> toAdd = new ArrayList<Integer>();
        int toAddAux = 0;
        int pointer = 0;

        ArrayList<Byte> inc = new ArrayList<Byte>();

        mfcGroup.remove(0);

        int dif = Byte.MAX_VALUE - Byte.MIN_VALUE;

        for(int id : mfcGroup){
            while(currentID + dif < id){
                if(pointer == 8) {
                    toAdd.add(toAddAux);
                    //System.out.println("TO ADD => (int)" + toAddAux + " (byte)" + (byte)(toAddAux + Byte.MIN_VALUE));
                    pointer = 0;
                    toAddAux = 0;
                }
                pointer++;
                toAddAux *= 2;
                currentID +=  dif;
            }
            if(pointer == 8) {
                toAdd.add(toAddAux);
                //System.out.println("TO ADD => (int)" + toAddAux + " (byte)" + (byte)(toAddAux + Byte.MIN_VALUE));
                pointer = 0;
                toAddAux = 0;
            }
            pointer++;
            toAddAux *= 2;
            toAddAux++;
            //System.out.println("INC TO ADD => " + (byte)(id - currentID + Byte.MIN_VALUE));
            inc.add((byte)(id - currentID + Byte.MIN_VALUE));
            currentID = id;
            counter++;
        }
        if(pointer == 8) {
            toAdd.add(toAddAux);
            //System.out.println("TO ADD => (int)" + toAddAux + "\n\tpointer => " + pointer);
        }

        ArrayList<Byte> invertedToAdd = new ArrayList<Byte>();
        int invertedToAddAux, aux=-10000000;
        byte auxb = 0;

        for(int b : toAdd){
            //System.out.println("ANTES => " + b);
            invertedToAddAux = 0;

            for (int i = 0; i < 8; i++) {
                invertedToAddAux *= 2;
                if (b % 2 == 1)
                    invertedToAddAux++;
                 b /=2;
            }

            //System.out.println("DEPOIS => " + invertedToAddAux);
            aux = invertedToAddAux;
            auxb = (byte)(invertedToAddAux - Byte.MIN_VALUE);
            invertedToAdd.add((byte)(invertedToAddAux - Byte.MIN_VALUE));
        }

        if(pointer < 8){
            //System.out.println("TO ADD => (int)" + toAddAux + "\n\tpointer => " + pointer);
            invertedToAddAux = 0;

            for (int i = 0; i < pointer; i++) {
                invertedToAddAux *= 2;
                if (toAddAux % 2 == 1)
                    invertedToAddAux++;
                toAddAux /=2;

            }
            aux = invertedToAddAux;
            auxb = (byte)(invertedToAddAux - Byte.MIN_VALUE);
            invertedToAdd.add((byte)(invertedToAddAux - Byte.MIN_VALUE));
        }

        //System.out.println("INVERTED => " + aux + "\nIN BYTE => " + auxb);
        //System.out.println("\tINC SIZE => " + inc.size() + "\n\tTO ADD SIZE => " + toAdd.size() + "\n\tINVERTED TO ADO SIZ => " + invertedToAdd.size() + "\n\tPOINTER => " + pointer);
        byte[] toAddArray = new byte[invertedToAdd.size()];
        for(int i = 0; i < invertedToAdd.size(); i++)
            toAddArray [i] = invertedToAdd.get(i);

        byte[] incArray = new byte[inc.size()];
        for(int i = 0; i < inc.size(); i++) {
            //System.out.println("inc[" + i + "] = " + inc.get(i));
            incArray[i] = inc.get(i);
        }

        FCIDStruct structPointer = new FCIDStruct(referenceID, toAddArray, incArray);

        //System.out.println("TENHO " + counter +1 + " IDS");
        return structPointer;
    }

    private void sendTimeoutPackets(String h) {
        //INFO RELATIVA AO FICHEIRO
        Ficheiro f = this.ficheiros.get(h);
        ArrayList<Integer> mfc = f.getMissingFileChunks();
        int totalNumOfFileChunks = f.getNumberOfChunks();

        //PERCENTAGEM DE PACOTES QUE TENHO
        int percentage = (totalNumOfFileChunks - mfc.size()) * 100 / totalNumOfFileChunks;
        System.out.println("Transferência de " + h + " em curso (" + percentage + "%)");

        //OBTER AS PORTAS QUE ESTAO A RECEBER O FICHEIRO
        int[] portas = new int[this.fileReceivers.get(h).size()];
        int j = 0;
        for (FileReceiver fr : this.fileReceivers.get(h)) {
            portas[j++] = fr.port;
        }

        if(percentage < 40){
            if(this.fileOwners.get(h).size() == 1) {
                //CASO NAO TENHA PELO MENOS ESTA PERCENTAGEM IREI PEDIR O FICHEIRO TODO DE NOVO
                FilePull fp = new FilePull(this.idGen.getID(""), this.myNode, this.fileInfos.get(h), portas, this.TimeOutpps, null);

                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);

                Kryo kryo = new Kryo();
                kryo.writeClassAndObject(output, fp);
                output.close();

                byte[] serializedTimeoutPacket = bStream.toByteArray();

                boolean twoPackets = false;
                int tries = 0;
                int failures = 0;

                while (!twoPackets && tries < 2 && failures < 10) {
                    try {
                        DatagramSocket ds = new DatagramSocket();
                        DatagramPacket packet = new DatagramPacket(serializedTimeoutPacket, serializedTimeoutPacket.length, InetAddress.getByName(this.fileOwners.get(h).get(0).ip), this.ucp_FilePullHandler);

                        ds.send(packet);
                        tries++;
                        Thread.sleep(50);
                        ds.send(packet);
                        twoPackets = true;
                        Thread.sleep(50);
                        ds.send(packet);
                        //System.out.println("ENVIEI O TIMEOUTPACKET " + "\n\t" + this.fileOwners.get(h).ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + this.fileInfos.get(h).hash + "\n\t" + "mfc.size = null");
                    } catch (IOException e) {
                        System.out.println("\t=======>Network is unreachable");
                        failures++;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            //ex.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }
            else{
                ArrayList<String> nodesToSend = new ArrayList<String>();
                HashMap<String, Integer> ndTO = this.nodeTimeOut.get(h);
                for(String id : ndTO.keySet()){
                    if(ndTO.get(id) < 5){
                        nodesToSend.add(id);
                    }
                }
                this.fph.sendToMultipleNodes(h, portas, nodesToSend);
            }
        }
        else {
            ArrayList<Integer> mfcGroup = new ArrayList<Integer>();
            int mfcGroupSize = 1000;
            ArrayList<Nodo> owners = this.fileOwners.get(h);
            int ownerIterator = 0;

            while (!mfc.isEmpty()) {

                //Adição dos ids dos filechunks à estrutura de dados a ser enviada
                for (int i = 0; (i < mfcGroupSize) && !mfc.isEmpty(); i++) {
                    mfcGroup.add(mfc.get(0));
                    mfc.remove(0);
                }

                FCIDStruct fcIDS = getFCIDStruct(mfcGroup);

                //System.out.println("FALTAM " + this.ficheiros.get(h).getNumberOfMissingFileChunks() + " de " + this.ficheiros.get(h).getNumberOfChunks());

                FilePull fp = new FilePull(this.idGen.getID(""), this.myNode, this.fileInfos.get(h), portas, this.TimeOutpps, fcIDS);


                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                Output output = new Output(bStream);

                Kryo kryo = new Kryo();
                kryo.writeClassAndObject(output, fp);
                output.close();

                byte[] serializedTimeoutPacket = bStream.toByteArray();

                boolean twoPackets = false;
                int tries = 0;
                int failures = 0;

                while(!twoPackets && tries < 2 && failures < 10) {
                    try {

                        DatagramSocket ds = new DatagramSocket();
                        DatagramPacket packet;
                        if(owners.size() == 1)
                            packet = new DatagramPacket(serializedTimeoutPacket, serializedTimeoutPacket.length, InetAddress.getByName(owners.get(0).ip), this.ucp_FilePullHandler);
                        else
                            packet = new DatagramPacket(serializedTimeoutPacket, serializedTimeoutPacket.length, InetAddress.getByName(owners.get(ownerIterator++).ip), this.ucp_FilePullHandler);

                        if(ownerIterator >= owners.size())
                            ownerIterator = 0;

                        ds.send(packet);
                        tries++;
                        Thread.sleep(50);
                        ds.send(packet);
                        twoPackets = true;
                        Thread.sleep(50);
                        ds.send(packet);

                        //System.out.println("ENVIEI O TIMEOUTPACKET " + "\n\t" + this.fileOwners.get(h).ip + "\n\t" + this.ucp_FilePullHandler + "\n\t" + this.fileInfos.get(h).hash + "\n\t" + "mfc.size = " + mfcGroup.size() + " => " + serializedTimeoutPacket.length + " bytes");
                        mfcGroup.clear();
                    } catch (IOException e) {
                        System.out.println("\t=======>Network is unreachable");
                        failures++;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            //ex.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }
    }

    public HashMap<String, Ficheiro> getFicheiros() {
        return this.ficheiros;
    }

    private void incrementNodeTimeouts(TreeSet<Nodo> aliveNodes, String hash) {
        HashMap<String, Integer> ndTO = this.nodeTimeOut.get(hash);
        ArrayList<String> aux = new ArrayList<String>();

        for(Nodo n: aliveNodes)
            aux.add(n.id);

        int to;
        for(String id : ndTO.keySet()){
            if(!aux.contains(id)) {
                to = ndTO.get(id);
                ndTO.put(id, to++);
            }
            else
                ndTO.put(id, 0);
        }

        this.nodeTimeOut.put(hash, ndTO);
    }

    public void kill(){
        this.run = false;
    }

    public void run(){
        Ficheiro filePointer;
        ArrayList<FileReceiver> fRPointer;
        ArrayList<FileChunk> fCPointer;
        TreeSet<Nodo> aliveNodes = new TreeSet<Nodo>();
        ArrayList<String> toRemove = new ArrayList<String>();

        int packets = 0, to, i, tam;

        while(this.run){
            try {
                for (String h : this.ficheiros.keySet()) {
                    filePointer = this.ficheiros.get(h);
                    fRPointer = this.fileReceivers.get(h);

                    for (FileReceiver fr : fRPointer) {
                        fCPointer = fr.getFileChunks();
                        aliveNodes.addAll(fr.getNodes());

                        if (fCPointer.size() > 0)
                            filePointer.addFileChunks(fCPointer);
                        packets += fCPointer.size();
                    }

                    incrementNodeTimeouts(aliveNodes, h);

                    if (packets == 0 && !filePointer.getFull()) {
                        to = this.timeouts.get(h) + 1;
                        this.timeouts.put(h, to);
                        //System.out.println("TIMEOUT NA TRANSFERÊNCIA DE " + filePointer.getFileName());
                    }
                    else {
                        if (filePointer.getFull()) {
                            System.out.println("TRANSFERÊNCIA DE " + filePointer.getFileName() + " CONCLUIDA");
                            toRemove.add(h);

                            this.fh.sendUpdate(this.ft.addFicheiroToMyContent(this.fileInfos.get(h), this.ficheiros.get(h)));
                        }
                        else
                            this.timeouts.put(h, 0);
                    }

                    if(this.timeouts.containsKey(h) && !toRemove.contains(h)) {
                        to = this.timeouts.get(h);
                        if (to >= 3) {
                            //System.out.println("ENVIAR MENSAGENS DE TIMEOUT");
                            sendTimeoutPackets(h);
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
