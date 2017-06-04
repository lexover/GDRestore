import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by lexover on 23.05.17.
 */
class QuickstartTest {
    @Test
    void createDirsTo() {

        Directory dir1 = new Directory();
        dir1.setPath(Quickstart.ROOT_PATH+"/parent/dir1");

        Directory dir2 = new Directory();
        dir2.setPath(Quickstart.ROOT_PATH+"/parent/dir2");

        List<Directory> dirs = new ArrayList<>();
        dirs.add(dir1);
        dirs.add(dir2);
        Quickstart.createDirsInRoot(dirs);

        java.io.File dr1 = new java.io.File(dir1.getPath());
        java.io.File dr2 = new java.io.File(dir2.getPath());
        assertTrue(dr1.exists());
        assertTrue(dr2.exists());

        dr1.delete();
        dr2.delete();
        java.io.File parent = new java.io.File(Quickstart.ROOT_PATH+"/parent");
        parent.delete();
        java.io.File root = new java.io.File(Quickstart.ROOT_PATH);
        root.delete();
    }

    @Test
    void parseRFC3339(){
        assertEquals(LocalDateTime.of(2016, 8, 15, 7, 5, 4),
                Quickstart.fromRFC3339("2016-08-15T07:05:04.000Z"));
    }



}