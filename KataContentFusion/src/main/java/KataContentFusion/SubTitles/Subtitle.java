package KataContentFusion.SubTitles;

import java.time.Instant;

public record Subtitle(
    Integer sequenceNumber,
    Instant timeFrom,
    Instant timeTo,
    String text
) {}
