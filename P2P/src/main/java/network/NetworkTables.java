package network;

import com.sun.security.auth.NTDomainPrincipal;
import files.FileInfo;
import files.FileTables;
import sun.rmi.runtime.NewThreadAction;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkTables{

    private HashMap<String, Nodo> nbrN1 = new HashMap<>(); //vizinhos nivel 1
    private ReentrantLock rlN1 = new ReentrantLock();
    private HashMap<String, ArrayList<Nodo>> nbrN2 = new HashMap<>(); //vizinhos nivel 2 (ID string n1 -> vizinhos nivel2 que passam por este de nivel1)
    private ReentrantLock rlN2 = new ReentrantLock();
    // ??????? Hashtable<String, Nodo> nbrN3 = new Hashtable<>(); //vizinhos nivel 3 ???????

    //
    private HashMap<String, Integer> nbrUp = new HashMap<>();
    private ReentrantLock rlUp = new ReentrantLock();

    public FileTables ft;


    public NetworkTables(FileTables ft){
        this.ft = ft;
    }

    private ArrayList<Nodo> concat(ArrayList<Nodo> a1, ArrayList<Nodo> a2){
        a1.addAll(a2);
        return a1;
    }

    public ArrayList<Nodo> getNbrsN1(){
        rlN1.lock();
        try{
            return (new ArrayList<Nodo>(nbrN1.values()));
        }finally {
            rlN1.unlock();
        }
    }

    public ArrayList<Nodo> getNbrsN2(){
        rlN2.lock();
        try{
            Optional<ArrayList<Nodo>> a = nbrN2.values().stream().reduce((x, y) -> concat(x, y));
            if(a.isPresent()) {
                HashSet<Nodo> b = new HashSet<Nodo>(a.get());
                return new ArrayList<>(b);
            }else{
                return new ArrayList<>();
            }
        }finally {
            rlN2.unlock();
        }
    }

    public void addNbrN1(ArrayList<Nodo> nodos){

        rlN1.lock();
        rlUp.lock();
        try{
            for(Nodo n: nodos) {
                nbrN1.put(n.id, n);
                nbrUp.put(n.id, 0);
            }
        }finally {
            rlN1.unlock();
            rlUp.unlock();
        }
        System.out.println(nbrN1.toString());

    }

    public void addNbrN1(Nodo nodo){

        rlN1.lock();
        rlUp.lock();
        try{
            nbrN1.put(nodo.id, nodo);
            nbrUp.put(nodo.id, 0);
        }finally {
            rlN1.unlock();
            rlUp.unlock();
        }
        System.out.println(nbrN1.toString());

    }

    public void rmNbrN1(ArrayList<Nodo> nodos){

        rlN1.lock();
        try{
            for(Nodo n: nodos)
                nbrN1.remove(n);
        }finally {
            rlN1.unlock();
        }

    }

    public void addNbrN2(String id, ArrayList<Nodo> nodos, Nodo myNode){
        rlN2.lock();
        try{
            ArrayList<Nodo> aux = nbrN2.get(id);
            if(aux == null)
                aux = new ArrayList<>();
            nodos.remove(myNode);
            aux.addAll(nodos);
            nbrN2.put(id, aux);
        }finally {
            rlN2.unlock();
        }

    }

    public void rmNbrN2(String id, ArrayList<Nodo> nodos){
        rlN2.lock();
        try{
            ArrayList<Nodo> aux = nbrN2.get(id);
            if(aux == null)
                aux = new ArrayList<>();
            aux.removeAll(nodos);
            nbrN2.put(id, aux);
        }finally {
            rlN2.unlock();
        }

    }

    public void updateNbrN2(String id, ArrayList<Nodo> nodos){
        rlN2.lock();
        try{
            nbrN2.put(id, nodos);
        }finally {
            rlN2.unlock();
        }

    }

    /**
     * Para fazer reset a um determinado nodo
     * Despoltado por um alive que se recebeu
     * @param id
     */
    public void reset(String id){
        rlUp.lock();
        try{
            nbrUp.put(id, 0);
            System.out.println(nbrUp);
        }finally {
            rlUp.unlock();
        }
    }

    /**
     * Para incrementar o valor dos "ups". Se atingir o valor de 3 para algum vizinho quer dizer que deixou de ser um vizinho
     */
    public void inc(){
        rlUp.lock();
        try{
            for(Map.Entry<String, Integer> a: this.nbrUp.entrySet()){
                int value = a.getValue();
                value++;
                if(value == 3){
                    //Temos de eliminar o vizinho ...
                    System.out.println("VAMOS LA ELIMINAR UM GAJO DE UM VIZNHO!!!! " + a.getKey());
                    String id = a.getKey();

                    rlN1.lock();
                    nbrN1.remove(id);
                    rlN1.unlock();

                    rlN2.lock();
                    nbrN2.remove(id);
                    rlN2.unlock();

                    nbrUp.remove(id);

                    ft.rmNbr(id);
                }else
                    this.nbrUp.put(a.getKey(), value);
            }
        }finally {
            rlUp.unlock();
        }

        System.out.println("O valor é: " + nbrUp.toString());
    }

    public ArrayList<FileInfo> getFileInfo(){
        return this.ft.getFileInfo();
    }

    public int getNumVN1(){
        this.rlN1.lock();
        int tam = this.nbrN1.size();
        this.rlN1.unlock();
        return tam;
    }

    public int getNumVN2(){
        this.rlN2.lock();
        int tam = this.nbrN2.size();
        this.rlN2.unlock();
        return tam;
    }

    public boolean nbrN1Contains(Nodo node){
        this.rlN1.lock();
        boolean res = this.nbrN1.containsValue(node);
        this.rlN1.unlock();
        return res;
    }

    public Nodo getRandomNN1(){
        Random rand = new Random();

        ArrayList<String> aux = new ArrayList<String>();

        for(String n : this.nbrN1.keySet())
            if(this.nbrN2.containsKey(n))
                if(this.nbrN2.get(n).size()>0)
                    aux.add(n);

        if(aux.size()>0)
            return this.nbrN1.get(aux.get(rand.nextInt(aux.size())));
        else
            return null;
    }

    public Nodo getRandomNN2(Nodo node){
        Random rand = new Random();

        ArrayList<Nodo> aux = this.nbrN2.get(node.id);
        return aux.get(rand.nextInt(this.nbrN2.size()));
    }
}
