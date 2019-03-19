package files;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import network.*;

public class FileTables {

    private Hashtable<String, myFile> myContent = new Hashtable<>(); // ficheiros que eu possuio, identificados pelo nome
    private ReentrantLock rlMyContent = new ReentrantLock();
    private Hashtable<String, HashSet<Nodo>> nbrContent = new Hashtable<>(); // ficheiros que os meus vizinhos posuem, identificados pelo nome do ficheiro
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

    }

    public void rmNbrContent(String filename, ArrayList<Nodo> nodos){

        rlMyContent.lock();
        try{
            HashSet<Nodo> aux = nbrContent.get(filename);
            aux.removeAll(nodos);
            nbrContent.put(filename, aux);
        }finally {
            rlMyContent.unlock();
        }

    }



}
