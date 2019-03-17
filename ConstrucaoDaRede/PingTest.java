import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class PingTest{
    public static void main(String[] args) {
            try{
            String ip = "224.0.2.14";
            int porta = 6789;
            int ttl = 10;
            
            PingListner pl = new PingListner(ip, porta);
            Thread t = new Thread(pl);
            t.start();

            String msg = InetAddress.getLocalHost().getHostAddress();
            InetAddress group = InetAddress.getByName(ip);
            System.out.println("ESTE É O GRUPO MULTICAST => " + group);
            MulticastSocket s = new MulticastSocket();
            s.setTimeToLive(ttl);
            //s.joinGroup(group);
            DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, porta);

            while(true){
                s.send(hi);
                //System.out.println("ENVIEI UM HELLO");
                Thread.sleep(200);
            }
            
            // get their responses!
            //byte[] buf = new byte[1024];
            //DatagramPacket recv = new DatagramPacket(buf, buf.length);
            //s.receive(recv);
            
            // OK, I'm done talking - leave the group...
            //s.leaveGroup(group);
        }
        catch(Exception e){
            e.printStackTrace();            
        }
    }
}