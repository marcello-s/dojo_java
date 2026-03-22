/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.SubtitleEntry;
import KataContentFusion.LocalDb.Repos.AssetRepository;
import KataContentFusion.LocalDb.Repos.MovieRepository;
import KataContentFusion.LocalDb.Repos.ScanNameMovieRepository;
import KataContentFusion.LocalDb.Repos.SubtitleEntryRepository;
import KataContentFusion.SubTitles.Subtitle;
import KataContentFusion.SubTitles.SubtitleExtended;
import KataContentFusion.SubTitles.SubtitleType;
import KataContentFusion.Tools.FFmpegUtil;

@Component
public class MovieSegmenter {

    private static final Integer AssetTypeIdMovie = 1;
    private static final Integer AssetTypeIdSubtitle = 2;

    private final ScanNameMovieRepository scanNameMovieRepository;
    private final MovieRepository movieRepository;
    private final AssetRepository assetRepository;
    private final SubtitleEntryRepository subtitleEntryRepository;

    public MovieSegmenter(
        ScanNameMovieRepository scanNameMovieRepository,
        MovieRepository movieRepository,
        AssetRepository assetRepository,
        SubtitleEntryRepository subtitleEntryRepository) {
        this.scanNameMovieRepository = scanNameMovieRepository;
        this.movieRepository = movieRepository;
        this.assetRepository = assetRepository;
        this.subtitleEntryRepository = subtitleEntryRepository;
    }

    public void CreateSegement(Integer movieId) {

        var scanNameMovies = scanNameMovieRepository.findScanNameMovieByMovieId(movieId);
        if (scanNameMovies.size() == 0) {
            System.out.println("no scan name movies found for movie id: " + movieId);
            return;
        }

        var movie = movieRepository.findById(movieId);
        if (movie == null) {
            System.out.println("no movie found for id: " + movieId);
            return;
        }
        
        var scanNameMovie = scanNameMovies.get(0);
        var assetSubtitle = assetRepository.findByScanNameIdAndAssetTypeId(scanNameMovie.scanName.id, AssetTypeIdSubtitle);
        if (assetSubtitle == null) {
            System.out.println("no asset found for scan name id: " + scanNameMovie.scanName.id);
            return;
        }

        var subtitleEntries = subtitleEntryRepository.findByAssetId(assetSubtitle.id);
        if (subtitleEntries.size() == 0) {
            System.out.println("no subtitle entries found for asset id: " + assetSubtitle.id);
            return;
        }

        var subtitleExtendedList = CreateSubtitleExtendedList(subtitleEntries);
        var runtimeInMinutes = movie.map(m -> m.runtime).orElse(0);
        var chunkStartTimes = CreateChunkStartTimes(runtimeInMinutes, 10);
        var nonDialogSubtitles = GetSubtitles(
            subtitleExtendedList, 
            SubtitleType.NonDialog, 
            chunkStartTimes, 
            3000,
            120000, 
            360);
        
        nonDialogSubtitles.forEach(subtitle -> {
            System.out.println("from: " + subtitle.subtitle().timeFrom() + " to: " + subtitle.subtitle().timeTo() + " duration: " + subtitle.getDurationInMilliSesonds());
        });
        

        var assetMovie = assetRepository.findByScanNameIdAndAssetTypeId(scanNameMovie.scanName.id, AssetTypeIdMovie);
        if (assetMovie == null) {
            System.out.println("no movie asset found for scan name id: " + scanNameMovie.scanName.id);
            return;
        }
        System.out.println("movie asset: " + assetMovie.mediaPath);

        var workingPath = "C:/Temp/cf/";
        // skip first 2 non dialog segments, because they often contain opening credits and are not suitable for documetary style segments
        // var nonDialogSubtitlesSkipped = nonDialogSubtitles.stream().skip(2).toList(); 
        CreateSegmentsFromSubtitles(nonDialogSubtitles, assetMovie.mediaPath, workingPath);
    }

    private List<SubtitleExtended> CreateSubtitleExtendedList(List<SubtitleEntry> subtitleEntries) {
        var result = new ArrayList<SubtitleExtended>();
        Long lastTimeTo = 0L;

        for (var subtitleEntry : subtitleEntries) {

            var subtitleNonDialog = new Subtitle(
                0, 
                Instant.ofEpochMilli(lastTimeTo), 
                Instant.ofEpochMilli(subtitleEntry.timeFrom), 
                "non dialog");
            var subtitleExtendedNonDialog = new SubtitleExtended(subtitleNonDialog, SubtitleType.NonDialog);
            result.add(subtitleExtendedNonDialog);

            var subtitleDialog = new Subtitle(
                subtitleEntry.sequenceNumber, 
                Instant.ofEpochMilli(subtitleEntry.timeFrom), 
                Instant.ofEpochMilli(subtitleEntry.timeTo), subtitleEntry.text);
            var subtitleExtendedDialog = new SubtitleExtended(subtitleDialog, SubtitleType.Dialog);
            result.add(subtitleExtendedDialog);

            lastTimeTo = subtitleEntry.timeTo;
        }

        return result;
    }

    private List<Integer> CreateChunkStartTimes(Integer runtimeInMinutes, Integer numberOfChunks) {
        var result = new ArrayList<Integer>();
        var runtimeInSeconds = runtimeInMinutes * 60;
        var chunkDurationInSeconds = runtimeInSeconds / numberOfChunks;
        
        for (int i = 0; i < numberOfChunks; i++) {
            result.add(i * chunkDurationInSeconds);
        }

        return result.stream().skip(1).toList();
    }   

    private List<SubtitleExtended> GetSubtitles(
        List<SubtitleExtended> subtitlesExtended,
        SubtitleType subtitleType, 
        List<Integer> chunkStartTimes,
        Integer minDurationMilliSeconds,
        Integer maxDurationMilliSeconds, 
        Integer maxTotalDurationSeconds) {

        var result = new ArrayList<SubtitleExtended>();
        var maxTotalDurationInMilliSeconds = maxTotalDurationSeconds * 1000;
        var accumulatedDuration = 0L;
        
        for (var i = 0; i < chunkStartTimes.size() - 1; i++) {
            final int index = i;
            var chunkStartTime = chunkStartTimes.get(index);
            var chunkStartTimeInMilliSeconds = chunkStartTime * 1000;
       
            var subtitleExtended = subtitlesExtended.stream()
                .filter(subtitle -> subtitle.subtitle().timeFrom().toEpochMilli() >= chunkStartTimeInMilliSeconds
                    && subtitle.subtitle().timeFrom().toEpochMilli() < chunkStartTimes.get(index + 1) * 1000)
                .filter(subtitle -> subtitle.subtitleType() == subtitleType)
                .filter(subtitle -> subtitle.getDurationInMilliSesonds() >= minDurationMilliSeconds 
                    && subtitle.getDurationInMilliSesonds() <= maxDurationMilliSeconds)
                .sorted(Comparator.comparing(subtitle -> subtitle.subtitle().timeFrom()))
                .limit(2)
                .toList();

            for (var subtitle : subtitleExtended) {
                accumulatedDuration += subtitle.getDurationInMilliSesonds();
                if (accumulatedDuration < maxTotalDurationInMilliSeconds) {
                    result.add(subtitle);
                } else {
                    break;
                }
            }        
        }

        return result;
    }

    private void CreateSegmentsFromSubtitles(List<SubtitleExtended> subtitlesExtended, String mediaPath, String workingPath) {
        var segmentIndex = 1;

        for (var subtitleExtended : subtitlesExtended) {
            var outputPath = workingPath + "rawclip_" + segmentIndex + ".mp4";
            CreateSegment(
                mediaPath, 
                outputPath, 
                subtitleExtended.subtitle().timeFrom().toEpochMilli(), 
                subtitleExtended.subtitle().timeTo().toEpochMilli());
            segmentIndex++;
        }
    }

    private void CreateSegment(String mediaPath, String outputPath, Long timeFrom, Long timeTo) {
        FFmpegUtil.ExtractSegment(mediaPath, outputPath, timeFrom, timeTo);
    }
}
