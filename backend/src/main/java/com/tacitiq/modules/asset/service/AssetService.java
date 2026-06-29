package com.tacitiq.modules.asset.service;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.dto.TelemetryDataDto;
import com.tacitiq.modules.asset.entity.Asset;
import com.tacitiq.modules.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final Random random = new Random();

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<AssetDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AssetDto getAssetById(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + id));
        return mapToDto(asset);
    }

    public AssetDto getAssetByTag(String tagNumber) {
        Asset asset = assetRepository.findByTagNumber(tagNumber)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with tag: " + tagNumber));
        return mapToDto(asset);
    }

    public AssetDto saveAsset(AssetDto dto) {
        Asset asset = Asset.builder()
                .id(dto.getId())
                .tagNumber(dto.getTagNumber())
                .assetType(dto.getAssetType())
                .parentAssetId(dto.getParentAssetId())
                .criticality(dto.getCriticality())
                .healthScore(dto.getHealthScore())
                .installationDate(dto.getInstallationDate())
                .digitalTwinId(dto.getDigitalTwinId())
                .plantArea(dto.getPlantArea())
                .oemModel(dto.getOemModel())
                .build();
        Asset saved = assetRepository.save(asset);
        return mapToDto(saved);
    }

    public List<TelemetryDataDto> getTelemetryData(UUID assetId, int points) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));

        List<TelemetryDataDto> list = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        
        // Base telemetry baselines depending on type
        double baseTemp = 45.0; // Celsius
        double baseVib = 2.2;   // mm/s
        double basePress = 12.0; // bar
        double baseFlow = 250.0; // m3/h
        double baseRpm = 1480.0;

        if (asset.getTagNumber().startsWith("K-")) {
            baseTemp = 75.0;
            baseVib = 3.5;
            basePress = 45.0;
            baseFlow = 1500.0;
            baseRpm = 2950.0;
        } else if (asset.getTagNumber().startsWith("E-")) {
            baseTemp = 95.0;
            baseVib = 0.2;
            basePress = 8.0;
            baseFlow = 500.0;
            baseRpm = 0.0;
        }

        // Lower health score triggers higher base metrics (abnormal state simulation)
        double anomalyMultiplier = 1.0;
        if (asset.getHealthScore() < 0.8) {
            anomalyMultiplier = 1.4; // 40% surge in temperature and vibration
        }

        for (int i = points - 1; i >= 0; i--) {
            OffsetDateTime timestamp = now.minusMinutes(i * 5L);
            double tempNoise = (random.nextDouble() - 0.5) * 2.0;
            double vibNoise = (random.nextDouble() - 0.5) * 0.4;
            double pressNoise = (random.nextDouble() - 0.5) * 0.5;
            double flowNoise = (random.nextDouble() - 0.5) * 10.0;
            double rpmNoise = (random.nextDouble() - 0.5) * 5.0;

            list.add(new TelemetryDataDto(
                    timestamp,
                    (baseTemp * anomalyMultiplier) + tempNoise,
                    (baseVib * anomalyMultiplier) + vibNoise,
                    basePress + pressNoise,
                    baseFlow + flowNoise,
                    baseRpm > 0.0 ? baseRpm + rpmNoise : 0.0
            ));
        }

        return list;
    }

    private AssetDto mapToDto(Asset entity) {
        return new AssetDto(
                entity.getId(),
                entity.getTagNumber(),
                entity.getAssetType(),
                entity.getParentAssetId(),
                entity.getCriticality(),
                entity.getHealthScore(),
                entity.getInstallationDate(),
                entity.getDigitalTwinId(),
                entity.getPlantArea(),
                entity.getOemModel()
        );
    }
}
