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

        debugArguments(arguments);

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        if (result.exitCode() != 0) {
            System.out.println("ffmpeg failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }

    private static String formatTime(Long timeInMilliSeconds) {
        var totalSeconds = timeInMilliSeconds / 1000;
        var hours = totalSeconds / 3600;
        var minutes = (totalSeconds % 3600) / 60;
        var seconds = totalSeconds % 60;
        var milliseconds = timeInMilliSeconds % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
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

        debugArguments(arguments);

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        if (result.exitCode() == 0) {

            return parseScenesFile(scenesFile);
        } else {
            System.out.println("ffmpeg failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }

        return new ArrayList<SceneChange>();
    }

    private static void debugArguments(List<String> arguments) {

        System.out.println("running " + toolName + " with arguments: " + String.join(" ", arguments));
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

        debugArguments(arguments);

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        if (result.exitCode() != 0) {
            System.out.println("ffmpeg failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }
}
