/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

import java.util.ArrayList;
import java.util.List;

public class ImageMagickUtil {

    private static final String toolName = "magick";

    public static ResolutionResult measureText(String text, String fontPath, Integer fontSize) {

        var arguments = new ArrayList<String>();
        arguments.add("-format");
        arguments.add("%wx%h");
        arguments.add("-font");
        arguments.add('"' + fontPath + '"');
        arguments.add("-pointsize");
        arguments.add(fontSize.toString());
        arguments.add("label:" + '"' + text + '"');
        arguments.add("info:");

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
                    System.out.println("failed to parse resolution for text " + text + ", output: " + output);
                }
            } else {
                System.out.println("unexpected magick output for text " + text + ", output: " + output);
            }
        } else {
            System.out.println("magick failed for text " + text + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }

        return new ResolutionResult(-1, -1);
    }

    public static void createTextImage(Integer sizeX, Integer sizeY, String text, String fontPath, Integer fontSize, String outputPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-size");
        arguments.add(sizeX.toString() + "x" + sizeY.toString());
        arguments.add("xc:transparent");
        arguments.add("-gravity");
        arguments.add("center");
        arguments.add("-font");
        arguments.add('"' + fontPath + '"');
        arguments.add("-pointsize");
        arguments.add(fontSize.toString());
        arguments.add("-fill");
        arguments.add("white");
        arguments.add("-annotate");
        arguments.add("+0+0"  );
        arguments.add('"' + text + '"');        
        arguments.add("(");
        arguments.add("+clone");
        arguments.add("-fill");
        arguments.add("red");
        arguments.add("-colorize");
        arguments.add("100%");
        arguments.add("-geometry");
        arguments.add("+5+5");
        arguments.add("-blur");
        arguments.add("0x5");
        arguments.add(")");
        arguments.add("-compose");
        arguments.add("DstOver");
        arguments.add("-composite");
        arguments.add('"' + outputPath + '"');

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        debugArguments(arguments);
        
        if (result.exitCode() != 0) {
            System.out.println("magick failed to create text image for text " + text + ", exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }

    public static void combineImages(Integer sizeX, Integer sizeY, String imagePath1, String imagePath2, Integer x, Integer y, String outputPath) {

        var arguments = new ArrayList<String>();
        arguments.add("-size");
        arguments.add(sizeX.toString() + "x" + sizeY.toString());
        arguments.add("xc:transparent");
        arguments.add("(");
        arguments.add('"' + imagePath1 + '"');
        arguments.add("-geometry");
        arguments.add("+0+0");
        arguments.add(")");
        arguments.add("-composite");
        arguments.add("(");
        arguments.add('"' + imagePath2 + '"');
        arguments.add("-geometry");
        arguments.add("+" + x.toString() + "+" + y.toString());
        arguments.add(")");
        arguments.add("-composite");
        arguments.add('"' + outputPath + '"');

        var process = new RunToolProcess();
        var result = process.run(toolName, arguments);

        debugArguments(arguments);
        
        if (result.exitCode() != 0) {
            System.out.println("magick failed to combine images, exit code: " + result.exitCode() + ", stdErr: " + result.stdErr());
        }
    }

    private static void debugArguments(List<String> arguments) {

        System.out.println("running " + toolName + " with arguments: " + String.join(" ", arguments));
    }    
}
