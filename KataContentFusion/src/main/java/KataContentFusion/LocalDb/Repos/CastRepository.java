/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb.Repos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import KataContentFusion.LocalDb.Cast;

public interface CastRepository extends JpaRepository<Cast, Integer> {
    List<Cast> findByExternalId(Integer externalId);
}
