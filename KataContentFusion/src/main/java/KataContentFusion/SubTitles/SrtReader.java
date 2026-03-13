/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.SubTitles;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SrtReader {

    private static final Pattern sequencePattern = Pattern.compile("(\\d+)");
    private static final Pattern timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3}) --> (\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");

    public List<Subtitle> readSrtFile(String filePath) {

        var subtitles = new ArrayList<Subtitle>();
        var path = Path.of(filePath);
        var charset = guessTextFileEncoding(path);
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {

            var entries = reader.lines()
                .toList();

            // read while there are lines to read
            var iterator = entries.iterator();
            while(iterator.hasNext()) {
                var line = iterator.next();
                if (sequencePattern.matcher(line).matches()) {
                    // read sequence number
                    int sequenceNumber = Integer.parseInt(line);
                    // read time line
                    if (iterator.hasNext()) {
                        var timeLine = iterator.next();
                        var timeMatcher = timePattern.matcher(timeLine);
                        if (timeMatcher.matches()) {
                            // parse start and end times
                            var startTime = parseTime(timeMatcher, 1);
                            var endTime = parseTime(timeMatcher, 5);
                            // read subtitle text
                            var textBuilder = new StringBuilder();
                            while (iterator.hasNext()) {
                                var textLine = iterator.next();
                                if (textLine.isBlank()) {
                                    break;
                                }
                                textBuilder.append(textLine).append("\n");
                            }
                            String text = textBuilder.toString().trim();
                            // create subtitle entry
                            Subtitle subtitle = new Subtitle(sequenceNumber, startTime, endTime, text);
                            // add to list
                            subtitles.add(subtitle); // Uncomment this line to add to a list of subtitles
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error reading SRT file: " + e.getMessage(), e);
        }

        return subtitles;
    }

    private Instant parseTime(Matcher timeMatcher, int i) {
        
        var hours = Integer.parseInt(timeMatcher.group(i));
        var minutes = Integer.parseInt(timeMatcher.group(i + 1));
        var second = Integer.parseInt(timeMatcher.group(i + 2));
        var milliseconds = Integer.parseInt(timeMatcher.group(i + 3));

        return Instant.ofEpochMilli(hours * 3600000L + minutes * 60000L + second * 1000L + milliseconds);
    }

    private Charset guessTextFileEncoding(Path path) {

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine(); // Try reading a line to confirm it's readable
            return StandardCharsets.UTF_8;
        } catch (Exception e) {        
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {                
                reader.readLine(); // Try reading a line to confirm it's readable
                return StandardCharsets.ISO_8859_1;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to determine file encoding: " + ex.getMessage(), ex);
            }
        }
    }
}
