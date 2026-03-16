/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Tools;

public record ProcessResult(
    Integer exitCode,
    String stdOut,
    String stdErr
) {}
