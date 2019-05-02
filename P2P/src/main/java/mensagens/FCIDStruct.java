package mensagens;

public class FCIDStruct{
    public byte order;
    public FCIDStruct[] group;
    public byte[] ids;

    public FCIDStruct(){}

    public FCIDStruct(byte order, FCIDStruct[] group, byte[] ids){
        this.order = order;
        this.group = group;
        this.ids = ids;
    }
}