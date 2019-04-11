package files;

public class MyFile extends FileInfo{

    public MyFile(){
        super();
    }

    public MyFile(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "MyFile{" +
                "id='" + id + '\'' +
                '}';
    }
}
