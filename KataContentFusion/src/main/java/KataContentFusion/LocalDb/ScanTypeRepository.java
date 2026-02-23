/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanTypeRepository extends JpaRepository<ScanType, Integer> {
    List<ScanType> findByName(String name);
}
