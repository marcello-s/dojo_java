/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.Asset;
import KataContentFusion.LocalDb.Repos.AssetRepository;
import KataContentFusion.LocalDb.Repos.VolumeRepository;
import KataContentFusion.Tools.FFprobeUtil;

@Component
public class AssetResolutionImporter {

    private final VolumeRepository volumeRepository;
    private final AssetRepository assetRepository;

    public AssetResolutionImporter(
        VolumeRepository volumeRepository,
        AssetRepository assetRepository) {
        this.volumeRepository = volumeRepository;
        this.assetRepository = assetRepository;
    }
    
    public void Import(String volume, Integer maxCount) {

        System.out.println("importing resolutions for volume " + volume);
        var volumeId = volumeRepository.findByName(volume).get(0).id;
        var assetsWithoutResolutionInfo = assetRepository.getAssetsWithoutResolutionInfo(volumeId);

        var onlyVideoAssets = assetsWithoutResolutionInfo.stream()
            .filter(asset -> asset.mediaPath.toLowerCase().endsWith(".mkv") 
                || asset.mediaPath.toLowerCase().endsWith(".mp4")
                || asset.mediaPath.toLowerCase().endsWith(".avi")
                || asset.mediaPath.toLowerCase().endsWith(".mpg"))
            .limit(maxCount)
            .toList();

        System.out.println("found " + onlyVideoAssets.size() + " assets without resolution info for volume " + volume);

        for(Asset asset : onlyVideoAssets) {
            System.out.println("importing resolution for asset: " + asset.mediaPath);
            importResolutionInfoForAsset(asset);
        };
    }

    private void importResolutionInfoForAsset(Asset asset) {

        var resolution = FFprobeUtil.GetResolution(asset.mediaPath);
        asset.resolutionX = resolution.width();
        asset.resolutionY = resolution.height();
        assetRepository.save(asset);
    }
}
