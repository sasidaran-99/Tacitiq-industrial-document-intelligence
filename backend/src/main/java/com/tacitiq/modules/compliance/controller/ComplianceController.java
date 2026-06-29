package com.tacitiq.modules.compliance.controller;

import com.tacitiq.modules.compliance.dto.ComplianceRuleDto;
import com.tacitiq.modules.compliance.entity.ComplianceRule;
import com.tacitiq.modules.compliance.repository.ComplianceRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceRuleRepository ruleRepository;

    public ComplianceController(ComplianceRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping("/rules")
    public ResponseEntity<List<ComplianceRuleDto>> getAllRules() {
        List<ComplianceRuleDto> list = ruleRepository.findAll().stream()
                .map(entity -> new ComplianceRuleDto(
                        entity.getId(),
                        entity.getStandard(),
                        entity.getClause(),
                        entity.getDescription(),
                        entity.getCheckQuery(),
                        entity.getFrequency(),
                        entity.getApplicableAssets(),
                        entity.getSeverity(),
                        entity.getEffectiveDate()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceRuleDto> saveRule(@Validated @RequestBody ComplianceRuleDto dto) {
        ComplianceRule rule = ComplianceRule.builder()
                .id(dto.getId())
                .standard(dto.getStandard())
                .clause(dto.getClause())
                .description(dto.getDescription())
                .checkQuery(dto.getCheckQuery())
                .frequency(dto.getFrequency())
                .applicableAssets(dto.getApplicableAssets())
                .severity(dto.getSeverity())
                .effectiveDate(dto.getEffectiveDate())
                .build();
        ComplianceRule saved = ruleRepository.save(rule);
        dto.setId(saved.getId());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/gaps")
    public ResponseEntity<List<Map<String, Object>>> getComplianceGaps() {
        // Returns active mock gaps for the visual operational feed
        return ResponseEntity.ok(List.of(
                Map.of(
                        "ruleId", "d0000000-0000-0000-0000-000000000001",
                        "standard", "OSHA 1910.147",
                        "clause", "Lockout/Tagout (LOTO)",
                        "assetTag", "P-101",
                        "description", "Missing standard isolation check-sheets inside maintenance manual files.",
                        "severity", "CRITICAL",
                        "detectedAt", "2026-06-25T11:00:00Z"
                ),
                Map.of(
                        "ruleId", "d0000000-0000-0000-0000-000000000002",
                        "standard", "API 570",
                        "clause", "Piping Inspection Code",
                        "assetTag", "E-205",
                        "description", "Thickness measurement report past due. Last inspection occurred 14 months ago.",
                        "severity", "MAJOR",
                        "detectedAt", "2026-06-24T09:30:00Z"
                )
        ));
    }
}
