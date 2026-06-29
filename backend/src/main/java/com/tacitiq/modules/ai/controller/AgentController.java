package com.tacitiq.modules.ai.controller;

import com.tacitiq.modules.ai.agent.*;
import com.tacitiq.modules.ai.service.ConversationContextStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final KnowledgeAgent knowledgeAgent;
    private final RcaAgent rcaAgent;
    private final FailurePredictionAgent failurePredictionAgent;
    private final KnowledgeLossAgent knowledgeLossAgent;
    private final RecommendationAgent recommendationAgent;
    private final ConversationContextStore conversationContextStore;

    public AgentController(
            KnowledgeAgent knowledgeAgent,
            RcaAgent rcaAgent,
            FailurePredictionAgent failurePredictionAgent,
            KnowledgeLossAgent knowledgeLossAgent,
            RecommendationAgent recommendationAgent,
            ConversationContextStore conversationContextStore) {
        this.knowledgeAgent = knowledgeAgent;
        this.rcaAgent = rcaAgent;
        this.failurePredictionAgent = failurePredictionAgent;
        this.knowledgeLossAgent = knowledgeLossAgent;
        this.recommendationAgent = recommendationAgent;
        this.conversationContextStore = conversationContextStore;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query parameter is required"));
        }
        String conversationId = payload.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        Map<String, Object> context = conversationContextStore.getContext(conversationId);
        String answer = knowledgeAgent.answerQuery(query, "Standard Engineer Context", context);
        conversationContextStore.saveContext(conversationId, context);

        return ResponseEntity.ok(Map.of("response", answer, "conversationId", conversationId));
    }

    @PostMapping("/rca/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'RELIABILITY_ENGINEER')")
    public ResponseEntity<Map<String, String>> triggerRca(@PathVariable UUID incidentId) {
        String report = rcaAgent.analyzeIncidentRootCause(incidentId);
        return ResponseEntity.ok(Map.of("report", report));
    }

    @GetMapping("/predict/{assetId}")
    public ResponseEntity<Map<String, Object>> getPrediction(@PathVariable UUID assetId) {
        return ResponseEntity.ok(failurePredictionAgent.predictFailureRisk(assetId));
    }

    @GetMapping("/retirement-risk")
    public ResponseEntity<List<Map<String, Object>>> getRetirementRisk() {
        return ResponseEntity.ok(knowledgeLossAgent.evaluateRetirementRisk());
    }

    @GetMapping("/recommendations/{assetId}")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@PathVariable UUID assetId) {
        return ResponseEntity.ok(recommendationAgent.generateAssetRecommendations(assetId));
    }

    @PostMapping("/voice-capture")
    public ResponseEntity<Map<String, String>> voiceCapture(@RequestBody Map<String, String> payload) {
        // Mock Whisper Speech-to-Text transcription endpoint
        String speechTranscription = payload.getOrDefault("speechText", "Inboard coupling bearing on P-101 motor component shows high surface friction.");
        return ResponseEntity.ok(Map.of(
                "transcription", speechTranscription,
                "confidence", "0.98",
                "status", "SUCCESS"
        ));
    }
}
