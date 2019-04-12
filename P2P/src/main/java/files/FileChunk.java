package files;

public class FileChunk {


    private byte[] fileChunk;
    private int place;

    public FileChunk(){}

    public FileChunk(byte[] data, int p){
        this.fileChunk = data;
        this.place = p;
    }

    public byte[] getFileChunk(){
        return this.fileChunk;
    }

    public int getPlace(){
        return this.place;
    }

}
