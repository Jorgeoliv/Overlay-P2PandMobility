package files;

import mensagens.ContentDiscovery;
import network.IDGen;
import network.NetworkTables;
import network.Nodo;

import java.util.ArrayList;


public class FileHandler implements Runnable {

    private int ucp_ContentDiscovery = 7000;
    private int ucp_ContentOwner = 7001;
    private int ucp_filePullHandler = 7002;
    private int ucp_filePushHandler= 7003;

    private ContentDiscoveryHandler contentDiscoveryHandler;
    private ContentOwnerHandler contentOwnerHandler;
    private FilePullHandler filePullHandler;
    private FilePushHandler filePushHandler;
    private FileTables fileTables;

    private NetworkTables networkTables;
    private Nodo myNode;
    private IDGen idGen;

    public FileHandler(){
        this.fileTables = new FileTables();
        this.idGen = new IDGen(8);
    }

    public void updateVars(NetworkTables networkTables){
        this.networkTables = networkTables;
        this.myNode = this.networkTables.getMyNode();
        this.contentDiscoveryHandler = new ContentDiscoveryHandler(this.networkTables, this.myNode,this.ucp_ContentDiscovery, this.idGen);
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

        t = null;
    }

    public void sendDiscovery(String file) {

        //tenho mesmo de criar assim o arraylist senão vai dar problemas com o kryo
        ArrayList<String> route = new ArrayList<>();
        route.add(myNode.id);
        //Vamos colocar por defeito um ttl de 5
        ContentDiscovery cd = new ContentDiscovery(this.idGen.getID(), myNode, 5, file, route);
        this.contentDiscoveryHandler.sendDiscovery(cd);

    }
}
