package files;

import java.util.ArrayList;
import java.util.Hashtable;
import network.*;

public class FileTables {

    Hashtable<String, File> myContent = new Hashtable<>(); // ficheiros que eu possuio, identificados pelo nome
    Hashtable<String, ArrayList<Nodo>> nbrContent = new Hashtable<>(); // ficheiros que os meus vizinhos posuem, identificados pelo nome do ficheiro

}
