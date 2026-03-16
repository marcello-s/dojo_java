/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

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
            var process = processBuilder.start();
            var exitCode = process.waitFor();
            var stdOut = new String(process.getInputStream().readAllBytes());
            var stdErr = new String(process.getErrorStream().readAllBytes());

            return new ProcessResult(exitCode, stdOut, stdErr);
        } catch (Exception ex) {
            return new ProcessResult(-1, "", ex.getMessage());
        }
    }
}
