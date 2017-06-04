import com.google.api.services.drive.model.File;

/**
 * Created by lexover on 22.05.17.
 */
public class Directory {
    private File file;
    private String path;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
