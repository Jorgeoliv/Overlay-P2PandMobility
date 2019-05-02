package mensagens;

public class FCIDStruct{
    public int referenceID;
    public byte[] toAdd;
    public byte[] inc;

    public FCIDStruct(){}

    public FCIDStruct(int referenceID , byte[] toAdd, byte[] inc){
        this.referenceID = referenceID ;
        this.toAdd = toAdd;
        this.inc = inc;
    }
}