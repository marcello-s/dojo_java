/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.SubTitles;

public record SubtitleExtended(
    Subtitle subtitle,
    SubtitleType subtitleType
) {
    public Long getDurationInMilliSesonds() {
        return (subtitle.timeTo().toEpochMilli() - subtitle.timeFrom().toEpochMilli());
    }
}
