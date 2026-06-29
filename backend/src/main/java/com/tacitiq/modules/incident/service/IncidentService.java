package com.tacitiq.modules.incident.service;

import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.incident.entity.Incident;
import com.tacitiq.modules.incident.repository.IncidentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public IncidentService(IncidentRepository incidentRepository, ApplicationEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<IncidentDto> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public IncidentDto getIncidentById(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));
        return mapToDto(incident);
    }

    public List<IncidentDto> getIncidentsByAssetId(UUID assetId) {
        return incidentRepository.findByAssetId(assetId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public IncidentDto saveIncident(IncidentDto dto) {
        Incident incident = Incident.builder()
                .id(dto.getId())
                .incidentType(dto.getIncidentType())
                .severity(dto.getSeverity())
                .assetId(dto.getAssetId())
                .rootCause(dto.getRootCause())
                .contributingFactors(dto.getContributingFactors())
                .lessonsLearned(dto.getLessonsLearned())
                .occurredAt(dto.getOccurredAt())
                .closedAt(dto.getClosedAt())
                .reportedBy(dto.getReportedBy())
                .build();
        Incident saved = incidentRepository.save(incident);
        IncidentDto savedDto = mapToDto(saved);

        // Publish internal Spring events for Event-driven RAG and RCA analysis triggers
        eventPublisher.publishEvent(savedDto);

        return savedDto;
    }

    private IncidentDto mapToDto(Incident entity) {
        return new IncidentDto(
                entity.getId(),
                entity.getIncidentType(),
                entity.getSeverity(),
                entity.getAssetId(),
                entity.getRootCause(),
                entity.getContributingFactors(),
                entity.getLessonsLearned(),
                entity.getOccurredAt(),
                entity.getClosedAt(),
                entity.getReportedBy()
        );
    }
}
