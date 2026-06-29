package com.tacitiq.modules.dashboard.controller;

import com.tacitiq.modules.asset.service.AssetService;
import com.tacitiq.modules.incident.service.IncidentService;
import com.tacitiq.modules.knowledge.service.ExpertKnowledgeService;
import com.tacitiq.modules.document.repository.DocumentRepository;
import com.tacitiq.modules.document.entity.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AssetService assetService;
    private final IncidentService incidentService;
    private final ExpertKnowledgeService expertKnowledgeService;
    private final DocumentRepository documentRepository;

    public DashboardController(
            AssetService assetService,
            IncidentService incidentService,
            ExpertKnowledgeService expertKnowledgeService,
            DocumentRepository documentRepository) {
        this.assetService = assetService;
        this.incidentService = incidentService;
        this.expertKnowledgeService = expertKnowledgeService;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryStats() {
        int assetCount = assetService.getAllAssets().size();
        double avgHealth = assetService.getAllAssets().stream()
                .mapToDouble(a -> a.getHealthScore())
                .average()
                .orElse(1.0);

        int activeIncidents = incidentService.getAllIncidents().size();
        int totalKnowledge = expertKnowledgeService.getAllKnowledge().size();

        List<Document> docs = documentRepository.findAll();
        int totalDocs = docs.size();
        long processedDocs = docs.stream().filter(d -> "DONE".equals(d.getEmbeddingStatus())).count();
        long graphLinks = docs.stream().mapToLong(d -> d.getRelatedAssets() != null ? d.getRelatedAssets().size() : 0).sum();
        long entities = totalDocs * 6; // failure modes, safety rules, assets, procedures, wos, findings
        String lastDoc = docs.isEmpty() ? "None" : docs.get(docs.size() - 1).getTitle();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssets", assetCount);
        stats.put("averageHealthScore", String.format("%.2f", avgHealth));
        stats.put("activeIncidentsCount", activeIncidents);
        stats.put("complianceAuditStatus", "ELEVATED_RISK");
        stats.put("expertSubmissionsCount", totalKnowledge);
        stats.put("criticalAssetsCount", 3);
        
        // Document Intelligence Stats
        stats.put("totalDocuments", totalDocs);
        stats.put("processedDocuments", processedDocs);
        stats.put("extractedEntities", entities);
        stats.put("graphLinksCreated", graphLinks);
        stats.put("lastUploadedDocument", lastDoc);

        return ResponseEntity.ok(stats);
    }
}
