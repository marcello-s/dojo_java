/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import KataContentFusion.FileSystemVisitor;
import KataContentFusion.FilesUtil;
import KataContentFusion.FsEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieScannerTest {

    @Mock
    private FilesUtil filesUtil; // Mocking the dependency

    @Mock
    private FileSystemVisitor fsVisitor; // Mocking the dependency

    private MovieScanner movieScanner;

    @BeforeEach
    void setUp() {
        // Initialize the class with the mocked visitor
        movieScanner = new MovieScanner(filesUtil, fsVisitor);
        
        // Since your code accesses 'fsVisitor.entries' directly as a field, 
        // we ensure the mock has an initialized list to work with.
        fsVisitor.entries = new ArrayList<>();
    }

    @Test
    void testScan_LogicWithMockedEntries() {
        // 1. ARRANGE: Define manual paths to simulate a file tree walk
        // Note: These paths don't need to exist on your hard drive!
        Path collectionPath = Paths.get("/movies/[Rambo]");
        Path moviePath = collectionPath.resolve("First Blood (1982)");
        Path videoFilePath = moviePath.resolve("rambo.mp4");

        // Simulate the entries that the Visitor would have "found"
        fsVisitor.entries.add(new FsEntry(collectionPath, true));  // Directory [Rambo]
        fsVisitor.entries.add(new FsEntry(moviePath, true));       // Directory First Blood
        fsVisitor.entries.add(new FsEntry(videoFilePath, false));  // File rambo.mp4

        // 2. ACT
        // We pass any string because Files.walkFileTree will be called on the mock,
        // and even if it fails/does nothing, the logic runs on our 'fsVisitor.entries'
        Collection<Movie> results = movieScanner.Scan("/any/path");

        // 3. ASSERT
        assertEquals(1, results.size());
        Movie movie = results.iterator().next();
        
        assertEquals("First Blood (1982)", movie.scanName);
        // Verify that the collection matching logic worked
        // (Assuming Movie has a getter or field for the collection name)
        assertTrue(movie.collection.contains("Rambo"));
    }

    @Test
    void testScan_MultipleFilesGrouping() {
        // ARRANGE: Simulate two files in the same movie folder
        Path moviePath = Paths.get("/movies/The Matrix");
        Path file1 = moviePath.resolve("matrix.mkv");
        Path file2 = moviePath.resolve("matrix-trailer.mp4");

        fsVisitor.entries.add(new FsEntry(moviePath, true));
        fsVisitor.entries.add(new FsEntry(file1, false));
        fsVisitor.entries.add(new FsEntry(file2, false));

        // ACT
        Collection<Movie> results = movieScanner.Scan(".");

        // ASSERT
        assertEquals(1, results.size(), "Should group both files into one Movie object");
        Movie movie = results.iterator().next();
        assertEquals(2, movie.mediaFiles.size(), "Should have found 2 media files for this movie");
    }    
}
