package files;

import mensagens.ContentDiscovery;
import mensagens.UpdateTable;
import network.IDGen;
import network.NetworkTables;
import network.Nodo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class PairNodoFileInfo{

    FileInfo fileInfo;
    ArrayList<Nodo> nodo;

    public PairNodoFileInfo(FileInfo fileInfo, ArrayList<Nodo> nodo) {
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


    private HashMap <String, ArrayList<PairNodoFileInfo>> cdResponses;

    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    private boolean drawMenu = true;

    private ArrayList<Thread> threads;

    public FileHandler(){
        this.fileTables = new FileTables();
        this.idGen = new IDGen(8);
        this.cdResponses = new HashMap<String, ArrayList<PairNodoFileInfo>>();
        this.threads = new ArrayList<Thread>();
    }


    private PairNodoFileInfo fileChoice(int max, ArrayList<PairNodoFileInfo> aux){
        BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));

        boolean sair = false;
        int opcao = 0;

        System.out.println("***** Escolha a fonte *****");
        System.out.print("Opção: ");

        boolean c = false;

        while (!c) {
            try {
                opcao = Integer.parseInt(inP.readLine());
                if(opcao >= 0 && opcao < max)
                    c = true;
                else
                    System.out.println("\nTente novamente: ");
            } catch (Exception e) {
                System.out.println(e);
                System.out.print("\nTente novamente: ");
                c = false;
            }
        }

        if(opcao != 0)
            return aux.get(opcao-1);
        else
            return null;
    }

    private void analisaCD(String id, String file){
        ArrayList<PairNodoFileInfo> aux = cdResponses.get(id);
        HashMap <String, PairNodoFileInfo> auxHash = new HashMap<String, PairNodoFileInfo>();

        if(aux.size() > 0) {
            HashMap<String, PairNodoFileInfo> sameFile = new HashMap<String, PairNodoFileInfo>();
            PairNodoFileInfo newPnfi;
            ArrayList<Nodo> node;

            System.out.println("\nA SUA PESQUISA POR " + file + " RETORNOU OS SEGUINTES RESOLTADOS:\n");
            int i = 1;
            for (PairNodoFileInfo pnfi : aux) {
                auxHash.put(pnfi.fileInfo.hash, pnfi);
                System.out.println("\t" + i++ + ")\n\t\tNode " + pnfi.nodo.get(0).id + "( ip: " + pnfi.nodo.get(0).ip + " )" + "\n\t\tNome => " + pnfi.fileInfo.name + "\n\t\tHash => " + pnfi.fileInfo.hash + "\n\t\tTamanho => " + pnfi.fileInfo.fileSize + " bytes ( " + pnfi.fileInfo.numOfFileChunks + " FileChunks )\n");
            }

            for(PairNodoFileInfo p : aux){

                if(sameFile.containsKey(p.fileInfo.hash)) {
                    newPnfi = sameFile.get(p.fileInfo.hash);
                }
                else {
                    newPnfi = new PairNodoFileInfo(p.fileInfo, new ArrayList<Nodo>());
                }
                node = newPnfi.nodo;
                node.addAll(p.nodo);
                sameFile.put(p.fileInfo.hash, newPnfi);

            }

            for(PairNodoFileInfo p : auxHash.values()){
                newPnfi = sameFile.get(p.fileInfo.hash);
                if(newPnfi.nodo.size() > 1) {
                    System.out.println("\t" + i++ + ")");
                    for (Nodo n : newPnfi.nodo) {
                        System.out.println("\t\tNode " + n.id + "( ip: " + n.ip + " )");
                    }
                    System.out.print("\t\tNome => " + p.fileInfo.name + "\n\t\tHash => " + p.fileInfo.hash + "\n\t\tTamanho => " + p.fileInfo.fileSize + " bytes ( " + p.fileInfo.numOfFileChunks + " FileChunks )\n");
                }
            }

            System.out.println("\t0 Para Cancelar");

            aux.addAll(sameFile.values());

            PairNodoFileInfo choice = fileChoice(i, aux);

            this.cdResponses.remove(id);
            if(choice != null)
                this.filePullHandler.send(choice);
        }
        else
            System.out.println("\nA SUA PESQUISA POR " + file + " NÃO RETURNOU NENHUM RESULTADO\n");
    }

    private Runnable analyseAndDelete(final String id, String file){
        Runnable ret = new Runnable() {
            @Override
            public void run() {
                analisaCD(id, file);
                drawMenu = true;
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
        this.filePushHandler = new FilePushHandler(this.ucp_filePushHandler, this.ucp_filePullHandler, this.fileTables, this, this.idGen, this.myNode);
        this.filePullHandler = new FilePullHandler(this.ucp_filePullHandler, this.filePushHandler, this.idGen, this.myNode);

        this.filePushHandler.setFPLH(this.filePullHandler);
    }

    public FileTables getFileTables() {
        return fileTables;
    }

    public void run() {
        Thread t;

        try {
            t = new Thread(this.contentDiscoveryHandler);
            this.threads.add(t);
            t.start();
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O CONTENTDISCOVERYHANDLER");
        }

        try {
            t = new Thread(this.updateHandler);
            this.threads.add(t);
            t.start();
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O UPDATEHANDLER");
        }

        try {
            t = new Thread(this.contentOwnerHandler);
            this.threads.add(t);
            t.start();
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O CONTENTOWNER");
        }

        try {
            t = new Thread(this.filePushHandler);
            this.threads.add(t);
            t.start();
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O FILEPUSHHANDLER");
        }

        try {
            t = new Thread(this.filePullHandler);
            this.threads.add(t);
            t.start();
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR O FILEPULLHANDLER");
        }

        t = null;
    }

    public void sendDiscovery(String file) {

        this.drawMenu = false;

        //tenho mesmo de criar assim o arraylist senão vai dar problemas com o kryo
        ArrayList<String> route = new ArrayList<>();
        route.add(myNode.id);
        String id = this.idGen.getID("");
        //Vamos colocar por defeito um ttl de 5
        this.cdResponses.put(id, new ArrayList<PairNodoFileInfo>());
        ContentDiscovery cd = new ContentDiscovery(id, myNode, 5, file, route, this.myNode);
        this.contentDiscoveryHandler.sendDiscovery(cd);
        this.ses.schedule(analyseAndDelete(id, file), 5, TimeUnit.SECONDS);

    }

    public void registerPair(String cdID, Nodo node, FileInfo fi){

        ArrayList<PairNodoFileInfo> aux = this.cdResponses.get(cdID);
        ArrayList<Nodo> nodePointer;
        if(aux != null) {
            nodePointer = new ArrayList<Nodo>();
            nodePointer.add(node);
            aux.add(new PairNodoFileInfo(fi, nodePointer));
            this.cdResponses.put(cdID, aux);
        }
        else
            System.out.println("AUX É NULL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    public void sendUpdate(ArrayList<FileInfo> files) {
        String oldHash = this.fileTables.getMyHash();
        String newHash = this.fileTables.newHash(files);

        UpdateTable ut = new UpdateTable(this.idGen.getID(""), this.myNode, files, null, oldHash, newHash);
        this.updateHandler.sendUpdate(ut);
    }

    public boolean getDrawMenu(){
        return this.drawMenu;
    }

    public ArrayList<FileInfo> getMyContent(){
        return this.fileTables.getFileInfo();
    }

    public ArrayList<String> getNBRContent(){
        return this.fileTables.getNBRFileInfo();
    }

    public HashMap<String, Ficheiro> getDownloadsInProgress(){
        return this.filePushHandler.getFicheiros();
    }

    public void kill() {
        try {

            this.filePullHandler.kill();
            this.contentOwnerHandler.kill();
            this.contentDiscoveryHandler.kill();
            this.updateHandler.kill();
            this.filePushHandler.kill();

            Thread.sleep(100);

            this.ses.shutdownNow();

        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("FILEHANDLER KILLED");
    }
}
