/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import java.nio.file.Path;

public record FsEntry(Path path, boolean isDirectory) {}
