/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.Repos.ScriptTrackingRepository;
import KataContentFusion.Movies.MovieEnricher;
import KataContentFusion.Movies.MovieImporter;
import KataContentFusion.Movies.MovieScanner;
import KataContentFusion.Movies.MovieSubtitleImporter;
import KataContentFusion.Movies.MovieVolume;
import KataContentFusion.Movies.ScannedMoviesStore;

@Component
public class WorkflowDispatcher implements Dispatching {

    private final MovieScanner movieScanner;
    private final MovieImporter movieImporter;
    private final MovieSubtitleImporter movieSubtitleImporter;
    private final MovieEnricher movieEnricher;

    public WorkflowDispatcher(
        MovieScanner movieScanner,
        ScriptTrackingRepository repository,
        MovieImporter movieImporter,
        MovieSubtitleImporter movieSubtitleImporter,
        MovieEnricher movieEnricher) {
        this.movieScanner = movieScanner;
        this.movieImporter = movieImporter;
        this.movieSubtitleImporter = movieSubtitleImporter;
        this.movieEnricher = movieEnricher;
    }

    public void Dispatch(String[] args) {
        System.out.println("dispatching..");

        if (args.length == 0) {
            System.out.println("missing arguments: blah");

            return;
        }

        var command = args[0];
        if (command.toLowerCase().equals("scan")) {

            var assets = args[1];
            if (assets.toLowerCase().equals("movies"))
            {
                var volume = args[2];
                var path = args[3];
                var movies = movieScanner.Scan(path);
                var store = new ScannedMoviesStore();
                store.Serialize(new MovieVolume(volume, movies));
            }
        } else if(command.toLowerCase().equals("import")) {
            
            var assets = args[1];
            if (assets.toLowerCase().equals("movies")) {
                var path = args[2];
                var store = new ScannedMoviesStore();
                var moviesVolumes = store.Deserialize(path);
                movieImporter.Import(moviesVolumes);
            } else if (assets.toLowerCase().equals("subtitles")) {
                var volume = args[2];
                var maxCount = args[3];
                movieSubtitleImporter.Import(volume, Integer.parseInt(maxCount));
            }
        } else if(command.toLowerCase().equals("enrich")) {

            var assets = args[1];
            if (assets.toLowerCase().equals("movies")) {
                var maxCount = args[2];
                movieEnricher.Enrich(Integer.parseInt(maxCount));
            }
        } else {
            System.out.println("command not found");
        }
    }
}
