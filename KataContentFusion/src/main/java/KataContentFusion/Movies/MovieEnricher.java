/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.*;
import KataContentFusion.LocalDb.Repos.ScanNameRepository;

@Component
public class MovieEnricher {
    
    private final ScanNameRepository scanNameRepo;

    public MovieEnricher(
        ScanNameRepository scanNameRepo)
        {
            this.scanNameRepo = scanNameRepo;
        }

    public void Enrich(Integer maxCount) {

        // var scanNamesToEnrich = scanNameRepo.getScanNamesWithoutMovies(maxCount);
        var scanNamesToEnrich = scanNameRepo.getScanNamesWithoutMoviesAgain(PageRequest.of(0, maxCount));
        for(var scanName : scanNamesToEnrich) {
            System.out.println(scanName.name);
        }
    }
}
