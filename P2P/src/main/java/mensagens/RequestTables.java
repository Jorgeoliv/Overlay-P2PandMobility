package mensagens;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RequestTables {

    private ArrayList<String> rootRequests = new ArrayList<String>();
    ReentrantLock rl = new ReentrantLock();
    private ArrayList<String> myRequests = new ArrayList<>();
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public RequestTables(){

    }


    /**
     * Serve para eliminar o primeiro da lista!
     * Os pedidos são inseridos por ordem de chegada e é criado um "timeout" que deve de eliminar o primeiro pedido à cabeça
     */
    Runnable deleteRootRequests = () -> {
        System.out.println("Antes de elimiar: " + rootRequests);
        rl.lock();
        try {
            rootRequests.remove(0);
        }finally {
            rl.unlock();
        }
        System.out.println("Depois de elimiar: " + rootRequests);
    };


    public void addRootRequest(String id){
        rl.lock();
        try {
            rootRequests.add(id);
            ses.schedule(deleteRootRequests, 5, TimeUnit.SECONDS);
        }finally {
            rl.unlock();
        }
    }

    public void addRequest(String id){
        rl.lock();
        try {
            myRequests.add(id);
        }finally {
            rl.unlock();
        }
    }

    public boolean containsRequest(String id){
        rl.lock();
        try{
            return (rootRequests.contains(id) || myRequests.contains(id));
        }finally {
            rl.unlock();
        }
    }

}
