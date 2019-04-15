package files;

public class FileInfo {
    public String name;
    public String hash;
    public int numOfFileChunks;
    public long fileSize;

    public FileInfo(){}


    public FileInfo(String name, String hash, int nofc, long fileSize){
        this.name = name;
        this.hash = hash;
        this.numOfFileChunks = nofc;
        this.fileSize = fileSize;
    }
}
