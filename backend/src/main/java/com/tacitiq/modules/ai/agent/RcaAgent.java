package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.ai.service.ChatService;
import com.tacitiq.modules.graph.service.GraphService;
import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.incident.service.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RcaAgent {

    private static final Logger log = LoggerFactory.getLogger(RcaAgent.class);

    private final ChatService chatService;
    private final GraphService graphService;
    private final IncidentService incidentService;

    public RcaAgent(ChatService chatService, GraphService graphService, IncidentService incidentService) {
        this.chatService = chatService;
        this.graphService = graphService;
        this.incidentService = incidentService;
    }

    public String analyzeIncidentRootCause(UUID incidentId) {
        log.info("RCA Agent executing 5-Why path traversal for incident ID: {}", incidentId);

        IncidentDto incident = incidentService.getIncidentById(incidentId);
        
        // Fetch graph context surrounding this incident's asset
        Map<String, Object> pathElements = Map.of();
        if (incident.getAssetId() != null) {
            pathElements = graphService.getAssetRcaPath("P-101"); // using tag fallback
        }

        // Construct 5-Why system prompt instructions
        String systemPrompt = String.format("""
            You are TacitIQ's Root Cause Analysis (RCA) Agent.
            Your task is to analyze the reported incident and compile a structured 5-Why root cause report.
            
            Incident Details:
            - Type: %s
            - Severity: %s
            - Description / Root Cause raw notes: %s
            - Contributing Factors raw notes: %s
            
            Graph Traversal Context:
            %s
            
            Formulate a report using the structure:
            1. Executive Summary
            2. 5-Why Causal Tree (Why 1 -> Why 2 -> Why 3 -> Why 4 -> Why 5)
            3. Recommended Corrective Actions
            """, 
            incident.getIncidentType(),
            incident.getSeverity(),
            incident.getRootCause(),
            incident.getContributingFactors(),
            pathElements
        );

        return chatService.generateResponse(systemPrompt, "Synthesize complete RCA report for Incident ID " + incidentId);
    }
}
