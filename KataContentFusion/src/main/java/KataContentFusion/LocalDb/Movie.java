/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    public Integer externalId;
    public String title;
    public Boolean adult;
    public Integer budget;
    public String imdbId;
    public String originalLanguage;
    public String originalTitle;
    public String overview;
    public Double popularity;
    public LocalDateTime releaseDate;
    public Integer revenue;
    public Integer runtime;
    public String status;
    public String tagline;
    public Double voteAverage;
    public Integer voteCount;
}
