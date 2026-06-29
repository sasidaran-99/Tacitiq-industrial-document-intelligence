package com.tacitiq.modules.knowledge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expert_knowledge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "engineer_id", nullable = false)
    private UUID engineerId;

    @Column(name = "knowledge_type", nullable = false)
    private String knowledgeType; // tribal, procedure, insight, workaround

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expert_knowledge_asset_tags", joinColumns = @JoinColumn(name = "knowledge_id"))
    @Column(name = "asset_tag")
    private List<String> assetTags;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expert_knowledge_validators", joinColumns = @JoinColumn(name = "knowledge_id"))
    @Column(name = "validator_id")
    private List<UUID> validatedBy;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now();
}
