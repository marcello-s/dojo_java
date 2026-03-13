/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.SubtitleEntry;
import KataContentFusion.LocalDb.Repos.AssetRepository;
import KataContentFusion.LocalDb.Repos.SubtitleEntryRepository;
import KataContentFusion.LocalDb.Repos.VolumeRepository;
import KataContentFusion.SubTitles.SrtReader;

@Component
public class MovieSubtitleImporter {

    private final VolumeRepository volumeRepository;
    private final AssetRepository assetRepository;
    private final SubtitleEntryRepository subtitleEntryRepository;
    private final SrtReader srtReader;
    
    public MovieSubtitleImporter(
        VolumeRepository volumeRepository,
        AssetRepository assetRepository, 
        SubtitleEntryRepository subtitleEntryRepository,
        SrtReader srtReader) {
        this.volumeRepository = volumeRepository;
        this.assetRepository = assetRepository;
        this.subtitleEntryRepository = subtitleEntryRepository;
        this.srtReader = srtReader;
    }

    public void Import(String volume,Integer maxCount) {

        System.out.println("importing subtitles for volume " + volume);
        var volumeId = volumeRepository.findByName(volume).get(0).id;
        var assetsWithoutSubtitles = assetRepository.getAssetsWithoutSubtitles(volumeId);        
        
        var onlySubtitleAssets = assetsWithoutSubtitles.stream()
            .filter(asset -> asset.mediaPath.toLowerCase().endsWith(".srt"))
            .limit(maxCount)
            .toList();
        System.out.println("found " + onlySubtitleAssets.size() + " assets without subtitles for volume " + volume);
        onlySubtitleAssets.forEach(asset -> System.out.println("asset: " + asset.mediaPath));

        for (var asset : onlySubtitleAssets) {
            var subtitles = srtReader.readSrtFile(asset.mediaPath); 
            var subTitleEntries = new ArrayList<SubtitleEntry>();
            for (var subtitle : subtitles) {
                var entry = new SubtitleEntry();
                entry.asset = asset;
                entry.sequenceNumber = subtitle.sequenceNumber();
                entry.timeFrom = subtitle.timeFrom().toEpochMilli();
                entry.timeTo = subtitle.timeTo().toEpochMilli();
                entry.text = subtitle.text();
                subTitleEntries.add(entry);
            }

            subtitleEntryRepository.saveAll(subTitleEntries);
            System.out.println("imported " + subtitles.size() + " subtitles for asset " + asset.mediaPath);
        }        
    }
}
