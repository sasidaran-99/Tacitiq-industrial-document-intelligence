package com.tacitiq.modules.incident.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String severity; // P1, P2, P3, P4, P5

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_factors", columnDefinition = "JSONB")
    private String contributingFactors; // JSON list

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "reported_by")
    private UUID reportedBy;
}
