package com.tacitiq.modules.maintenance.controller;

import com.tacitiq.modules.maintenance.dto.MaintenanceRecordDto;
import com.tacitiq.modules.maintenance.service.MaintenanceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
public class AccountMaintenanceController {

    private final MaintenanceRecordService maintenanceRecordService;

    public AccountMaintenanceController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRecordDto>> getAllRecords() {
        return ResponseEntity.ok(maintenanceRecordService.getAllRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecordDto> getRecordById(@PathVariable UUID id) {
        return ResponseEntity.ok(maintenanceRecordService.getRecordById(id));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<MaintenanceRecordDto>> getRecordsByAssetId(@PathVariable UUID assetId) {
        return ResponseEntity.ok(maintenanceRecordService.getRecordsByAssetId(assetId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'MAINTENANCE_ENGINEER')")
    public ResponseEntity<MaintenanceRecordDto> saveRecord(@Validated @RequestBody MaintenanceRecordDto maintenanceRecordDto) {
        return ResponseEntity.ok(maintenanceRecordService.saveRecord(maintenanceRecordDto));
    }
}
