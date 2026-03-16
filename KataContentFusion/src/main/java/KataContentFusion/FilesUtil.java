/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FilesUtil {

    private static final Logger log = LoggerFactory.getLogger(FilesUtil.class);
    
    public void walkFileTree(String path, FileSystemVisitor visitor) {

        var absolutePath = Paths.get(path).toAbsolutePath();

        try {
            Files.walkFileTree(absolutePath, visitor);
        } catch (Exception e) {
            log.error("error walking file tree", e);
        }
    }
}
