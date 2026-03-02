/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    public Integer externalId;
    public String name;
    public Boolean adult;
    public String biography;
    public LocalDateTime birthday;
    public LocalDateTime deathday;
    public Integer gender;
    public String homepage;
    public String imdbId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId")
    public Role role;

    public String placeOfBirth;
    public Double popularity;
    public String profilePath;
    
    public Optional<LocalDateTime> getDeathday() {
        return Optional.ofNullable(deathday);
    }

    public void setCollection(LocalDateTime deathday) {
        this.deathday = deathday;
    }
}
