/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.LocalDb;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Cast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    public Integer externalId;
    public String name;
    public String originalName;
    public Boolean adult;
    public Integer gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId")
    public Role role;

    public Double popularity;
    public String profilePath;
    public Integer castId;
    public String character;
    public String creditId;
    public Integer orderNo;
}
