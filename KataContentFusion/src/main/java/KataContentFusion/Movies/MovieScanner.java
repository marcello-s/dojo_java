/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.springframework.stereotype.Component;

import KataContentFusion.FileSystemVisitor;
import KataContentFusion.FilesUtil;
import KataContentFusion.FsEntry;

@Component
public class MovieScanner {

    private final FilesUtil filesUtil;
    private final FileSystemVisitor fsVisitor;

    public MovieScanner(
        FilesUtil filesUtil, 
        FileSystemVisitor fsVisitor) {
        this.filesUtil = filesUtil;
        this.fsVisitor = fsVisitor;
    }
    
    public Collection<Movie> Scan(String path) {
        System.out.println("scanning Movies");

        filesUtil.walkFileTree(path, fsVisitor);

        // first pass: identify collections and movies
        var collectionNames = new ArrayList<String>();
        var movieLookup = new HashMap<String, Movie>();
        for(FsEntry entry : fsVisitor.entries) {
            if(entry.isDirectory())
            {                    
                var name = entry.path().getName(entry.path().getNameCount() -1).toString();                    
                if (name.startsWith("[") && name.endsWith("]")) {
                    collectionNames.add(name);
                }

            } else {                    
                var parent = entry.path().getParent();
                var scanName = parent.getName(parent.getNameCount() -1);

                if (movieLookup.containsKey(scanName.toString()))
                {
                    var movie = movieLookup.get(scanName.toString());
                    movie.mediaFiles.add(entry.path());
                } else {
                    var newMovie = new Movie(entry.path(), scanName.toString());
                    newMovie.mediaFiles.add(entry.path());
                    movieLookup.put(scanName.toString(), newMovie);
                }
            }
        }

        // second pass: assign movies to collections
        for(Movie movie : movieLookup.values()) {

            // assume there are only few collections and break if match found
            for(String collectionName : collectionNames) {
                if (movie.AssignCollectionMatch(collectionName)){
                    break;
                }
            }
        }

        return movieLookup.values();
    }
}
