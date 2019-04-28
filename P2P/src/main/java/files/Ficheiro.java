package files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;


public class Ficheiro {

    private File file;
    private String nodeID;
    private String fileName;

    private byte [] fileAsBytes;
    private FileChunk [] fileChunks;

    private int numberOfChunks;
    private int numberOfChunksInArray;
    private ArrayList<Integer> missingFileChunks;
    private long fileSize;

    private boolean full;

    private ReentrantLock fileLock;

    public Ficheiro (int numberOfChunks, String id, String name){
        this.nodeID = id;
        this.fileName = name;

        this.numberOfChunks = numberOfChunks;
        this.numberOfChunksInArray = 0;
        this.missingFileChunks = new ArrayList<Integer>();
        for(int i = 0; i < this.numberOfChunks; i++)
            this.missingFileChunks.add(i);

        this.fileChunks = new FileChunk[numberOfChunks];
        this.fileSize = 0;
        this.full = false;
        this.fileLock = new ReentrantLock();
    }

    public Ficheiro (String path, String id, String name, long datagramMaxSize){
        this.file = new File(path);
        this.nodeID = id;
        this.fileName = name;

        this.fileSize = this.file.length();
        this.fileAsBytes = new byte[(int) this.fileSize];
        try{
            this.fileAsBytes = Files.readAllBytes(Paths.get(path));
        }
        catch(Exception e){e.printStackTrace();}
        this.numberOfChunks = (int) Math.ceil((double)this.fileSize/(double)datagramMaxSize);

        byte [][] fileAsBytesChunks = separate(numberOfChunks, datagramMaxSize);
        this.fileChunks = createFileChunks(numberOfChunks, fileAsBytesChunks);

        try {
            writeFileChunksToFolder("uploads", this.fileChunks, this.numberOfChunks);
            cleanSpace();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        this.full = true;
    }

    private void cleanSpace(){
        this.fileAsBytes = null;
        this.fileChunks = null;
    }

    private void writeFileChunksToFolder(String folder, FileChunk[] fcs, int size){
        int i;
        String uploadPath = "NODE_" + this.nodeID + "/" + folder + "/" + this.fileName;
        File ficheiro = new File(uploadPath);

        if(!ficheiro.exists() && !ficheiro.isDirectory()){
            ficheiro.mkdir();
        }

        Path file;
        for(i = 0; i < size; i++){
            try {
                FileChunk fc = fcs[i];
                file = Paths.get(uploadPath + "/" + fc.getPlace() + ".filechunk");
                Files.write(file, fc.getFileChunk());
            }
                catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFileToFolder(String folder){
        String folderPath = "NODE_" + this.nodeID + "/" + folder;
        File ficheiro = new File(folderPath);

        try {
            Path file = Paths.get(folderPath + "/" + this.fileName);
            Files.write(file, this.fileAsBytes);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean addFileChunks (ArrayList<FileChunk> fcs){
        this.fileLock.lock();

        for(FileChunk fc: fcs) {
            this.fileChunks[fc.getPlace()] = fc;
            this.missingFileChunks.remove(new Integer(fc.getPlace()));
            this.numberOfChunksInArray++;
            this.fileSize += fc.getFileChunk().length;
        }

        if(this.numberOfChunksInArray == this.numberOfChunks) {
            this.full = true;
            fileReconstructor(new ArrayList<FileChunk>(Arrays.asList(this.fileChunks)));
            writeFileToFolder("files");
            cleanSpace();
        }
        this.fileLock.unlock();

        FileChunk[] aux = fcs.toArray(new FileChunk[0]);

        writeFileChunksToFolder("/downloads/", aux, fcs.size());

        return this.full;
    }

    private FileChunk[] createFileChunks(int noc, byte[][] fileAsBytesChunks){
        FileChunk[] res = new FileChunk[noc];

        for (int i = 0; i < noc; i++)
            res[i] = new FileChunk(fileAsBytesChunks[i], i);

        return res;
    }

    public long getFileSize(){
        return this.fileSize;
    }

    public FileChunk[] getFileChunks(){
        // IR BUSCAR AO FOLDER UPLOAD
        String uploadFolder = "NODE_" + this.nodeID + "/uploads/" + this.fileName;

        File ficheiro = new File (uploadFolder);
        FileChunk [] fChunks = null;

        try {
            if(ficheiro.exists() && ficheiro.isDirectory()){
                fChunks = new FileChunk[this.numberOfChunks];
                int i;

                for(i = 0; i < this.numberOfChunks; i++){
                    fChunks[i] = new FileChunk(Files.readAllBytes(Paths.get(uploadFolder + "/" + i + ".filechunk")), i);
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return fChunks;
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

    private void fileReconstructor(ArrayList<FileChunk> fc){

        int i, tam = fc.size(), auxTam = 0;
        this.fileAsBytes = new byte[Math.toIntExact(fileSize)];
        byte [] arrayPointer;

        for (i = 0; i < tam; i++){

            arrayPointer = fc.get(i).getFileChunk();
            System.arraycopy(arrayPointer, 0, this.fileAsBytes, auxTam , arrayPointer.length);
            auxTam += arrayPointer.length;
        }
    }

    public ArrayList<Integer> getMissingFileChunks(){
        return this.missingFileChunks;
    }

    public FileChunk[] getMissingFileChunks(ArrayList<Integer> mfc){

        String uploadFolder = "NODE_" + this.nodeID + "/uploads/" + this.fileName;

        File ficheiro = new File (uploadFolder);
        FileChunk[] res = null;

        try {
            if(ficheiro.exists() && ficheiro.isDirectory()){
                int tam = mfc.size();
                res = new FileChunk[tam];
                int j = 0;
                for(Integer i : mfc){
                    res[j++] = new FileChunk(Files.readAllBytes(Paths.get(uploadFolder + "/" + i + ".filechunk")), i);
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean getFull(){
        return this.full;
    }

    public String getFileName(){
        return this.fileName;
    }
}
