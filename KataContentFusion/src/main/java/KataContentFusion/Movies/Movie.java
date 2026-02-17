/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Movie {

    public final String scanName;
    public final Path path;
    public List<Path> mediaFiles = new ArrayList<Path>();

    public String collection = "";
    
    @JsonCreator
    public Movie(@JsonProperty("path") Path path, @JsonProperty("scanName") String scanName) {
        this.path = path;
        this.scanName = scanName;
    }

    public boolean AssignCollectionMatch(String collectionName) {
        if (path.toString().toLowerCase().contains(collectionName.toLowerCase())) {
            collection = collectionName;

            return true;
        }

        return false;
    }
}
