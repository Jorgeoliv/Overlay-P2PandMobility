package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkTables{

    private Hashtable<String, Nodo> nbrN1 = new Hashtable<>(); //vizinhos nivel 1
    private ReentrantLock rlN1 = new ReentrantLock();
    private Hashtable<String, Nodo> nbrN2 = new Hashtable<>(); //vizinhos nivel 2
    private ReentrantLock rlN2 = new ReentrantLock();
    // ??????? Hashtable<String, Nodo> nbrN3 = new Hashtable<>(); //vizinhos nivel 3 ???????


    public NetworkTables(){

    }

    public void addNbrN1(ArrayList<Nodo> nodos){

        rlN1.lock();
        try{
            for(Nodo n: nodos)
                nbrN1.put(n.id, n);
        }finally {
            rlN1.unlock();
        }

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

    public void addNbrN2(ArrayList<Nodo> nodos){

        rlN2.lock();
        try{
            for(Nodo n: nodos)
                nbrN2.put(n.id, n);
        }finally {
            rlN2.unlock();
        }

    }

    public void rmNbrN2(ArrayList<Nodo> nodos){

        rlN2.lock();
        try{
            for(Nodo n: nodos)
                nbrN2.remove(n);
        }finally {
            rlN2.unlock();
        }

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
