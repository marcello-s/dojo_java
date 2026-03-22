/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb.Repos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import KataContentFusion.LocalDb.Asset;

public interface AssetRepository extends JpaRepository<Asset, Integer> {
    List<Asset> findByMediaPath(String mediaPath);
    Asset findByScanNameIdAndAssetTypeId(Integer scanNameId, Integer assetTypeId);

    @Query("SELECT a FROM Asset a INNER JOIN AssetVolume av ON av.asset.id = a.id LEFT JOIN SubtitleEntry se ON se.asset.id = a.id WHERE av.volume.id = :volumeId AND se.asset.id IS NULL")
    List<Asset> getAssetsWithoutSubtitles(@Param("volumeId") Integer volumeId);

    @Query("SELECT a FROM Asset a INNER JOIN AssetVolume av ON av.asset.id = a.id  WHERE av.volume.id = :volumeId AND a.resolutionX IS NULL AND a.resolutionY IS NULL")
    List<Asset> getAssetsWithoutResolutionInfo(@Param("volumeId") Integer volumeId);
}
