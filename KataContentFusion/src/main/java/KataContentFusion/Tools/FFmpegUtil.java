/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

import java.util.ArrayList;

public class FFmpegUtil {

    private static final String toolName = "ffmpeg";

    public static void ExtractSegment(String mediaPath, String outputPath, Long timeFrom, Long timeTo) {

        var arguments = new ArrayList<String>();
        arguments.add("-i");
        arguments.add('"' + mediaPath + '"');
        arguments.add("-ss");
        arguments.add(FormatTime(timeFrom));
        arguments.add("-to");
        arguments.add(FormatTime(timeTo));
        arguments.add("-c:v");
        arguments.add("libx264");
        arguments.add("-preset");
        arguments.add("ultrafast");
        arguments.add("-c:a");
        arguments.add("aac");
        arguments.add('"' + outputPath + '"');

        System.out.println("running ffmpeg with arguments: " + String.join(" ", arguments));

        var process = new RunToolProcess();
        var result = process.Run(toolName, arguments);

        if (result.exitCode() != 0) {
            System.out.println("ffmpeg failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }

    private static String FormatTime(Long timeInMilliSeconds) {
        var totalSeconds = timeInMilliSeconds / 1000;
        var hours = totalSeconds / 3600;
        var minutes = (totalSeconds % 3600) / 60;
        var seconds = totalSeconds % 60;
        var milliseconds = timeInMilliSeconds % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }
}
