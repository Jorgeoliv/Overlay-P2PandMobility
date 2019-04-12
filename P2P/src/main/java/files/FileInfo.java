package files;

public class FileInfo {
    public String id;
    //NOME
    //QUANTOS CHUNKS TEM PARA ENVIAR
    //TAMANHO DO FICHEIRO EM BYTES


    public FileInfo(String path, long fileSize){ }

    public FileInfo(String id){
        this.id = id;
    }
}
