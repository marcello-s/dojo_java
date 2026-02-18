/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.MovieDb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResult(
    Boolean adult,
    String backdrop_path,
    Integer[] genre_ids,
    Integer id,
    String original_language,
    String original_title,
    String overview,
    Double popularity,
    String poster_path,
    String release_date,
    String title,
    Boolean video,
    Double vote_average,
    Integer vote_count
) {};
