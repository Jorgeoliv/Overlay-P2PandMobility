import java.io.*;
import java.net.*;
import java.util.ArrayList;

import files.*;
import network.*;

public class StartP2P {

    final static int port = 6000;
    final static int mcp = 6789;
    final static int ucp_Pong = 9876;
    final static int ucp_NbrConfirmation = 9877;

    final static int SOFTCAP = 3;
    final static int HARDCAP = 6;

    private static void upload(BufferedReader inP, FileHandler fh, FileTables ft, String NodeId){

        System.out.print("1 - Insira o nome do ficheiro: ");
        boolean c = false;
        String file = null;

        while(!c) {
            try {
                file = inP.readLine();
                c = true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.print("\nTente novamente: ");
                c = false;
            }
        }

        //Criei um arraylist caso queiramos dar a possibilidade de o utilizador escolher mais do que um ficheiro
        ArrayList<String> files = new ArrayList<>();
        files.add(file);

        ArrayList<FileInfo> fi = ft.newFicheiro(files, NodeId);
        fh.sendUpdate(fi);

    }

    private static void download(BufferedReader inP, FileHandler fh){

        System.out.print("1 - Insira o nome do ficheiro: ");
        boolean c = false;
        String file = null;

        while(!c) {
            try {
                file = inP.readLine();
                c = true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.print("\nTente novamente: ");
                c = false;
            }
        }

        //Neste caso faz sentido só procurar por um ficheiro

        fh.sendDiscovery(file);
        //System.out.println("O ficheiro lido foi: " + file);

    }


    private static void printNBR(NetworkHandler nh) {

        ArrayList<Nodo> nbrN1 = nh.getNBRN1();
        ArrayList<Nodo> nbrN2 = nh.getNBRN2();

        System.out.println("");
        System.out.println("***************************************");
        System.out.println("*   Vizinhos N1:                      *");
        for(Nodo n : nbrN1){
            System.out.println("*       Node ID => " + n.id + "   *");
            System.out.println("*       IP      => " + n.ip + "           *");
            System.out.println("*                                     *");
        }

        System.out.println("*   Vizinhos N2:                      *");
        for(Nodo n : nbrN2){
            System.out.println("*       Node ID => " + n.id + "   *");
            System.out.println("*       IP      => " + n.ip + "           *");
            System.out.println("*                                     *");
        }
        System.out.println("***************************************");

    }


    private static void printNBRContent(FileHandler fh) {
        ArrayList<String> fileNames = fh.getNBRContent();
        int i = 1;

        for(String name : fileNames){
            System.out.println("\t" + i++ + "\n\t\t" + "Nome => " + name);
        }
    }

    private static void printMyContent(FileHandler fh) {
        ArrayList<FileInfo> fis = fh.getMyContent();
        int i = 1;

        for(FileInfo fi :fis){
            System.out.println("\t" + i++ + ")\n\t\tNome => " + fi.name + "\n\t\tHash => " + fi.hash + "\n\t\tTamanho => " + fi.fileSize + " bytes ( " + fi.numOfFileChunks + " FileChunks )\n");
        }
    }

    public static void main(String[] args) throws UnknownHostException {

        FileHandler fh = new FileHandler();

        NetworkHandler nh = new NetworkHandler(fh.getFileTables());

        fh.updateVars(nh.getNetworkTables());

        Thread nht = new Thread(nh);
        nht.start();

        Thread fht = new Thread(fh);
        fht.start();

        BufferedReader inP = new BufferedReader(new InputStreamReader(System.in));
        boolean sair = false;
        int opcao = 0;

        while(!sair){

            while(!fh.getDrawMenu()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("*********** MENU ******************");
            System.out.println("*    1 - Upload Ficheiro          *");
            System.out.println("*    2 - Download Ficheiro        *");
            System.out.println("*    3 - Consultar Vizinhos       *");
            System.out.println("*    4 - Consultar meu Conteúdo   *");
            System.out.println("*    5 - Conteúdo dos Vizinhos    *");
            System.out.println("*        Outro para sair          *");
            System.out.println("***********************************");
            System.out.print("Opção: ");

            boolean c = false;

            while(!c) {
                try {
                    opcao = Integer.parseInt(inP.readLine());
                    c = true;
                } catch (Exception e) {
                    System.out.print("\nTente novamente: ");
                    c = false;
                }
            }

            switch (opcao){
                case 1: upload(inP, fh, fh.getFileTables(), nh.getID()); break;
                case 2: download(inP, fh); break;
                case 3: printNBR(nh); break;
                case 4: printMyContent(fh); break;
                case 5: printNBRContent(fh); break;
                default: sair = true;
            }
        }
        nh.sendBroadcastQuit();
        nh.kill();
        fh.kill();
        nht.interrupt();
        fht.interrupt();
    }
}
