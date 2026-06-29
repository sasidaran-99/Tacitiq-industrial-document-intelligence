package com.tacitiq.modules.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDto {
    private UUID id;
    private String incidentType;
    private String severity;
    private UUID assetId;
    private String rootCause;
    private String contributingFactors; // JSON String
    private String lessonsLearned;
    private OffsetDateTime occurredAt;
    private OffsetDateTime closedAt;
    private UUID reportedBy;
}
