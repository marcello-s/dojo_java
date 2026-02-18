/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.MovieDb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Cast(
    Boolean adult,
    Integer gender,
    Integer id,
    String known_for_department, // role
    String name,
    String original_name,
    Double popularity,
    String profile_path,
    Integer cast_id,
    String character,
    String credit_id,
    Integer order
) {};
