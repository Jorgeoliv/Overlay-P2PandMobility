package network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        Random rand = new Random();

        String id = this.getID();
        String date = "" + System.currentTimeMillis();
        String nounce = "" + rand.nextInt();

        System.out.println(id + " " + date + " " + nounce);

        String toHash = id + date + nounce;

        MessageDigest digest = null;
        byte[] encodedhash = null;
        String hash = "ola";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            encodedhash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            hash = new String(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
