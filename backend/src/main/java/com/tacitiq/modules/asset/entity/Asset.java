package com.tacitiq.modules.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tag_number", unique = true, nullable = false)
    private String tagNumber;

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(name = "parent_asset_id")
    private UUID parentAssetId;

    @Column(nullable = false)
    private String criticality; // A, B, C, D

    @Column(name = "health_score", nullable = false)
    private Double healthScore;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Column(name = "digital_twin_id")
    private UUID digitalTwinId;

    @Column(name = "plant_area", nullable = false)
    private String plantArea;

    @Column(name = "oem_model")
    private String oemModel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
