package com.tacitiq.modules.maintenance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "work_order_no", unique = true, nullable = false)
    private String workOrderNo;

    @Column(name = "maint_type", nullable = false)
    private String maintType; // PM (Preventive), CM (Corrective), PdM (Predictive), Overhaul, Inspection

    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parts_replaced", columnDefinition = "JSONB")
    private String partsReplaced; // JSON string representation of replaced components

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "labor_hours")
    private Double laborHours;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
