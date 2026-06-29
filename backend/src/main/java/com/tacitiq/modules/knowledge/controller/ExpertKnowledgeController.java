package com.tacitiq.modules.knowledge.controller;

import com.tacitiq.modules.knowledge.dto.ExpertKnowledgeDto;
import com.tacitiq.modules.knowledge.service.ExpertKnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
public class ExpertKnowledgeController {

    private final ExpertKnowledgeService expertKnowledgeService;

    public ExpertKnowledgeController(ExpertKnowledgeService expertKnowledgeService) {
        this.expertKnowledgeService = expertKnowledgeService;
    }

    @GetMapping
    public ResponseEntity<List<ExpertKnowledgeDto>> getAllKnowledge() {
        return ResponseEntity.ok(expertKnowledgeService.getAllKnowledge());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpertKnowledgeDto> getKnowledgeById(@PathVariable UUID id) {
        return ResponseEntity.ok(expertKnowledgeService.getKnowledgeById(id));
    }

    @GetMapping("/engineer/{engineerId}")
    public ResponseEntity<List<ExpertKnowledgeDto>> getKnowledgeByEngineerId(@PathVariable UUID engineerId) {
        return ResponseEntity.ok(expertKnowledgeService.getKnowledgeByEngineerId(engineerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'MAINTENANCE_ENGINEER', 'RELIABILITY_ENGINEER')")
    public ResponseEntity<ExpertKnowledgeDto> saveKnowledge(@Validated @RequestBody ExpertKnowledgeDto expertKnowledgeDto) {
        return ResponseEntity.ok(expertKnowledgeService.saveKnowledge(expertKnowledgeDto));
    }
}
