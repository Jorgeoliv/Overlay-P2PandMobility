import java.net.*;
import java.util.ArrayList;

import files.*;
import mensagens.*;
import network.*;

public class StartP2P {

    final static int port = 6000;

    public static void main(String[] args) {


        //Vai ter de começar a iniciar o multicast com o Ping

        //Neste ponto já está na rede (mesmo que esteja sozinho) e tem de se juntar ao grupo multicast


        /**
         * Iniciação de threads:
         *
         * -> Criação das estruturas partilhadas
         * -> Uma thread para tratar de enviar periodicamente os PINGs
         * -> Uma thread que vai estar à escuta de PINGs (numa porta especifica) e que vai responder com PONGs
         * -> Uma thread para escutar os "alive" (numa porta especifica) (testar com a mesma thread colocar um timeout para verificar todos os alives e também para enviar um alive para a lista de vizinhos nivel 1)
         * -> Uma thread para ouvir os restantes pedidos (consoante o pedido pode <<convocar>> novas threads)
         *
         */

        //Vai estar a correr num "while(true)" até ser interrompida quer por saida brusca ou então por indicação do utilizador

        //Código:

        //Criação das estruturas partilhadas do proprio nodo

        NetworkTables nt = new NetworkTables();
        FileTables ft = new FileTables();
        RequestTables rt = new RequestTables();

        //Entrada na rede

        //CODIGO!!!

        try {
            //Vou criar um socket que fica a escuta na porta 6000 e com o endereço do meu HOST
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            System.out.println("O endereço é: " + InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));

            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

}
