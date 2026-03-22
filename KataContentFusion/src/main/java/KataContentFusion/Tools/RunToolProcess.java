/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RunToolProcess {
    
    public ProcessResult Run(String command, List<String> args) {
        try {
            var commandArguments = new ArrayList<String>();
            commandArguments.add(command);
            commandArguments.addAll(args);
            
            var processBuilder = new ProcessBuilder();
            processBuilder.command(commandArguments);
            processBuilder.redirectErrorStream(true); // Merges stderr into stdout
            var process = processBuilder.start();

            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Just consuming the line prevents the hang
                }
            }

            var exitCode = process.waitFor();
            var stdOut = new String(process.getInputStream().readAllBytes());
            var stdErr = new String(process.getErrorStream().readAllBytes());

            return new ProcessResult(exitCode, stdOut, stdErr);
        } catch (Exception ex) {
            return new ProcessResult(-1, "", ex.getMessage());
        }
    }
}
