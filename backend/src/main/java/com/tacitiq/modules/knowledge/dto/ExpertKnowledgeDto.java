package com.tacitiq.modules.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertKnowledgeDto {
    private UUID id;
    private UUID engineerId;
    private String knowledgeType;
    private List<String> assetTags;
    private String content;
    private List<UUID> validatedBy;
    private Double confidenceScore;
    private OffsetDateTime recordedAt;
}
