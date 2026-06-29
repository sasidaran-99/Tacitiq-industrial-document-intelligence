package com.tacitiq.modules.document.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "doc_type", nullable = false)
    private String docType; // SOP, incident, manual, inspection, email, drawing

    @Column(nullable = false)
    private String title;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_related_assets", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "asset_id")
    private List<UUID> relatedAssets;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "embedding_status", nullable = false)
    private String embeddingStatus; // PENDING, PROCESSING, DONE, FAILED

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "extracted_tags")
    private String extractedTags;

    @Column(name = "extracted_failure_modes")
    private String extractedFailureModes;

    @Column(name = "extracted_procedures")
    private String extractedProcedures;

    @Column(name = "extracted_safety_references")
    private String extractedSafetyReferences;

    @Column(name = "extracted_work_orders")
    private String extractedWorkOrders;

    @Column(name = "extracted_findings")
    private String extractedFindings;
}
