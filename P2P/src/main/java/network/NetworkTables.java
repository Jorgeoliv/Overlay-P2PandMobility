package network;

import files.FileInfo;
import files.FileTables;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkTables{

    private HashMap<String, Nodo> nbrN1 = new HashMap<>(); //vizinhos nivel 1
    private ReentrantLock rlN1 = new ReentrantLock();
    private HashMap<String, TreeSet<Nodo>> nbrN2 = new HashMap<>(); //vizinhos nivel 2 (ID string n1 -> vizinhos nivel2 que passam por este de nivel1)
    private ReentrantLock rlN2 = new ReentrantLock();

    private HashMap<String, Integer> nbrUp = new HashMap<>();
    private ReentrantLock rlUp = new ReentrantLock();

    public FileTables ft;

    private Nodo myNode;

    NetworkTables(Nodo myNode, FileTables ft){
        this.ft = ft;
        this.myNode = myNode;
    }

    public HashMap<String, Nodo> getNbrN1(){
        return this.nbrN1;
    }

    public ReentrantLock getRlN1(){
        return this.rlN1;
    }

    public ReentrantLock getRlN2(){
        return this.rlN2;
    }

    public Nodo getMyNode(){
        return this.myNode;
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

        this.rlN2.lock();
        TreeSet<Nodo> res = new TreeSet<Nodo>();
        for(TreeSet<Nodo> vn2 : this.nbrN2.values()){
            res.addAll(vn2);
        }

        ArrayList<Nodo> resArray = new ArrayList<Nodo>(res);

        this.rlN2.unlock();

        return resArray;

    }

    public HashMap<String, TreeSet<Nodo>> getNbrN2(){
        this.rlN2.lock();
        HashMap<String, TreeSet<Nodo>> res = this.nbrN2;
        this.rlN2.unlock();

        return res;
    }

    public void addNbrN1(Nodo nodo){

        this.rlN1.lock();
        this.rlN2.lock();
        this.rlUp.lock();
        try{
            this.nbrN1.put(nodo.id, nodo);
            this.nbrUp.put(nodo.id, 0);
            TreeSet <Nodo> ts;
            for(String id: this.nbrN2.keySet()){
                ts = this.nbrN2.get(id);
                ts.remove(nodo);
                this.nbrN2.put(id, ts);
            }
        }finally {
            this.rlN1.unlock();
            this.rlN2.unlock();
            this.rlUp.unlock();
        }
        System.out.println(nbrN1.toString());

    }

    public void rmNbrN1(String id){

        rlN1.lock();
        try{
            nbrN1.remove(id);
        }finally {
            rlN1.unlock();
        }

    }

    public void addNbrN2(String id, ArrayList<Nodo> nodos, Nodo myNode){
        rlN2.lock();
        try{
            TreeSet<Nodo> aux = this.nbrN2.get(id);
            this.rlN1.lock();
            ArrayList<Nodo> n1 = new ArrayList<Nodo>(this.nbrN1.values());
            this.rlN1.unlock();
            if(aux == null)
                aux = new TreeSet<Nodo>();
            //System.out.println("Os nodos de nivel 2 que vou adicionar são: ");
            //System.out.println(nodos);
            nodos.remove(myNode);
            nodos.removeAll(n1);
            //System.out.println("Os meus vizinhos de nivel 1 são: ");
            //System.out.println(n1);
            //System.out.println("Os nodos de nivel 2 que vou efetivamente adicionar são: ");
            //System.out.println(nodos);
            aux.addAll(nodos);
            nbrN2.put(id, aux);
        }finally {
            rlN2.unlock();
        }

    }

    public void rmNbrN2(String id){
        rlN2.lock();
        try{
            nbrN2.remove(id);
        }finally {
            rlN2.unlock();
        }

    }

    public void updateNbrN2(String id, ArrayList<Nodo> nodos, Nodo myNode){
        rlN2.lock();
        try{
            TreeSet <Nodo> n = new TreeSet<>(nodos);
            this.rlN1.lock();
            ArrayList<Nodo> n1 = new ArrayList<Nodo>(this.nbrN1.values());
            this.rlN1.unlock();
            // **** Tamos a eliminar os vizinhos de nivel 1 do nosso vizinho que são nossos vizinhos de nivel 1
            n.remove(myNode);
            n.removeAll(n1);
            nbrN2.put(id, n);
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
            //System.out.println(nbrUp);
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
            ArrayList<String> toRemove = new ArrayList<>();
            for(Map.Entry<String, Integer> a: this.nbrUp.entrySet()){
                int value = a.getValue();
                value++;
                if(value == 3){
                    //Temos de eliminar o vizinho ...
                    //System.out.println("VAMOS LA ELIMINAR UM GAJO DE UM VIZNHO!!!! " + a.getKey());
                    String id = a.getKey();

                    rlN1.lock();
                    nbrN1.remove(id);
                    rlN1.unlock();

                    rlN2.lock();
                    nbrN2.remove(id);
                    rlN2.unlock();

                    toRemove.add(id);

                    ft.rmNbr(id);
                }else
                    this.nbrUp.put(a.getKey(), value);
            }
            toRemove.stream().map(a -> nbrUp.remove(a));
        }finally {
            rlUp.unlock();
        }

        //System.out.println("O valor é: " + nbrUp.toString());
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

        this.rlN1.lock();
        this.rlN2.lock();

        for(String n : this.nbrN1.keySet()) {
            if (this.nbrN2.containsKey(n)) {
                if (this.nbrN2.get(n).size() > 0) {
                    aux.add(n);
                }
            }
        }
        Nodo res;

        if(aux.size()>0)
            res = this.nbrN1.get(aux.get(rand.nextInt(aux.size())));
        else
            res = null;
        this.rlN1.unlock();
        this.rlN2.unlock();

        return res;
    }

    public Nodo getRandomNN2(Nodo node){
        Random rand = new Random();

        this.rlN2.lock();
        TreeSet <Nodo> ts = this.nbrN2.get(node.id);
        ArrayList<Nodo> aux = new ArrayList<Nodo>(ts);
        Nodo nodo = aux.get(rand.nextInt(ts.size()));
        this.rlN2.unlock();

        return nodo;
    }
}
