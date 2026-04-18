/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final String ChunkFilename = "chunk_";
    private static final String RawClipFilename = "rawclip_";
    private static final String ClipFilename = "clip_";
    private static final String NormalizedClipFilename = "nclip_";
    private static final Double ClipMinLimitTime = 800.0;
    private static final Double ClipMaxLimitTime = 1500.0;
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

    public List<String> CreateSegment(Integer movieId) {

        var empty = new ArrayList<String>();
        var scanNameMovies = scanNameMovieRepository.findScanNameMovieByMovieId(movieId);
        if (scanNameMovies.size() == 0) {
            System.out.println("no scan name movies found for movie id: " + movieId);
            return empty;
        }

        var movie = movieRepository.findById(movieId);
        if (movie == null) {
            System.out.println("no movie found for id: " + movieId);
            return empty;
        }
        
        var scanNameMovie = scanNameMovies.get(0);
        var assetSubtitle = assetRepository.findByScanNameIdAndAssetTypeId(scanNameMovie.scanName.id, AssetTypeIdSubtitle);
        if (assetSubtitle == null) {
            System.out.println("no asset found for scan name id: " + scanNameMovie.scanName.id);
            return empty;
        }

        var subtitleEntries = subtitleEntryRepository.findByAssetId(assetSubtitle.id);
        if (subtitleEntries.size() == 0) {
            System.out.println("no subtitle entries found for asset id: " + assetSubtitle.id);
            return empty;
        }

        var subtitleExtendedList = createSubtitleExtendedList(subtitleEntries);
        var runtimeInMinutes = movie.map(m -> m.runtime).orElse(0);
        System.out.println("movie runtime in minutes: " + runtimeInMinutes);
        var chunkTimes = createChunkTimes(runtimeInMinutes, 10);
        chunkTimes.forEach(chunk -> System.out.println("chunk index: " + chunk.index() + " from: " + chunk.timeFrom() + " to: " + chunk.timeTo()));

        var nonDialogSubtitles = getSubtitles(
            subtitleExtendedList, 
            SubtitleType.NonDialog, 
            chunkTimes, 
            2000,
            120000, 
            (60 * 30));
        
        var formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
        nonDialogSubtitles.forEach(subtitle -> {
            System.out.println("from: " + formatter.format(subtitle.subtitle().timeFrom()) + " to: " + formatter.format(subtitle.subtitle().timeTo()) + " duration: " + subtitle.getDurationInMilliSesonds());
        });        

        var assetMovie = assetRepository.findByScanNameIdAndAssetTypeId(scanNameMovie.scanName.id, AssetTypeIdMovie);
        if (assetMovie == null) {
            System.out.println("no movie asset found for scan name id: " + scanNameMovie.scanName.id);
            return empty;
        }
        System.out.println("movie asset: " + assetMovie.mediaPath);

        var movieClipPath = ensureMovieClipPath(WorkingPath, movieId);
        var chunkFiles = createChunkFiles(chunkTimes, assetMovie.mediaPath, movieClipPath);
        System.out.println("chunk files created: " + chunkFiles.size());

        var rawClips = createSegmentsFromSubtitles(
            chunkTimes,
            nonDialogSubtitles,
            movieClipPath);

        var sceneClips = createSceneClips(rawClips, movieClipPath);
        return normalizeSceneClips(sceneClips, movieClipPath, ResolutionX, ResolutionY, Fps);
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

    private List<FileChunk> createChunkTimes(Integer runtimeInMinutes, Integer numberOfChunks) {

        var runtimeInSeconds = runtimeInMinutes * 60;
        var chunkDurationInSeconds = runtimeInSeconds / numberOfChunks;

        var chunks = new ArrayList<FileChunk>();
        for (int i = 0; i < numberOfChunks; i++) {
            var chunkStartTimeInSeconds = i * chunkDurationInSeconds;
            var chunkEndTimeInSeconds = (i + 1) * chunkDurationInSeconds;
            chunks.add(new FileChunk(i, chunkStartTimeInSeconds, chunkEndTimeInSeconds));
        }

        // skip last chunk, because it often contains end credits and is not suitable for documetary style segments
        return chunks.stream()
            .skip(1)
            .limit(chunks.size() - 2)
            .toList();
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

    private List<String> createChunkFiles(List<FileChunk> chunkTimes, String mediaPath, String workingPath) {

        var chunkFiles = new ArrayList<String>();
        for (var chunkTime : chunkTimes) {
            var outputPath = workingPath + ChunkFilename + chunkTime.index() + ".mp4";
            chunkFiles.add(outputPath);
            createSegment(
                mediaPath, 
                outputPath, 
                chunkTime.timeFrom() * 1000L, 
                chunkTime.timeTo() * 1000L);
        }

        return chunkFiles;
    }

    private List<SubtitleExtended> getSubtitles(
        List<SubtitleExtended> subtitlesExtended,
        SubtitleType subtitleType, 
        List<FileChunk> fileChunks,
        Integer minDurationMilliSeconds,
        Integer maxDurationMilliSeconds, 
        Integer maxTotalDurationSeconds) {

        var result = new ArrayList<SubtitleExtended>();
        var maxTotalDurationInMilliSeconds = maxTotalDurationSeconds * 1000;
        var accumulatedDuration = 0L;
        
        for (var i = 0; i < fileChunks.size() - 1; i++) {
            final int index = i;
            var chunkStartTimeInMilliSeconds = fileChunks.get(index).timeFrom() * 1000L;
            var nextChunkStartTimeInMilliSeconds = fileChunks.get(index + 1).timeFrom() * 1000L;        
       
            var subtitlesFiltered = subtitlesExtended.stream()
                .filter(subtitle -> subtitle.subtitle().timeFrom().toEpochMilli() >= chunkStartTimeInMilliSeconds
                    && subtitle.subtitle().timeFrom().toEpochMilli() < nextChunkStartTimeInMilliSeconds)
                .filter(subtitle -> subtitle.subtitleType() == subtitleType)
                .filter(subtitle -> subtitle.getDurationInMilliSesonds() >= minDurationMilliSeconds 
                    && subtitle.getDurationInMilliSesonds() <= maxDurationMilliSeconds)
                .sorted(Comparator.comparing(subtitle -> subtitle.subtitle().timeFrom()))
                .limit(10)
                .toList();

            for (var subtitle : subtitlesFiltered) {
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

    private List<String> createSegmentsFromSubtitles(
        List<FileChunk> fileChunks,
        List<SubtitleExtended> subtitlesExtended, 
        String workingPath) {
        
        var segmentIndex = 1;
        var rawClipFiles = new ArrayList<String>();

        for (var fileChunk : fileChunks) {
            var chunkStartTimeInMilliSeconds = fileChunk.timeFrom() * 1000L;
            var chunkEndTimeInMilliSeconds = fileChunk.timeTo() * 1000L;

            var subtitlesInChunk = subtitlesExtended.stream()
                .filter(subtitle -> subtitle.subtitle().timeFrom().toEpochMilli() >= chunkStartTimeInMilliSeconds
                    && subtitle.subtitle().timeFrom().toEpochMilli() < chunkEndTimeInMilliSeconds)
                .toList();

            for (var subtitleExtended : subtitlesInChunk) {
                var inputPath = workingPath + ChunkFilename + fileChunk.index() + ".mp4";
                var outputPath = workingPath + RawClipFilename + segmentIndex + ".mp4";
                var timeFrom = subtitleExtended.subtitle().timeFrom().toEpochMilli() - chunkStartTimeInMilliSeconds;
                var timeTo = subtitleExtended.subtitle().timeTo().toEpochMilli() - chunkStartTimeInMilliSeconds;
                if (timeTo-timeFrom < ClipMinLimitTime) {
                    continue;
                }

                rawClipFiles.add(outputPath);
                createSegment(
                    inputPath, 
                    outputPath, 
                    timeFrom, 
                    timeTo);
                segmentIndex++;
            }
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
                var rawEndTime = limitTime(0.0, clipDuration * 1000.0, ClipMaxLimitTime);
                clips.add(new Clip(0L, Math.round(rawEndTime), clipIndex++));
            } else {

                for (var i = 0; i < sceneChanges.size() - 1; i++ ) {

                    var sceneChange = sceneChanges.get(i);
                    if (sceneChange.sequenceNumber() == 0 && sceneChange.timeInSeconds() < 2) {
                        timeFrom = sceneChange.timeInSeconds();
                        continue;
                    }
                    
                    var timeTo = limitTime(timeFrom * 1000.0, sceneChange.timeInSeconds() * 1000.0, ClipMaxLimitTime);
                    if (timeTo-timeFrom*1000.0 < ClipMinLimitTime) {
                        continue;
                    }

                    clips.add(new Clip(Math.round(timeFrom * 1000.0), Math.round(timeTo), clipIndex++));
                    timeFrom = sceneChange.timeInSeconds();
                }

                // add last segment to the clipDuration
                if (sceneChanges.size() > 0) {
                    var lastScene = sceneChanges.get(sceneChanges.size() - 1);
                    var endTimeTo = limitTime(lastScene.timeInSeconds() * 1000.0, clipDuration * 1000.0, ClipMaxLimitTime);
                    if ((endTimeTo - lastScene.timeInSeconds() * 1000.0) >= ClipMinLimitTime) {
                        clips.add(new Clip(Math.round(lastScene.timeInSeconds() * 1000.0), Math.round(endTimeTo), clipIndex++));
                    }
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
