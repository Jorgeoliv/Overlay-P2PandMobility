public class StartP2P {

    public static void main(String[] args) {

        //Vai ter de começar a iniciar o multicast com o Ping

        //Neste ponto já está na rede (mesmo que esteja sozinho) e tem de se juntar ao grupo multicast

        /**
         * Iniciação de threads:
         *
         * -> Uma thread para tratar de enviar periodicamente os PINGs
         * -> Uma thread que vai estar à escuta de PINGs (numa porta especifica) e que vai responder com PONGs
         * -> Uma thread para a tabela dos ficheiros e outra para a tabela da rede (cada uma vai ficar em escuta de pedidos para alterar a estrutura partilhada => 0MQ, inproc)
         * -> Uma thread para escutar os "alive" (numa porta especifica) (testar com a mesma thread colocar um timeout para verificar todos os alives e também para enviar um alive para a lista de vizinhos nivel 1)
         * -> Uma thread para ouvir os restantes pedidos (pode ser a principal, e depois consoante o pedido pode <<convocar>> novas threads)
         *
         */

        //Vai estar a correr num "while(true)" até ser interrompida quer por saida brusca ou então por indicação do utilizador
    }

}
