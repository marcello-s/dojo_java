/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb.Repos;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

import KataContentFusion.LocalDb.ScanName;

public interface ScanNameRepository extends JpaRepository<ScanName, Integer> {
    List<ScanName> findByName(String name);

    // with nativeQuery=true the query should include actual table and column names!
    /*
    @Query(value = "SELECT sn.* FROM ScanName sn LEFT JOIN ScanNameMovie snm ON sn.Id = snm.ScanNameId WHERE snm.ScanNameId IS NULL LIMIT :maxCount", nativeQuery = true)
    List<ScanName> getScanNamesWithoutMovies(@Param("maxCount") Integer maxCount);
    */

    // refer to @entity attribute names, not the table definitions!
    // with this data model the relation is by entity. Remove 'Id' from the field name
    @Query("SELECT sn FROM ScanName sn LEFT JOIN ScanNameMovie snm ON sn.id = snm.scanName.id WHERE snm.scanName.id IS NULL")
    List<ScanName> getScanNamesWithoutMoviesAgain(Pageable peagable);    
}
