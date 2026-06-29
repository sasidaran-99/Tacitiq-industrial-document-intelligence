package com.tacitiq.modules.asset.controller;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.dto.TelemetryDataDto;
import com.tacitiq.modules.asset.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDto> getAssetById(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @GetMapping("/tag/{tagNumber}")
    public ResponseEntity<AssetDto> getAssetByTag(@PathVariable String tagNumber) {
        return ResponseEntity.ok(assetService.getAssetByTag(tagNumber));
    }

    @GetMapping("/{id}/telemetry")
    public ResponseEntity<List<TelemetryDataDto>> getTelemetryData(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int points) {
        return ResponseEntity.ok(assetService.getTelemetryData(id, points));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER')")
    public ResponseEntity<AssetDto> saveAsset(@Validated @RequestBody AssetDto assetDto) {
        return ResponseEntity.ok(assetService.saveAsset(assetDto));
    }
}
