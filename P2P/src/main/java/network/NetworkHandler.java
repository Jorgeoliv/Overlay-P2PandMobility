package network;


import files.FileTables;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkHandler implements Runnable{

    //Portas
    private int port = 6000;
    private int mcp = 6789;
    private int ucp_Pong = 9876;
    private int ucp_NbrConfirmation = 9877;
    private int ucp_AddNbr = 7789;
    private int ucp_Alive = 6001;

    //Caps
    private int SOFTCAP = 3;
    private int HARDCAP = 6;

    //Estruturas de dados externas
    private IDGen idgen;
    private Nodo myNode;

    //Estruturas de dados Internas
    //estruturas de segurança
    private HashMap<String, String> idNodo;
    private ArrayList<String> validPings;
    private ArrayList<String> validAddNbrs;

    private NetworkTables nt;
    private ReentrantLock nodeLock;
    private ReentrantLock pingLock;
    private ReentrantLock addNbrsLock;
    private ScheduledExecutorService ses;

    //Handlers
    private PingHandler pingHandler;
    private PongHandler pongHandler;
    private NbrConfirmationHandler nbrcHandler;
    private AddNbrHandler addNbrHandler;
    private AliveHandler aliveHandler;



    public NetworkHandler(FileTables ft) throws UnknownHostException {
        this.idgen = new IDGen(8);
        this.myNode = new Nodo(idgen.getNodeID(), InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).toString().replace("/", ""));;

        this.idNodo = new HashMap<String, String>();
        this.validPings = new ArrayList<String>();
        this.validAddNbrs = new ArrayList<String>();

        this.nt = new NetworkTables(ft);
        this.nodeLock = new ReentrantLock();
        this.pingLock = new ReentrantLock();
        this.addNbrsLock = new ReentrantLock();
        this.ses = Executors.newSingleThreadScheduledExecutor();

        this.pingHandler = new PingHandler(SOFTCAP, HARDCAP, this, this.idgen, this.myNode, InetAddress.getByName("224.0.2.14"), mcp, ucp_Pong, 1, this.nt);
        this.pongHandler = new PongHandler(SOFTCAP, HARDCAP, this, this.idgen, this.myNode, ucp_Pong, ucp_NbrConfirmation, ucp_AddNbr, nt);
        this.nbrcHandler = new NbrConfirmationHandler(this, this.myNode, this.ucp_NbrConfirmation, this.ucp_Alive, nt);
        this.addNbrHandler = new AddNbrHandler(SOFTCAP, HARDCAP, this.idgen, this, this.myNode, this.ucp_AddNbr, this.ucp_NbrConfirmation, this.nt);
        this.aliveHandler = new AliveHandler(this, this.nt, this.myNode, this.ucp_Alive, this.idgen);

    }

    public void run() {

        Thread t;

        System.out.println("\n--------------------------------------------\n");
        try {
            t = new Thread(this.pingHandler);
            t.start();
            System.out.println("\t=> PINGHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR PINGHANDLER");
        }

        try {
            t = new Thread(this.pongHandler);
            t.start();
            System.out.println("\t=> PONGHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR PONGHANDLER");
        }

        try {
            t = new Thread(this.nbrcHandler);
            t.start();
            System.out.println("\t=> NBRCONFIRMATIONHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR NBRCONFIRMATIONHANDLER");
        }

        try {
            t = new Thread(this.addNbrHandler);
            t.start();
            System.out.println("\t=> ADDNBRHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR ADDNBRHANDLER");
        }

        try {
            t = new Thread(this.aliveHandler);
            t.start();
            System.out.println("\t=> ALIVEHANDLER CRIADO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("=> ERRO AO CRIAR ALIVEHANDLER");
        }

        t = null;
        System.out.println("\n--------------------------------------------\n");

    }
    private Runnable invalidatePing = () ->{
        this.pingLock.lock();
        this.validPings.remove(0);
        this.pingLock.unlock();
    };

    public void registerPing(String id) {
        this.pingLock.lock();
        this.validPings.add(id);
        this.pingLock.unlock();
        ses.schedule(invalidatePing, 60, TimeUnit.SECONDS);
    }

    public boolean isPingValid(String id) {
        this.pingLock.lock();
        boolean res = this.validPings.contains(id);
        this.pingLock.unlock();
        return res;
    }

    private Runnable invalidateAddNbrs = () ->{
        this.addNbrsLock.lock();
        this.validAddNbrs.remove(0);
        this.addNbrsLock.unlock();
    };
    public void registerAddNbr(String id) {
        this.addNbrsLock.lock();
        this.validAddNbrs.add(id);
        this.addNbrsLock.unlock();
        ses.schedule(invalidateAddNbrs, 30, TimeUnit.SECONDS);
    }

    public boolean isAddNbrValid(String id) {
        this.addNbrsLock.lock();
        boolean res = this.validAddNbrs.contains(id);
        this.addNbrsLock.unlock();
        return res;
    }

    public void removeAddNbr(String id){
        this.addNbrsLock.lock();
        this.validAddNbrs.remove(id);
        this.addNbrsLock.unlock();
    }

    public void registerNode(String id, Nodo node){
        this.nodeLock.lock();
        this.idNodo.put(id, node.id);
        this.nodeLock.unlock();
    }

    public boolean isNodeValid(String id, Nodo node){
        this.nodeLock.lock();
        boolean res = false;
        if(this.idNodo.containsKey(id))
            res = this.idNodo.get(id).equals(node.id);
        this.nodeLock.unlock();
        return res;
    }

    public boolean isNodePresent(Nodo node) {
        boolean res = false;
        this.nodeLock.lock();
        res = this.idNodo.containsValue(node.id);
        this.nodeLock.unlock();

        return res;
    }

    public boolean contains(Nodo node){
        this.nodeLock.lock();
        boolean res = this.idNodo.containsValue(node.id);
        this.nodeLock.unlock();
        return res;
    }

    public  void removeNode(String id, Nodo node){
        this.nodeLock.lock();
        this.idNodo.remove(id,node.id);
        this.nodeLock.unlock();
    }
}
