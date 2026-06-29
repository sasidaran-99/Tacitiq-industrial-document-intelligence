package com.tacitiq.modules.maintenance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecordDto {
    private UUID id;
    private UUID assetId;
    private String workOrderNo;
    private String maintType;
    private UUID technicianId;
    private String findings;
    private String partsReplaced;
    private LocalDate nextDueDate;
    private Double laborHours;
    private BigDecimal totalCost;
    private OffsetDateTime completedAt;
}
