/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import KataContentFusion.Tools.FFmpegUtil;

@Component
public class MovieSegmentConcatenator {
    
    private static final String WorkingPath = "C:/Temp/cf/";
    private static final String EffectClipFilename = "eclip_";

    public void concatenateSegments(Integer movieId, List<String> clips) {
        
        var workingPath = createOutputPath(WorkingPath, movieId);
        var effectClips = applyEffect(workingPath, clips);
        var arollFile = concatenateClips(effectClips, workingPath, movieId);
        System.out.println("a-roll file: " + arollFile);
    }

    private static String createOutputPath(String workingPath, Integer movieId) {

        return workingPath + movieId + "/";
    }

    private List<String> applyEffect(String workingPath, List<String> clips) {

        var clipNames = new ArrayList<String>();
        var counter = 0;
        var index = 1;
        for (var clip : clips) {

            var effectClipName = EffectClipFilename + index++ + ".mp4";
            clipNames.add(effectClipName);
            var clipPath = workingPath + clip;
            var effectClipPath = workingPath + effectClipName; 
            switch(counter % 5) {
                case 0:
                    FFmpegUtil.flipHorizontal(clipPath, effectClipPath);
                    break;
                case 1:
                    FFmpegUtil.zoomFactor(clipPath, effectClipPath, "1.04");
                    break;
                case 2:
                    FFmpegUtil.boxBlur(clipPath, effectClipPath, 500);
                    break;
                case 3:
                    FFmpegUtil.setTempo(clipPath, effectClipPath, "1.04");
                    break;
                case 4:
                    FFmpegUtil.changeSaturationAndColor(clipPath, effectClipPath, "0.75", "0.05");
                    break;
            }

            counter++;
        }

        return clipNames;
    }

    private String concatenateClips(List<String> clips, String workingPath, Integer movieId) {

        var outputPath = WorkingPath + movieId + "aroll.mp4";            
        var clipPaths = new ArrayList<String>();
        for (var clip : clips) {
            var path = workingPath + clip;
            clipPaths.add(path);
        }

        FFmpegUtil.concatenateClips(clipPaths, outputPath);
    
        return outputPath;
    }
}
