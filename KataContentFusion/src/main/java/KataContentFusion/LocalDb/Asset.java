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
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanTypeId")
    public ScanType scanTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanNameId")
    public ScanName scanNameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assetTypeId")
    public AssetType assetTypeId;

    public String mediaPath;
    public Integer resolutionX;
    public Integer resolutionY;
}
