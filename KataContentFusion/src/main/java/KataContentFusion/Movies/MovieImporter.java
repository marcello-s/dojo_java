/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.*;

@Component
public class MovieImporter {
    
    private final ScanTypeRepository scanTypeRepo;
    private final AssetTypeRepository assetTypeRepo;
    private final VolumeRepository volumeRepo;
    private final ScanNameRepository scanNameRepo;
    private final AssetRepository assetRepo;
    private final AssetVolumeRepository assetVolumeRepo;

    public MovieImporter(
        ScanTypeRepository scanTypeRepo,
        AssetTypeRepository assetTypeRepo,
        VolumeRepository volumeRepo,
        ScanNameRepository scanNameRepo,
        AssetRepository assetRepo,
        AssetVolumeRepository assetVolumeRepo) {
        this.scanTypeRepo = scanTypeRepo;
        this.assetTypeRepo = assetTypeRepo;
        this.volumeRepo = volumeRepo;
        this.scanNameRepo = scanNameRepo;
        this.assetRepo = assetRepo;
        this.assetVolumeRepo = assetVolumeRepo;
    }

    public void Import(Collection<MovieVolume> movieVolumes) {
        ensureScanType("Movie");
        ensureAssetType("Video");
        ensureAssetType("Subtitle");

        var movieScanType = scanTypeRepo.findByName("Movie").getFirst();
        var assetTypes = assetTypeRepo.findAll();

        for(var v : movieVolumes) {      
            ensureVolume(v.volume());
            var volume = volumeRepo.findByName(v.volume()).getFirst();

            for (var m : v.movies()) {
                ensureScanName(m.scanName, m.collection);
                var scanName = scanNameRepo.findByName(m.scanName).getFirst();

                for(var mediaFile : m.mediaFiles) {
                    var mediaPath = mediaFile.toAbsolutePath().toString();
                    var existingAsset = assetRepo.findByMediaPath(mediaPath);
                    if (existingAsset.size() > 0) {
                        continue;
                    }

                    var assetType = mediaPath.endsWith(".srt")
                    ? assetTypes.get(1)
                    : assetTypes.get(0);

                    var asset = new Asset();
                    asset.scanTypeId = movieScanType;
                    asset.assetTypeId = assetType;
                    asset.scanNameId = scanName;
                    asset.mediaPath = mediaPath;
                    assetRepo.save(asset);

                    var assetVolume = new AssetVolume();
                    assetVolume.volumeId = volume;
                    assetVolume.assetId = asset;
                    assetVolumeRepo.save(assetVolume);
                }
            }
        }

    }

    private void ensureScanType(String name) {
        var scanTypes = scanTypeRepo.findByName(name);        
        if (scanTypes.isEmpty()) {
            var movieScanType = new ScanType();
            movieScanType.name = name;
            scanTypeRepo.save(movieScanType);
        }
    }

    private void ensureAssetType(String name) {
        var assetTypes = assetTypeRepo.findByName(name);
        if (assetTypes.isEmpty()) {
            var assetType = new AssetType();
            assetType.name = name;
            assetTypeRepo.save(assetType);
        } 
    }

    private void ensureVolume(String name) {
        var volumes = volumeRepo.findByName(name);
        if (volumes.isEmpty()) {
            var volume = new Volume();
            volume.name = name;
            volumeRepo.save(volume);
        }
    }

    private void ensureScanName(String name, String collection) {
        var scanNames = scanNameRepo.findByName(name);
        if (scanNames.isEmpty()) {
            var scanName = new ScanName();
            scanName.name = name;
            scanName.collection = collection;
            scanNameRepo.save(scanName);
        }
    }

    /*
    var st1 = new ScriptTracking();
    st1.scriptName = "test1";
    st1.createdAt = LocalDateTime.now();

    var st2 = new ScriptTracking();
    st2.scriptName = "test2";
    st2.createdAt = LocalDateTime.now();

    repository.saveAll(Arrays.asList(st1, st2));
    */            
}
