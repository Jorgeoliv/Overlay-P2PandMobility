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
        System.out.println("O ficheiro lido foi: " + file);

    }

    public static void main(String[] args) throws UnknownHostException {

        FileHandler fh = new FileHandler();

        NetworkHandler nh = new NetworkHandler(fh.getFileTables());

        fh.updateVars(nh.getNetworkTables());

        Thread t = new Thread(nh);
        t.start();
        System.out.println("NETWORKHANDLER CRIADO");

        t = new Thread(fh);
        t.start();
        System.out.println("FILEHANDLER CRIADO");


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
            System.out.println("*********** MENU ************");
            System.out.println("*    1 - Upload Ficheiro    *");
            System.out.println("*    2 - Download Ficheiro  *");
            System.out.println("*    Outro para sair        *");
            System.out.println("*****************************");
            System.out.print("Opção: ");

            boolean c = false;

            while(!c) {
                try {
                    opcao = Integer.parseInt(inP.readLine());
                    c = true;
                } catch (Exception e) {
                    System.out.println(e);
                    System.out.print("\nTente novamente: ");
                    c = false;
                }
            }

            switch (opcao){
                case 1: upload(inP, fh, fh.getFileTables(), nh.getID()); break;
                case 2: download(inP, fh); break;
                default: sair = true;
            }
        }
    }
}
