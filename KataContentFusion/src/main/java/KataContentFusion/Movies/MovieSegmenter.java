/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import KataContentFusion.Tools.FFprobeUtil;
import KataContentFusion.Tools.SceneChange;

@Component
public class MovieSegmenter {

    private static final String WorkingPath = "C:/Temp/cf/";
    private static final Integer AssetTypeIdMovie = 1;
    private static final Integer AssetTypeIdSubtitle = 2;
    private static final String RawClipFilename = "rawclip_";
    private static final String ClipFilename = "clip_";
    private static final String NormalizedClipFilename = "nclip_";
    private static final Double ClipLimitTime = 3000.0;
    private static final Integer ResolutionX = 1920;
    private static final Integer ResolutionY = 1080;
    private static final Integer Fps = 30;

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

    public void CreateSegment(Integer movieId) {

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

        var subtitleExtendedList = createSubtitleExtendedList(subtitleEntries);
        var runtimeInMinutes = movie.map(m -> m.runtime).orElse(0);
        var chunkStartTimes = createChunkStartTimes(runtimeInMinutes, 10);
        var nonDialogSubtitles = getSubtitles(
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

        var movieClipPath = ensureMovieClipPath(WorkingPath, movieId);
        // skip first 2 non dialog segments, because they often contain opening credits and are not suitable for documetary style segments
        // var nonDialogSubtitlesSkipped = nonDialogSubtitles.stream().skip(2).toList(); 
        var rawClips = createSegmentsFromSubtitles(nonDialogSubtitles, assetMovie.mediaPath, movieClipPath);
        var sceneClips = createSceneClips(rawClips, movieClipPath);        
        var normalizedClips = normalizeSceneClips(sceneClips, movieClipPath, ResolutionX, ResolutionY, Fps);
    }

    private List<SubtitleExtended> createSubtitleExtendedList(List<SubtitleEntry> subtitleEntries) {
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

    private List<Integer> createChunkStartTimes(Integer runtimeInMinutes, Integer numberOfChunks) {
        var result = new ArrayList<Integer>();
        var runtimeInSeconds = runtimeInMinutes * 60;
        var chunkDurationInSeconds = runtimeInSeconds / numberOfChunks;
        
        for (int i = 0; i < numberOfChunks; i++) {
            result.add(i * chunkDurationInSeconds);
        }

        return result.stream().skip(1).toList();
    }   

    private List<SubtitleExtended> getSubtitles(
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

    private String ensureMovieClipPath(String workingPath, Integer movieId) {

        var moviePath = workingPath + movieId + "/";
        var path = Path.of(moviePath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                return moviePath;
            }
            catch (IOException ex) {
                System.out.println("Error creating directory: " + path);
            }
        }

        return moviePath;
    }

    private List<String> createSegmentsFromSubtitles(List<SubtitleExtended> subtitlesExtended, String mediaPath, String workingPath) {
        
        var segmentIndex = 1;
        var rawClipFiles = new ArrayList<String>();

        for (var subtitleExtended : subtitlesExtended) {
            var outputPath = workingPath + RawClipFilename + segmentIndex + ".mp4";
            rawClipFiles.add(outputPath);
            createSegment(
                mediaPath, 
                outputPath, 
                subtitleExtended.subtitle().timeFrom().toEpochMilli(), 
                subtitleExtended.subtitle().timeTo().toEpochMilli());
            segmentIndex++;
        }

        return rawClipFiles;
    }

    private void createSegment(String mediaPath, String outputPath, Long timeFrom, Long timeTo) {
        FFmpegUtil.extractSegment(mediaPath, outputPath, timeFrom, timeTo);
    }

    private List<String> createSceneClips(List<String> rawClips, String workingPath) {

        var clipIndex = 1;
        var clipNames = new ArrayList<String>();
        for (var rawClip : rawClips) {
            
            var clipDuration = FFprobeUtil.getDuration(rawClip);
            System.out.println("clip duration:" + clipDuration);
            var sceneChanges = detectSceneChanges(rawClip);

            var clips = new ArrayList<Clip>();
            Double timeFrom = 0.0;

            if (sceneChanges.size() == 0) {
                // no scene changes - use the raw clip
                var rawEndTime = limitTime(0.0, clipDuration * 1000.0, ClipLimitTime);
                clips.add(new Clip(0L, Math.round(rawEndTime), clipIndex++));
            } else {

                for (var i = 0; i < sceneChanges.size() - 1; i++ ) {

                    var sceneChange = sceneChanges.get(i);
                    if (sceneChange.sequenceNumber() == 0 && sceneChange.timeInSeconds() < 2) {
                        timeFrom = sceneChange.timeInSeconds();
                        continue;
                    }
                    
                    var timeTo = limitTime(timeFrom * 1000.0, sceneChange.timeInSeconds() * 1000.0, ClipLimitTime);
                    clips.add(new Clip(Math.round(timeFrom * 1000.0), Math.round(timeTo), clipIndex++));
                    timeFrom = sceneChange.timeInSeconds();
                }

                // add last segment to the clipDuration
                if (sceneChanges.size() > 0) {
                    var lastScene = sceneChanges.get(sceneChanges.size() - 1);
                    var endTimeTo = limitTime(lastScene.timeInSeconds() * 1000.0, clipDuration * 1000.0, ClipLimitTime);
                    clips.add(new Clip(Math.round(lastScene.timeInSeconds() * 1000.0), Math.round(endTimeTo), clipIndex++));
                }
            }

            // cut to short clips
            for (var clip : clips) {
                var clipName = ClipFilename + clip.clipIndex() + ".mp4";
                clipNames.add(clipName);
                var outputPath = workingPath + clipName;
                createSegment(
                    rawClip, 
                    outputPath, 
                    clip.timeFrom(),
                    clip.timeTo());
            }
        }

        return clipNames;
    }
    
    private Double limitTime(Double timeFrom, Double timeTo, Double upperlimit) {

        var time = timeTo-timeFrom;
        var timeLimited = Math.min(time, upperlimit);
        return timeFrom + timeLimited;
    }

    private List<SceneChange> detectSceneChanges(String rawClip) {
        return FFmpegUtil.detectSceneChanges(rawClip);
    }

    private List<String> normalizeSceneClips(
        List<String> sceneClips, 
        String workingPath, 
        Integer resolutionX,
        Integer resolutionY,
        Integer fps) {

        var clipNames = new ArrayList<String>();
        var index = 1;
        for (var sceneClip : sceneClips) {
            var normalizedClipName = NormalizedClipFilename + index++ + ".mp4";
            clipNames.add(normalizedClipName);
            normalizeClip(workingPath + sceneClip, workingPath + normalizedClipName, resolutionX, resolutionY, fps);
        }

        return clipNames;
    }

    private void normalizeClip(
        String sceneClipPath, 
        String normalizedClipPath, 
        Integer resolutionX, 
        Integer resolutionY, 
        Integer fps) {

        FFmpegUtil.normalizeClip(sceneClipPath, normalizedClipPath, resolutionX, resolutionY, fps);
    }
}
