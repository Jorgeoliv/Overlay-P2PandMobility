package network;


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
    private Nodo myNode;
    private NetworkTables nt;

    //Handlers
    private PingHandler pingHandler;
    private PongHandler pongHandler;
    private NbrConfirmationHandler nbrcHandler;
    private AddNbrHandler addNbrHandler;
    private AliveHandler aliveHandler;

    //Estruturas de dados Internas
        //estruturas de segurança
    private HashMap<String, Nodo> idNodo;
    private ArrayList<String> validPings;

    private IDGen idgen;
    private ReentrantLock nodeLock;
    private ReentrantLock pingLock;
    private ScheduledExecutorService ses;

    public NetworkHandler(Nodo me, NetworkTables nt) throws UnknownHostException {
        this.idgen = new IDGen(8);
        this.myNode = me;
        this.nt = nt;

        this.pingHandler = new PingHandler(SOFTCAP, HARDCAP, this, this.idgen, this.myNode, InetAddress.getByName("224.0.2.14"), mcp, ucp_Pong, 1, this.nt);
        this.pongHandler = new PongHandler(SOFTCAP, HARDCAP, this, this.myNode, ucp_Pong, ucp_NbrConfirmation, nt);
        this.nbrcHandler = new NbrConfirmationHandler(this, this.myNode, this.ucp_NbrConfirmation, this.ucp_Alive, nt);
        this.addNbrHandler = new AddNbrHandler(SOFTCAP, HARDCAP, this.idgen, this, this.myNode, this.ucp_AddNbr, this.ucp_NbrConfirmation, this.nt);
        this.aliveHandler = new AliveHandler(this, this.nt, this.myNode, this.ucp_Alive, this.idgen);

        this.idNodo = new HashMap<String, Nodo>();
        this.validPings = new ArrayList<String>();

        this.nodeLock = new ReentrantLock();
        this.pingLock = new ReentrantLock();

        this.ses = Executors.newSingleThreadScheduledExecutor();
    }

    public void run() {

        Thread t = new Thread(this.pingHandler);
        t.start();
        System.out.println("PINGHANDLER CRIADO");

        t = new Thread(this.pongHandler);
        t.start();
        System.out.println("PONGHANDLER CRIADO");

        t = new Thread(this.nbrcHandler);
        t.start();
        System.out.println("NBRCONFIRMATIONHANDLER CRIADO");

        t = new Thread(this.addNbrHandler);
        t.start();
        System.out.println("ADDNBRHANDLER CRIADO");

        t = new Thread(this.aliveHandler);
        t.start();
        System.out.println("ALIVEHANDLER CRIADO");

        t = null;
    }
    private Runnable invalidatePing = () ->{
        this.validPings.remove(0);
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

        for(String a : this.validPings)
            System.out.println(a);
        System.out.println(id);
        this.pingLock.unlock();
        return res;
    }

    public void registerNode(String id, Nodo node){
        this.nodeLock.lock();
        this.idNodo.put(id, node);
        this.nodeLock.unlock();
    }

    public boolean isNodeValid(String id, Nodo node){
        this.nodeLock.lock();
        boolean res = this.idNodo.get(id).equals(node);
        System.out.println("\nIS VALID?? => " + res);
        this.nodeLock.unlock();
        return res;
    }

    public boolean contains(Nodo node){
        this.nodeLock.lock();
        boolean res = this.idNodo.containsValue(node);
        System.out.println("\nCONTAINS?? => " + res);
        this.nodeLock.unlock();
        return res;
    }

    public  void removeNode(String id, Nodo node){
        this.nodeLock.lock();
        this.idNodo.remove(id,node);
        this.nodeLock.unlock();
    }
}
