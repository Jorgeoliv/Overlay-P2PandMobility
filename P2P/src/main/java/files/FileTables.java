package files;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import network.*;

public class FileTables {

    private HashMap<String, myFile> myContent = new HashMap<>(); // ficheiros que eu possuio, identificados pelo nome
    private ReentrantLock rlMyContent = new ReentrantLock();
    private HashMap<String, HashSet<Nodo>> nbrContent = new HashMap<>(); // ficheiros que os meus vizinhos posuem, identificados pelo nome do ficheiro
    private ReentrantLock rlNbrContent = new ReentrantLock();

    public FileTables(){

    }

    public void addMyContent(ArrayList<myFile> files){

        rlMyContent.lock();
        try{
            for(myFile m: files)
                myContent.put(m.id, m);
        }finally {
            rlMyContent.unlock();
        }

    }

    public void rmMyContent(ArrayList<myFile> files){

        rlMyContent.lock();
        try{
            for(myFile m: files)
                myContent.remove(m);
        }finally {
            rlMyContent.unlock();
        }

    }

    public void addNbrContent(String filename, ArrayList<Nodo> nodos){

        rlNbrContent.lock();
        try{
            if(nbrContent.containsKey(filename)){
                HashSet<Nodo> aux = nbrContent.get(filename);
                aux.addAll(nodos);
                nbrContent.put(filename, aux);
            }else{
                HashSet<Nodo> aux = new HashSet<>();
                aux.addAll(nodos);
                nbrContent.put(filename, aux);
            }
        }finally {
            rlNbrContent.unlock();
        }

        System.out.println("Conteudo: " + nbrContent.toString());

    }

    public void rmNbrContent(String filename, ArrayList<Nodo> nodos){

        rlNbrContent.lock();
        try{
            HashSet<Nodo> aux = nbrContent.get(filename);
            aux.removeAll(nodos);
            nbrContent.put(filename, aux);
        }finally {
            rlNbrContent.unlock();
        }

    }

    public void rmNbr(String id){
        Nodo n = new Nodo(id, null);
        rlNbrContent.lock();
        try{
            ArrayList<String> delete = new ArrayList<>();
            for(Map.Entry<String, HashSet<Nodo>> a: this.nbrContent.entrySet()){
                a.getValue().remove(n);
                if(a.getValue().size() == 0)
                    delete.add(a.getKey());
                //this.nbrContent.put(a.getKey(), a.getValue());
            }

            for(String a: delete)
                nbrContent.remove(a);

                               /*.filter(a -> {a.contains(n);
                                   System.out.println("Estou aqui!!"); return true;})*/
                              /* .map(a -> {a.remove(n); System.out.println("Eu estou aqui"); return a;});*/
        }finally {
            rlNbrContent.unlock();
        }

        System.out.println("Conteudo depois de eliminar o vizinho: " + nbrContent.toString());
    }



}
