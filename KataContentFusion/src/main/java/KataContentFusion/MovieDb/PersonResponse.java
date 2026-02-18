/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.MovieDb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonResponse(
    Boolean adult,
    String[] also_known_as,
    String biography,
    String birthday,
    String deathday,
    Integer gender,
    String homepage,
    Integer id,
    String imdb_id,
    String known_for_department,
    String name,
    String place_of_birth,
    Double popularity,
    String profile_path
) {};
