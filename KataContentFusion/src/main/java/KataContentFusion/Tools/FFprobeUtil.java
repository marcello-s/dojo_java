/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

import java.util.ArrayList;
import java.util.List;

public class FFprobeUtil {

    private static final String toolName = "ffprobe";
    
    public static ResolutionResult getResolution(String mediaPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-v");
        arguments.add("error");
        arguments.add("-select_streams");
        arguments.add("v:0"); 
        arguments.add("-show_entries");
        arguments.add("stream=width,height");
        arguments.add("-of");
        arguments.add("csv=s=x:p=0");
        arguments.add('"' + mediaPath + '"');

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        debugArguments(arguments);
        
        if (result.exitCode() == 0) {
            var output = result.stdOut().trim();
            var parts = output.split("x");
            if (parts.length == 2) {
                try {
                    var width = Integer.parseInt(parts[0]);
                    var height = Integer.parseInt(parts[1]);
                    return new ResolutionResult(width, height);
                } catch (NumberFormatException e) {
                    System.out.println("failed to parse resolution for asset " + mediaPath + ", output: " + output);
                }
            } else {
                System.out.println("unexpected ffprobe output for asset " + mediaPath + ", output: " + output);
            }
        } else {
            System.out.println("ffprobe failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }

        return new ResolutionResult(-1, -1);
    }

    public static Double getDuration(String mediaPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-v");
        arguments.add("error");
        arguments.add("-show_entries");
        arguments.add("format=duration");
        arguments.add("-of");
        arguments.add("csv=p=0");
        arguments.add('"' + mediaPath + '"');

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        debugArguments(arguments);

        if (result.exitCode() == 0) {
            var output = result.stdOut().trim();
            var duration = Double.parseDouble(output);

            return duration;
        } else {
            System.out.println("ffprobe failed for asset " + mediaPath + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }

        return 0.0;
    }

    private static void debugArguments(List<String> arguments) {

        System.out.println("running " + toolName + " with arguments: " + String.join(" ", arguments));
    }
}
