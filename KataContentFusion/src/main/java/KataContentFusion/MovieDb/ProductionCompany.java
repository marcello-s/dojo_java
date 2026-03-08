/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */
package KataContentFusion.MovieDb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductionCompany(
    Integer id,
    String logo_path,
    String name,
    String origin_country
) {};
