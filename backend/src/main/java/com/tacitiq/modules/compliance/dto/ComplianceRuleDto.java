package com.tacitiq.modules.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleDto {
    private UUID id;
    private String standard;
    private String clause;
    private String description;
    private String checkQuery;
    private String frequency;
    private String applicableAssets;
    private String severity;
    private LocalDate effectiveDate;
}
