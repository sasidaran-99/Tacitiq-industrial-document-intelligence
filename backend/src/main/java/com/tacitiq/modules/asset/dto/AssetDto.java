package com.tacitiq.modules.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetDto {
    private UUID id;
    private String tagNumber;
    private String assetType;
    private UUID parentAssetId;
    private String criticality;
    private Double healthScore;
    private LocalDate installationDate;
    private UUID digitalTwinId;
    private String plantArea;
    private String oemModel;
}
