/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb.Repos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import KataContentFusion.LocalDb.Volume;

public interface VolumeRepository extends JpaRepository<Volume, Integer> {
    List<Volume> findByName(String name);
}
