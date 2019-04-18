package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import mensagens.FilePull;
import mensagens.Header;
import network.IDGen;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class FilePullHandler implements Runnable{

    private final FilePushHandler fph;
    private final IDGen idGen;
    private int ucp_FilePull;

    public FilePullHandler(int ucp_FilePull, FilePushHandler fph, IDGen idGen){
        this.ucp_FilePull = ucp_FilePull;
        this.fph = fph;
        this.idGen = idGen;

    }

    private void processFPH(DatagramPacket dp) {
        byte[] buf;
        Kryo kryo = new Kryo();
        buf = dp.getData();
        ByteArrayInputStream bStream = new ByteArrayInputStream(buf);
        Input input = new Input(bStream);
        Header header = (Header) kryo.readClassAndObject(input);
        input.close();

        if(header instanceof FilePull) {
            //VER SE TEMOS O FICHEIRO
            this.fph.sendFile((FilePull) header);
        }
    }

    public void send(PairNodoFileInfo choice) {
        String id = idGen.getID();

        this.fph.getPorts(id);
    }

    public void run() {
        try{


            byte[] buf;
            DatagramPacket filepull;

            DatagramSocket ds = new DatagramSocket(this.ucp_FilePull);

            while(true){
                buf = new byte[1500];
                filepull = new DatagramPacket(buf, buf.length);

                ds.receive(filepull);

                processFPH(filepull);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
