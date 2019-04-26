package files;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;


public class Ficheiro {

    private File file;
    private byte [] fileAsBytes;
    private FileChunk [] fileChunks;

    private int numberOfChunks;
    private int numberOfChunksInArray;
    private long fileSize;

    private boolean full;

    private ReentrantLock fileLock;

    public Ficheiro (int numberOfChunks){
        this.numberOfChunks = numberOfChunks;
        this.numberOfChunksInArray = 0;
        this.fileChunks = new FileChunk[numberOfChunks];
        this.fileSize = 0;
        this.full = false;
        this.fileLock = new ReentrantLock();
    }

    public Ficheiro (String path, long datagramMaxSize){
        this.file = new File(path);
        String p = this.file.getAbsolutePath();

        this.fileSize = this.file.length();
        this.fileAsBytes = new byte[(int) this.fileSize];
        try{
            this.fileAsBytes = Files.readAllBytes(Paths.get(path));
        }
        catch(Exception e){e.printStackTrace();}
        this.numberOfChunks = (int) Math.ceil((double)this.fileSize/(double)datagramMaxSize);

        byte [][] fileAsBytesChunks = separate(numberOfChunks, datagramMaxSize);
        this.fileChunks = createFileChunks(numberOfChunks, fileAsBytesChunks);
        this.full = true;
    }

    public boolean addFileChunks (ArrayList<FileChunk> fcs){
        this.fileLock.lock();

        for(FileChunk fc: fcs) {
            this.fileChunks[fc.getPlace()] = fc;
            this.numberOfChunksInArray++;
            this.fileSize += fc.getFileChunk().length;
        }
        if(this.numberOfChunksInArray == this.numberOfChunks)
            this.full = true;
        fileReconstructor(new ArrayList<FileChunk>(Arrays.asList(this.fileChunks)));

        this.fileLock.unlock();

        return this.full;
    }

    private FileChunk[] createFileChunks(int noc, byte[][] fileAsBytesChunks){
        FileChunk[] res = new FileChunk[noc];

        for (int i = 0; i < noc; i++)
            res [i] = new FileChunk(fileAsBytesChunks[i], i);

        return res;
    }

    public long getFileSize(){
        return this.fileSize;
    }

    public File getFile(){
        return this.file;
    }

    public FileChunk[] getFileChunks(){
        return this.fileChunks;
    }

    public int getNumberOfChunks(){
        return this.numberOfChunks;
    }

    private byte[][] separate(int numberOfChunks, long datagramMaxSize){

        byte[][] res = new byte [this.numberOfChunks][];
        int i, j = 0;
        long count = 0;
        long packetSize, a;

        while (count < this.fileSize){
            if ((a = this.fileSize - count) > datagramMaxSize)
                packetSize = datagramMaxSize;
            else
                packetSize = a;

            res[j] = new byte[(int)packetSize];

            for (i = 0; i < packetSize; i++, count++)
                res[j][i] = this.fileAsBytes[(int)count];

            j++;
        }

        return res;
    }

    public boolean isFull(){
        return this.full;
    }

    private void fileReconstructor(ArrayList<FileChunk> fc){

        int i, tam = fc.size();
        int fileChunks_tam, fileAsBytes_tam;
        byte [] c;

        for (i = 0; i < tam; i++){
            fileChunks_tam = fc.get(i).getFileChunk().length;
            if (i == 0)
                fileAsBytes_tam = 0;
            else
                fileAsBytes_tam = this.fileAsBytes.length;

            c = new byte[fileChunks_tam + fileAsBytes_tam];
            if (i != 0)
                System.arraycopy(this.fileAsBytes, 0, c, 0, fileAsBytes_tam);
            System.arraycopy(fc.get(i).getFileChunk(), 0, c, fileAsBytes_tam, fileChunks_tam);
            this.fileAsBytes = c;
        }
    }

    public boolean getFull(){
        return this.full;
    }

    public void print(){
        System.out.println(new String(this.fileAsBytes));
    }
}
