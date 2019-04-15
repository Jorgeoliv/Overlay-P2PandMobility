package files;

import mensagens.ContentDiscovery;
import mensagens.UpdateTable;
import network.IDGen;
import network.NetworkTables;
import network.Nodo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class PairNodoFileInfo{

    FileInfo fileInfo;
    Nodo nodo;

    public PairNodoFileInfo(){

    }

    public PairNodoFileInfo(FileInfo fileInfo, Nodo nodo) {
        this.fileInfo = fileInfo;
        this.nodo = nodo;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairNodoFileInfo that = (PairNodoFileInfo) o;
        return nodo.equals(that.nodo);
    }

    public int hashCode() {
        return Objects.hash(nodo);
    }

    public String toString() {
        return "PairNodoFileInfo{" +
                "fileInfo=" + fileInfo +
                ", nodo=" + nodo +
                '}';
    }
}

public class FileHandler implements Runnable {

    private int ucp_ContentDiscovery = 7000;
    private int ucp_ContentOwner = 7001;
    private int ucp_filePullHandler = 7002;
    private int ucp_filePushHandler= 7003;
    private int ucp_Update = 7004;


    private ContentDiscoveryHandler contentDiscoveryHandler;
    private ContentOwnerHandler contentOwnerHandler;
    private FilePullHandler filePullHandler;
    private FilePushHandler filePushHandler;
    private FileTables fileTables;

    private NetworkTables networkTables;
    private UpdateHandler updateHandler;
    private Nodo myNode;
    private IDGen idGen;

    private ReentrantLock cdValidIDsLock;
    private ReentrantLock validNodesLock;

    private HashMap <String, ArrayList<PairNodoFileInfo>> cdResponses;

    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public FileHandler(){
        this.fileTables = new FileTables();
        this.idGen = new IDGen(8);
    }

    private void analisaCD(String id){
        int i = 1;
        ArrayList<PairNodoFileInfo> aux = cdResponses.get(id);
        for(PairNodoFileInfo pnfi: aux)
            System.out.println(i + ": " + pnfi.toString());

        this.cdResponses.remove(id);
    }

    private Runnable analyseAndDelete(final String id){
        Runnable ret = new Runnable() {
            @Override
            public void run() {
                analisaCD(id);
            }
        };

        return ret;
    }

    public void updateVars(NetworkTables networkTables){
        this.networkTables = networkTables;
        this.myNode = this.networkTables.getMyNode();
        this.contentDiscoveryHandler = new ContentDiscoveryHandler(this, this.networkTables, this.myNode,this.ucp_ContentDiscovery, this.ucp_ContentOwner, this.idGen);
        this.updateHandler = new UpdateHandler(this.ucp_Update, this.myNode, this.fileTables, this.idGen, this.networkTables);
        this.contentOwnerHandler = new ContentOwnerHandler(this, this.ucp_ContentOwner);
    }

    public FileTables getFileTables() {
        return fileTables;
    }

    public void run() {
        Thread t;

        try {
            t = new Thread(this.contentDiscoveryHandler);
            t.start();
            System.out.println("\t=> CONTENTDISCOVERYHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O CONTENTDISCOVERYHANDLER");
        }

        try {
            t = new Thread(this.updateHandler);
            t.start();
            System.out.println("\t=> UPDATEHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O UPDATEHANDLER");
        }

        t = null;
    }

    public void sendDiscovery(String file) {

        //tenho mesmo de criar assim o arraylist senão vai dar problemas com o kryo
        ArrayList<String> route = new ArrayList<>();
        route.add(myNode.id);
        String id = this.idGen.getID();
        //Vamos colocar por defeito um ttl de 5
        this.cdResponses.put(id, new ArrayList<PairNodoFileInfo>());
        ContentDiscovery cd = new ContentDiscovery(id, myNode, 5, file, route, this.myNode);
        this.contentDiscoveryHandler.sendDiscovery(cd);
        this.ses.schedule(analyseAndDelete(id), 5, TimeUnit.SECONDS);

    }

    public void registerPair(String cdID, Nodo node, FileInfo fi){

        ArrayList<PairNodoFileInfo> aux = this.cdResponses.get(cdID);
        if(aux != null) {
            aux.add(new PairNodoFileInfo(fi, node));
            this.cdResponses.put(cdID, aux);
        }
    }

    public void sendUpdate(ArrayList<FileInfo> files) {
        String oldHash = this.fileTables.getMyHash();
        String newHash = this.fileTables.addMyContent(files);

        UpdateTable ut = new UpdateTable(this.idGen.getID(), this.myNode, files, null, oldHash, newHash);
        this.updateHandler.sendUpdate(ut);
    }

}
