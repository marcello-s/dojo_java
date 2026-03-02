/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ScanName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    public String name;
    public String collection;

    public Optional<String> getCollection() {
        return Optional.ofNullable(collection);
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    @OneToMany(mappedBy = "id")
    public List<ScanNameMovie> scanNameMovies;
}
