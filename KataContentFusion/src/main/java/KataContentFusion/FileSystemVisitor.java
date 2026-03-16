/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FileSystemVisitor extends SimpleFileVisitor<Path> {

    public List<FsEntry> entries = new ArrayList<FsEntry>();
    
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            entries.add(new FsEntry(path, false));
        } else {
            System.out.format("unknown path - %s%n", path);
        }

        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exc) {
        entries.add(new FsEntry(path, true));

        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException exc) {
        System.err.println(exc);

        return CONTINUE;
    }
}
