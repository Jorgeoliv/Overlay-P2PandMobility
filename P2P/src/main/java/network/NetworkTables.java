package network;

import files.FileTables;

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

    private FileTables ft;


    public NetworkTables(FileTables ft){
        this.ft = ft;
    }

    public ArrayList<Nodo> getNbrsN1(){
        rlN1.lock();
        try{
            return (new ArrayList<Nodo>(nbrN1.values()));
        }finally {
            rlN1.unlock();
        }
    }

    private ArrayList<Nodo> concat(ArrayList<Nodo> a1, ArrayList<Nodo> a2){
        a1.addAll(a2);
        return a1;
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

    public void rmNbrN1(ArrayList<Nodo> nodos){

        rlN1.lock();
        try{
            for(Nodo n: nodos)
                nbrN1.remove(n);
        }finally {
            rlN1.unlock();
        }

    }

    public void addNbrN2(String id, ArrayList<Nodo> nodos){
        rlN2.lock();
        try{
            ArrayList<Nodo> aux = nbrN2.get(id);
            if(aux == null)
                aux = new ArrayList<>();
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


    /**
     * Para fazer reset a um determinado nodo
     * Despoltado por um alive que se recebeu
     * @param id
     */
    public void reset(String id){
        rlUp.lock();
        try{
            System.out.println("LA VOU EU FAZER UM RESET!! " + id);
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

//    @Override
//    public void run() {
//        byte[] b = null;
//        int opcao = -1;
//        ArrayList<Nodo> lista = null;
//        while(true){
//
//            System.out.println("Em escuta!");
//
//            //multi-part messaging
//            b = socket.recv(0);
//            opcao = Integer.parseInt(new String(b));
//            System.out.println("A opção é: " + opcao);
//
//            /**
//             * Caso a opção seja:
//             * 1 -> é para adicionar no N1
//             * 2 -> é para adicionar no N2
//             * 3 -> é para remover no N1
//             * 4 -> é para remover no N2
//             */
//
//            if(socket.hasReceiveMore()) {
//
//                byte[] l = socket.recv(0);
//                ArrayList<String> testeLista = new ArrayList<String>(Arrays.asList((new String(l)).split(" , ")));
//                for(String s: testeLista){
//                    Nodo n = (Nodo) s.;
//
//                }
//                System.out.println("O que eu recebi foi: " + new String(l));
//
//                switch (opcao) {
//                    case 1:
//                        break;
//                    case 2:
//                        break;
//                    case 3:
//                        break;
//                    case 4:
//                        break;
//                }
//            }
//        }
//    }




}
