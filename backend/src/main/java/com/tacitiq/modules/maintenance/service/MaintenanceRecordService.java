package com.tacitiq.modules.maintenance.service;

import com.tacitiq.modules.maintenance.dto.MaintenanceRecordDto;
import com.tacitiq.modules.maintenance.entity.MaintenanceRecord;
import com.tacitiq.modules.maintenance.repository.MaintenanceRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MaintenanceRecordService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            ApplicationEventPublisher eventPublisher) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<MaintenanceRecordDto> getAllRecords() {
        return maintenanceRecordRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MaintenanceRecordDto getRecordById(UUID id) {
        MaintenanceRecord record = maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found with ID: " + id));
        return mapToDto(record);
    }

    public List<MaintenanceRecordDto> getRecordsByAssetId(UUID assetId) {
        return maintenanceRecordRepository.findByAssetId(assetId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MaintenanceRecordDto saveRecord(MaintenanceRecordDto dto) {
        MaintenanceRecord record = MaintenanceRecord.builder()
                .id(dto.getId())
                .assetId(dto.getAssetId())
                .workOrderNo(dto.getWorkOrderNo())
                .maintType(dto.getMaintType())
                .technicianId(dto.getTechnicianId())
                .findings(dto.getFindings())
                .partsReplaced(dto.getPartsReplaced())
                .nextDueDate(dto.getNextDueDate())
                .laborHours(dto.getLaborHours())
                .totalCost(dto.getTotalCost())
                .completedAt(dto.getCompletedAt())
                .build();
        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        MaintenanceRecordDto savedDto = mapToDto(saved);

        // Publish internal Spring events for Event-driven baseline resets
        eventPublisher.publishEvent(savedDto);

        return savedDto;
    }

    private MaintenanceRecordDto mapToDto(MaintenanceRecord entity) {
        return new MaintenanceRecordDto(
                entity.getId(),
                entity.getAssetId(),
                entity.getWorkOrderNo(),
                entity.getMaintType(),
                entity.getTechnicianId(),
                entity.getFindings(),
                entity.getPartsReplaced(),
                entity.getNextDueDate(),
                entity.getLaborHours(),
                entity.getTotalCost(),
                entity.getCompletedAt()
        );
    }
}
