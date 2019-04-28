package files;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mensagens.FilePull;
import mensagens.Header;
import network.IDGen;
import network.Nodo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class FilePullHandler implements Runnable{
    private Nodo myNode;

    private FilePushHandler fph;
    private IDGen idGen;
    private int ucp_FilePullHandler;

    private int pps = 500;

    public FilePullHandler(int ucp_FilePull, FilePushHandler fph, IDGen idGen, Nodo myNode){
        this.myNode = myNode;

        this.ucp_FilePullHandler = ucp_FilePull;
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
            FilePull fp = (FilePull) header;
            System.out.println("RECEBI O FILEPULL " + "\n\t" + fp.fi.name + "\n\t" + fp.fi.hash);
            this.fph.sendFile(fp);
        }
    }

    public void send(PairNodoFileInfo choice) {
        this.fph.registerFile(choice.fileInfo, choice.nodo);

        ArrayList<Integer> ports = this.fph.getPorts(choice.fileInfo.hash);

        HashMap <Integer, Integer> ppps = new HashMap<Integer, Integer>();

        for(int p : ports)
            ppps.put(p,this.pps);

        FilePull fp = new FilePull(this.idGen.getID(), this.myNode, choice.fileInfo, ppps, null);

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Output output = new Output(bStream);

        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, fp);
        output.close();

        byte[] serializedPing = bStream.toByteArray();

        try {
            DatagramPacket packet = new DatagramPacket(serializedPing, serializedPing.length, InetAddress.getByName(choice.nodo.ip), this.ucp_FilePullHandler);
            (new DatagramSocket()).send(packet);
            System.out.println("ENVIEI O FILEPULL " + "\n\t" + choice.fileInfo.name + "\n\t" + choice.fileInfo.hash);
            this.fph.startReceivers(choice.fileInfo.hash);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run() {
        try{


            byte[] buf;
            DatagramPacket filepull;

            DatagramSocket ds = new DatagramSocket(this.ucp_FilePullHandler);

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
