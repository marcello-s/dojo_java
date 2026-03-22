/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb.Repos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import KataContentFusion.LocalDb.ScanNameMovie;

public interface ScanNameMovieRepository extends JpaRepository<ScanNameMovie, Integer> {
    List<ScanNameMovie> findByScanNameIdAndMovieId(Integer scanNameId, Integer movieId);    

    List<ScanNameMovie> findScanNameMovieByMovieId(Integer movieId);
}
