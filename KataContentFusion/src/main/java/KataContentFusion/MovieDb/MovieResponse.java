/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.MovieDb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieResponse (
    Boolean adult,
    String backdrop_path,
    MovieCollection belongs_to_collection,
    Integer budget,    
    Genre[] genres,
    String homepage,
    Integer id,
    String imdb_id,
    String[] origin_country,
    String original_language,
    String original_title,
    String overview,
    Double popularity,
    String poster_path,
    ProductionCompany[] production_companies,
    ProductionCountry[] production_countries,
    String release_date,
    Integer revenue,
    Integer runtime,
    Language[] spoken_languages,
    String status,
    String tagline,
    String title,
    Boolean video,
    Double vote_average,
    Integer vote_count
) {};
