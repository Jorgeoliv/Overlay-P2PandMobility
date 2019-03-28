package network;

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
}
