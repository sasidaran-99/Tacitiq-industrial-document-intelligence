package com.tacitiq.modules.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compliance_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String standard; // OSHA, ISO, API, ASME, NFPA

    @Column(nullable = false)
    private String clause;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "check_query", nullable = false, columnDefinition = "TEXT")
    private String checkQuery;

    @Column(nullable = false)
    private String frequency; // daily, weekly, monthly, annual

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_assets", columnDefinition = "JSONB")
    private String applicableAssets; // JSON filter rules

    @Column(nullable = false)
    private String severity; // critical, major, minor

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
}
