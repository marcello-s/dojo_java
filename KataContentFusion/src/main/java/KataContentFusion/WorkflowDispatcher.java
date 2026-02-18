/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import org.springframework.stereotype.Component;

import KataContentFusion.MovieDb.MovieDbClient;
import KataContentFusion.Movies.MovieScanner;
import KataContentFusion.Movies.MovieVolume;
import KataContentFusion.Movies.ScannedMoviesStore;

@Component
public class WorkflowDispatcher implements Dispatching {

    private MovieScanner movieScanner;
    private MovieDbClient movieDbClient;

    public WorkflowDispatcher(MovieScanner movieScanner, MovieDbClient movieDbClient) {
        this.movieScanner = movieScanner;
        this.movieDbClient = movieDbClient;
    }

    public void Dispatch(String[] args) {
        System.out.println("dispatching..");

        if (args.length == 0) {
            System.out.println("missing arguments: blah");

            return;
        }

        var command = args[0];
        var assets = args[1];
        var volume = args[2];
        var path = args[3];
        System.out.printf("%s - %s - %s - %s%n", command, assets, volume, path);

        if (command.toLowerCase().equals("scan")) {
            if (assets.toLowerCase().equals("movies"))
            {
                var movies = movieScanner.Scan(path);
                var store = new ScannedMoviesStore();
                store.Serialize(new MovieVolume(volume, movies));
            }
        } else {
            /*
            var store = new ScannedMoviesStore();
            var movieVolumes = store.Deserialize();
            for(var v : movieVolumes) {
                for (var m : v.movies()) {
                    System.out.println(m.scanName);
                }
            }
            */

            var movieResponse = movieDbClient.getMovieDetail(2163);
            System.out.println(movieResponse);
        }
    }
}
