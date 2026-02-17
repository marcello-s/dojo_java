/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import org.springframework.stereotype.Component;

@Component
public class WorkflowDispatcher implements Dispatching {

    private MovieScanner movieScanner;

    public WorkflowDispatcher(MovieScanner movieScanner) {
        this.movieScanner = movieScanner;
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
        }
    }
}
