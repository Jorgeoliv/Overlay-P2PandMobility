package files;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class Ficheiro {

    private File file;
    private String nodeID;
    private String fileName;
    private String filePath;
    private int datagramMaxSize;

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
        int maxID = this.numberOfChunks + Integer.MIN_VALUE;
        for(int i = Integer.MIN_VALUE; i < maxID; i++)
            this.missingFileChunks.add(i);

        this.fileSize = 0;
        this.full = false;
        this.fileLock = new ReentrantLock();
    }

    public Ficheiro (String path, String id, String name, int datagramMaxSize){
        this.file = new File(path);
        this.nodeID = id;
        this.fileName = name;
        this.filePath = path;
        this.datagramMaxSize = datagramMaxSize;

        this.fileSize = this.file.length();
        this.numberOfChunks = (int) Math.ceil((double)this.fileSize/(double)datagramMaxSize);


        loadFile();

        this.full = true;
    }

    private void writeFileChunksToFolder(String folder, FileChunk[] fcs, int size){
        int i;
        String folderPath = "NODE_" + this.nodeID + "/" + folder + "/" + this.fileName;
        File ficheiro = new File(folderPath);
        File filePointer;
        String path;

        while((!ficheiro.exists() && !ficheiro.isDirectory()) && !ficheiro.mkdir());

        Path file;
        for(i = 0; i < size; i++){
            try {
                FileChunk fc = fcs[i];
                path = folderPath + "/" + fc.getPlace() + ".filechunk";
                filePointer = new File(path);

                //SO VAI ESCREVER O FICHEIRO SE ELE NAO EXISTIR
                if(!filePointer.exists()) {
                    file = Paths.get(path);
                    Files.write(file, fc.getFileChunk());
                }
            }
                catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFileToFolder(String folder){
        String folderToWritePath = "NODE_" + this.nodeID + "/" + folder + "/" + this.fileName;
        String folderToReadPath = "NODE_" + this.nodeID + "/tmp/" + this.fileName;

        FileChunk filechunk;
        int i = 0;

        try {
            Path p;
            FileOutputStream outputStream = new FileOutputStream(folderToWritePath, true);

            while (i < this.numberOfChunks) {
                p = Paths.get(folderToReadPath + "/" + (i + Integer.MIN_VALUE) + ".filechunk");
                filechunk = new FileChunk(Files.readAllBytes(p), (i + Integer.MIN_VALUE));

                outputStream.write(filechunk.getFileChunk());

                i++;
            }
            outputStream.close();
            System.out.println("FICHEIRO MONTADO COM SUCESSO");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("ERRO AO JUNTAR OS FILECHUNKS");
        }
    }

    public boolean addFileChunks (ArrayList<FileChunk> fcs){
        this.fileLock.lock();

        for(FileChunk fc: fcs) {
            //System.out.println("ID!!! => " + fc.getPlace());
            if(this.missingFileChunks.contains(fc.getPlace())) {
                this.missingFileChunks.remove(new Integer(fc.getPlace()));
                this.numberOfChunksInArray++;
                this.fileSize += fc.getFileChunk().length;
            }
        }

        FileChunk[] aux = fcs.toArray(new FileChunk[0]);

        writeFileChunksToFolder("tmp", aux, fcs.size());

        if(this.numberOfChunksInArray == this.numberOfChunks) {
            this.full = true;
            writeFileToFolder("files");
        }
        this.fileLock.unlock();

        return this.full;
    }

    private FileChunk[] createFileChunks(ArrayList<byte[]> fileAsBytesChunks, int id){
        int noc = fileAsBytesChunks.size();
        int fcID = id - noc + Integer.MIN_VALUE;
        FileChunk[] res = new FileChunk[noc];

        for (int i = 0; i < noc; i++)
            res[i] = new FileChunk(fileAsBytesChunks.get(i), fcID++);

        return res;
    }

    public ArrayList<FileChunk> getFileChunks(int start, int len){
        start += Integer.MIN_VALUE;
        String tmpFolder = "NODE_" + this.nodeID + "/tmp/" + this.fileName;

        File ficheiro = new File (tmpFolder);
        ArrayList<FileChunk> fChunks = null;

        try {
            if(ficheiro.exists() && ficheiro.isDirectory()){
                fChunks = new ArrayList<FileChunk>();

                for(int i = 0; i < len; i++, start++){
                    fChunks.add(new FileChunk(Files.readAllBytes(Paths.get(tmpFolder + "/" + start + ".filechunk")), start));
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return fChunks;
    }

    private void loadFile(){

        try {
            FileInputStream fis = new FileInputStream(this.filePath);

            int i = 0;
            int read = 1;
            ArrayList<byte[]> info = new ArrayList<byte[]>();
            byte[] buffer;
            FileChunk[] fileChunks;

            while(i < this.numberOfChunks){

                for(int j = 0; j < 10000 && i < this.numberOfChunks && read != 0; i++, j++){
                    buffer = new byte[this.datagramMaxSize];
                    read = fis.read(buffer);
                    info.add(buffer);
                }
                fileChunks = createFileChunks(info, i);
                writeFileChunksToFolder("tmp", fileChunks, info.size());
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Integer> getMissingFileChunks(){

        return (ArrayList<Integer>) this.missingFileChunks.clone();
    }

    public ArrayList<FileChunk> getMissingFileChunks(ArrayList<Integer> mfc){

        String tmpFolder = "NODE_" + this.nodeID + "/tmp/" + this.fileName;

        File ficheiro = new File (tmpFolder);
        ArrayList<FileChunk> res = new ArrayList<FileChunk>();

        try {
            if(ficheiro.exists() && ficheiro.isDirectory()){
                FileChunk f;
                for(Integer i : mfc){
                    //System.out.println("LI O MISSING FILE CHUNK " + i);
                    f = new FileChunk(Files.readAllBytes(Paths.get(tmpFolder + "/" + i + ".filechunk")), i);
                    res.add(f);
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

    public int getNumberOfMissingFileChunks(){
        return this.missingFileChunks.size();
    }

    public int getNumberOfChunks(){
        return this.numberOfChunks;
    }

    public long getFileSize(){
        return this.fileSize;
    }
}
