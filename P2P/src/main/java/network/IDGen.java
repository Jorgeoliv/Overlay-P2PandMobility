package network;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

public class IDGen {
    private int tam;
    private String chars;

    public IDGen(int tam){
        this.tam = tam;
        this.chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz!#$%&/()=?»«'}][{§£@|*-+.:,;_<>";

    }
    public String getID() {

        StringBuilder sb = new StringBuilder(this.tam);

        for (int i = 0; i < this.tam; i++) {

            int index = (int) (this.chars.length() * Math.random());

            sb.append(this.chars.charAt(index));
        }

        return sb.toString();
    }

    public String getNodeID(){
        String id = null;

        while(id == null)
            id = this.newID();

        try {
            File idFolder = new File("NODE_" + id);
            File tmp = new File("NODE_" + id + "/tmp");
            File files = new File("NODE_" + id + "/files");

            while ((!idFolder.exists() || !idFolder.isDirectory()) && !idFolder.mkdir());

            while((!tmp.exists() || !tmp.isDirectory()) && !tmp.mkdir());
            while((!files.exists() || !files.isDirectory()) && !files.mkdir());
        }
        catch (Exception e){
            e.printStackTrace();
        }

        /*try {
            String macAddress = null;
            Enumeration<NetworkInterface> nI = NetworkInterface.getNetworkInterfaces();
            while(macAddress == null || !nI.hasMoreElements())
                macAddress = new String((byte[]) nI.nextElement().getHardwareAddress());

            File macFolder = new File(macAddress);

            if (!macFolder.exists() || !macFolder.isDirectory()) {
                macFolder.mkdir();

                 while (id == null)
                     id = newID(macAddress);

            }
            else {

                File f = new File(macAddress + "/config");
                if (f.exists() && !f.isDirectory()) {

                    Path path = Paths.get(macAddress + "/config");

                    byte[] fileContents = null;

                    fileContents = Files.readAllBytes(path);

                    if (fileContents != null)
                        id = new String(fileContents);
                    else
                        System.out.println("ERRO NA LEITURA DO ID DO NODO");
                }
                else{
                    while (id == null)
                        id = newID(macAddress);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
        System.out.println("NEW NODE ID => " + id);
        return id;
    }

    private String newID(){//String macAddress){
        String hash = null;
        try {
            Random rand = new Random();

            String id = this.getID();
            String date = "" + System.currentTimeMillis();
            String nounce = "" + rand.nextInt();

            String toHash = id + date + nounce;

            MessageDigest digest = null;
            byte[] encodedhash = null;

            digest = MessageDigest.getInstance("SHA-256");
            encodedhash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            hash = new String(encodedhash);

            /*if (hash != null) {
                Path file = Paths.get(macAddress + "/config");
                Files.write(file, encodedhash);
            }*/
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return hash;
    }
}
