package com.tacitiq.modules.knowledge.service;

import com.tacitiq.modules.knowledge.dto.ExpertKnowledgeDto;
import com.tacitiq.modules.knowledge.entity.ExpertKnowledge;
import com.tacitiq.modules.knowledge.repository.ExpertKnowledgeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpertKnowledgeService {

    private final ExpertKnowledgeRepository expertKnowledgeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpertKnowledgeService(
            ExpertKnowledgeRepository expertKnowledgeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.expertKnowledgeRepository = expertKnowledgeRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<ExpertKnowledgeDto> getAllKnowledge() {
        return expertKnowledgeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ExpertKnowledgeDto getKnowledgeById(UUID id) {
        ExpertKnowledge expertKnowledge = expertKnowledgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge entry not found with ID: " + id));
        return mapToDto(expertKnowledge);
    }

    public List<ExpertKnowledgeDto> getKnowledgeByEngineerId(UUID engineerId) {
        return expertKnowledgeRepository.findByEngineerId(engineerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ExpertKnowledgeDto saveKnowledge(ExpertKnowledgeDto dto) {
        ExpertKnowledge expertKnowledge = ExpertKnowledge.builder()
                .id(dto.getId())
                .engineerId(dto.getEngineerId())
                .knowledgeType(dto.getKnowledgeType())
                .assetTags(dto.getAssetTags())
                .content(dto.getContent())
                .validatedBy(dto.getValidatedBy())
                .confidenceScore(dto.getConfidenceScore())
                .build();
        ExpertKnowledge saved = expertKnowledgeRepository.save(expertKnowledge);
        ExpertKnowledgeDto savedDto = mapToDto(saved);

        // Publish internal Spring events for vector search index synchronization
        eventPublisher.publishEvent(savedDto);

        return savedDto;
    }

    private ExpertKnowledgeDto mapToDto(ExpertKnowledge entity) {
        return new ExpertKnowledgeDto(
                entity.getId(),
                entity.getEngineerId(),
                entity.getKnowledgeType(),
                entity.getAssetTags(),
                entity.getContent(),
                entity.getValidatedBy(),
                entity.getConfidenceScore(),
                entity.getRecordedAt()
        );
    }
}
