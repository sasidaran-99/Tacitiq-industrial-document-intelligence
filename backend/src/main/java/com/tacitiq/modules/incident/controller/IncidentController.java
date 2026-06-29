package com.tacitiq.modules.incident.controller;

import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.incident.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentDto>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getIncidentById(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<IncidentDto>> getIncidentsByAssetId(@PathVariable UUID assetId) {
        return ResponseEntity.ok(incidentService.getIncidentsByAssetId(assetId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'MAINTENANCE_ENGINEER', 'RELIABILITY_ENGINEER')")
    public ResponseEntity<IncidentDto> saveIncident(@Validated @RequestBody IncidentDto incidentDto) {
        return ResponseEntity.ok(incidentService.saveIncident(incidentDto));
    }
}
