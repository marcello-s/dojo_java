/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.springframework.stereotype.Component;

@Component
public class MovieScanner {
    
    public Collection<Movie> Scan(String path) {
        System.out.println("scanning Movies");

        var fsVisitor = new FileSytemVisitor();
        var absolutePath = Paths.get(path).toAbsolutePath();
        try {
            Files.walkFileTree(absolutePath, fsVisitor);

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
        } catch (IOException e) {
            e.printStackTrace();
            
            return new ArrayList<>();
        }
    }
}
