/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FFmpegUtil {

    private static final String toolName = "ffmpeg";

    public static void extractSegment(String mediaPath, String outputPath, Long timeFrom, Long timeTo) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-ss");
        arguments.add(formatTime(timeFrom));
        arguments.add("-to");
        arguments.add(formatTime(timeTo));
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-preset");
        arguments.add("ultrafast");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);
    }

    private static String formatTime(Long timeInMilliSeconds) {
        var totalSeconds = timeInMilliSeconds / 1000;
        var hours = totalSeconds / 3600;
        var minutes = (totalSeconds % 3600) / 60;
        var seconds = totalSeconds % 60;
        var milliseconds = timeInMilliSeconds % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }
   
    private static void debugArguments(List<String> arguments) {

        System.out.println("running " + toolName + " with arguments: " + String.join(" ", arguments));
    }

    private static ProcessResult runFFmpeg(List<String> arguments) {

        debugArguments(arguments);

        var process = new RunToolProcess();
        return process.run(toolName, arguments);
    }

    private static void printResultFailed(ProcessResult result, String mediaPath) {

        if (result.exitCode() != 0) {
            System.out.println(toolName + " failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }

    public static List<SceneChange> detectSceneChanges(String mediaPath) {

        final String scenesFile = "/Temp/scenes.txt";
        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "select='gt(scene,0.3)',metadata=print:file=" + scenesFile +  '"');
        arguments.add("-f");
        arguments.add("null");
        arguments.add("-");

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);

        if (result.exitCode() == 0) {
            return parseScenesFile(scenesFile);
        }

        return new ArrayList<SceneChange>();
    }

    private static List<SceneChange> parseScenesFile(String scenesFile) {

        // frame:0    pts:27648   pts_time:2.25
        // ^frame:(\d+).+pts_time:(\d+\.\d+).+$
        final Pattern ptsPattern = Pattern.compile("^frame:(\\d+).+pts_time:(\\d+\\.\\d+).+$", Pattern.CASE_INSENSITIVE);

        var sceneChanges = new ArrayList<SceneChange>();
        var filePath = Path.of(scenesFile);
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {

            var lines = reader.lines()
                .toList();
            for (var line : lines) {
                var matcher = ptsPattern.matcher(line);
                if (matcher.find()) {
                    var sequenceNumber = Integer.parseInt(matcher.group(1));
                    var timeInSeconds = Double.parseDouble(matcher.group(2));

                    sceneChanges.add(new SceneChange(sequenceNumber, timeInSeconds));
                }
            }            
        } catch (Exception e) {
            throw new RuntimeException("Error reading scenes file: " + e.getMessage(), e);
        }

        return sceneChanges;
    }

    public static void normalizeClip(
        String mediaPath, 
        String outputPath, 
        Integer resolutionX, 
        Integer resolutionY, 
        Integer fps) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "scale=" + resolutionX + ":" + resolutionY + ":force_original_aspect_ratio=decrease,pad=" + resolutionX + ":" + resolutionY + ":(ow-iw)/2:(oh-ih)/2,fps=" + fps + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-preset");
        arguments.add("fast");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add("-ar");
        arguments.add("44100");
        arguments.add("-ac");
        arguments.add("2");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);
    }

    public static void flipHorizontal(String mediaPath, String outputPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "hflip" + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);        
    }

    public static void zoomFactor(String mediaPath, String outputPath, String factor) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-filter_complex");
        arguments.add('"' + "zoompan=z=" + factor + ":x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1920x1080" + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");        
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);
    }

    public static void boxBlur(String mediaPath, String outputPath, Integer blurRadius) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-filter_complex");
        arguments.add('"' + "boxblur=luma_radius=min(h\\,w)/" + blurRadius + ":luma_power=1:chroma_radius=min(cw\\,ch)/" + blurRadius + ":chroma_power=1" + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");        
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);
    }

    public static void setTempo(String mediaPath, String outputPath, String tempo) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "setpts=PTS/" + tempo + '"');
        arguments.add("-af");
        arguments.add('"' + "atempo=" + tempo + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);        
    }

    public static void changeSaturationAndColor(
        String mediaPath, 
        String outputPath, 
        String saturation, 
        String blueMidTones) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "eq=saturation=" + saturation + ", colorbalance=bm=" + blueMidTones + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);
    }

    public static void reverseClip(String mediaPath, String outputPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-vf");
        arguments.add('"' + "reverse" + '"');
        arguments.add("-af");
        arguments.add('"' + "areverse" + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPath);        
    }

    public static void concatenateClips(String listFilePath, String outputPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-f");
        arguments.add("concat");
        arguments.add("-safe");
        arguments.add("0");
        arguments.add("-i");
        arguments.add('"' + listFilePath + '"');
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, listFilePath);        
    }

    public static void concatenateClips(List<String> mediaPaths, String outputPath) {

        var sb = new StringBuffer();
        for (var i = 0; i < mediaPaths.size(); i++) {
            sb.append("[" + i + ":v][" + i + ":a]");
        }

        sb.append("concat=n=" + mediaPaths.size());
        var arguments = new ArrayList<String>();

        for (var mediaPath : mediaPaths) {
            arguments.add("-i");
            arguments.add('"' + mediaPath + '"');
        }

        arguments.add("-filter_complex");
        arguments.add('"' + sb.toString() + ":v=1:a=1[outv][outa]" + '"');
        arguments.add("-map");
        arguments.add("[outv]");
        arguments.add("-map");
        arguments.add("[outa]");
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-crf");
        arguments.add("23");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        var result = runFFmpeg(arguments);
        printResultFailed(result, mediaPaths.get(0));
    }
}
