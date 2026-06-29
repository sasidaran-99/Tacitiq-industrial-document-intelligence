package com.tacitiq.modules.document.dto;

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
public class DocumentDto {
    private UUID id;
    private String docType;
    private String title;
    private String storagePath;
    private List<UUID> relatedAssets;
    private Integer version;
    private String embeddingStatus;
    private Integer chunkCount;
    private UUID uploadedBy;
    private OffsetDateTime processedAt;
    private String extractedTags;
    private String extractedFailureModes;
    private String extractedProcedures;
    private String extractedSafetyReferences;
    private String extractedWorkOrders;
    private String extractedFindings;
}
