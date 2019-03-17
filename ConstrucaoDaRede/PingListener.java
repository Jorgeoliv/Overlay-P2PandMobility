import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class PingListner implements Runnable{
    InetAddress mcGroupIP;
    InetAddress myIP;
    int port;
    MulticastSocket mcSocket;

    public PingListner(String ip, int p){
        try{
            this.myIP = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
            System.out.println("MY IP => " + this.myIP);
            this.mcGroupIP = InetAddress.getByName(ip);
            this.port = p;
            this.mcSocket = new MulticastSocket(this.port);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        
        byte[] buf;
        DatagramPacket dp;

        try{
            this.mcSocket.joinGroup(this.mcGroupIP);

            while(true){
                buf = new byte[1024];
                dp = new DatagramPacket(buf, 1024);
                //System.out.println("ANTES DO RECEIVE");
                this.mcSocket.receive(dp);
                //System.out.println("DEPOIS DO RECEIVE");
                if (!dp.getAddress().equals(this.myIP))
                    System.out.println("RECEBI UMA MENSAGEM MULTICAST => " + new String(buf));
                else
                    System.out.println("SELF MSG => " + this.myIP + " " + dp.getAddress());
            }
        }
        catch(Exception e){
            e.printStackTrace();            
        }
        finally{
            try{
                this.mcSocket.leaveGroup(this.mcGroupIP);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}