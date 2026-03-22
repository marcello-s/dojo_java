/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.SubTitles;

import java.time.Instant;

public record Subtitle(
    Integer sequenceNumber,
    Instant timeFrom,
    Instant timeTo,
    String text
) {}
