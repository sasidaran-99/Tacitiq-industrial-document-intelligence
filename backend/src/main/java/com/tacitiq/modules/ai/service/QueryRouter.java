package com.tacitiq.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class QueryRouter {

    private static final Logger log = LoggerFactory.getLogger(QueryRouter.class);

    // List of pre-seeded asset tag numbers
    private static final List<String> SEEDED_ASSETS = Arrays.asList("P-101", "K-201", "E-205", "V-301", "M-101");

    // Common pronouns and action indicators that shouldn't be treated as custom asset tags
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "it", "this", "the", "a", "an", "is", "was", "has", "have", "fail", "failed", 
            "unhealthy", "problem", "condition", "status", "repaired", "serviced", "healthy",
            "now", "today", "yesterday", "repaired", "fixed", "broken", "critical", "diagnose"
    ));

    // Broad set of industrial vocabulary to filter out-of-scope queries
    private static final Set<String> INDUSTRIAL_VOCAB = new HashSet<>(Arrays.asList(
            "pump", "pumps", "compressor", "compressors", "motor", "exchanger", "vessel", "health", "incident", "failure", 
            "maintenance", "work", "order", "loto", "osha", "api", "compliance", "retirement", 
            "expert", "engineer", "telemetry", "vibration", "temperature", "rpm", "flow", "pressure", 
            "graph", "rca", "status", "summary", "anomaly", "anomalies", "risk", "highest", "average", 
            "score", "index", "repair", "service", "violation", "audit", "standard", "cause", "issue",
            "problem", "condition", "wrong", "fail", "active", "rule", "gap", "sop", "why", "what", "how",
            "when", "was", "it", "this", "repaired", "serviced", "fixed", "history", "date", "completed",
            "done", "perform", "performed", "did", "she", "he", "they", "scheduled", "last",
            "assets", "asset", "monitored", "inventory", "equipment", "list", "show", "display", "all", "exist",
            "cdu", "hcu", "criticality", "healthiest", "lowest", "worst", "best"
    ));

    public enum QueryIntent {
        PLANT_SUMMARY,
        HEALTH_INDEX,
        HIGHEST_RISK_ASSETS,
        ASSET_DIAGNOSTIC,
        INCIDENT_SUMMARY,
        COMPLIANCE_STATUS,
        WORKFORCE_RISK,
        MAINTENANCE_HISTORY,
        TELEMETRY_ANOMALIES,
        KNOWLEDGE_GRAPH,
        ASSET_INVENTORY,
        DOCUMENT_PROCEDURES,
        DOCUMENT_SUMMARY,
        DOCUMENT_REFERENCES,
        OUT_OF_SCOPE
    }

    public static class RoutingResult {
        private final QueryIntent intent;
        private final String targetAssetTag;
        private final boolean missingAssetContext;

        public RoutingResult(QueryIntent intent, String targetAssetTag, boolean missingAssetContext) {
            this.intent = intent;
            this.targetAssetTag = targetAssetTag;
            this.missingAssetContext = missingAssetContext;
        }

        public QueryIntent getIntent() {
            return intent;
        }

        public String getTargetAssetTag() {
            return targetAssetTag;
        }

        public boolean isMissingAssetContext() {
            return missingAssetContext;
        }

        @Override
        public String toString() {
            return "RoutingResult{intent=" + intent + ", targetAssetTag='" + targetAssetTag + "', missingAssetContext=" + missingAssetContext + "}";
        }
    }

    public RoutingResult route(String query, Map<String, Object> chatContext) {
        if (query == null || query.isBlank()) {
            return new RoutingResult(QueryIntent.PLANT_SUMMARY, null, false);
        }

        log.info("QueryRouter processing query: '{}'", query);

        // Step 1: Normalize query
        String normalized = query.toLowerCase().replaceAll("[^a-zA-Z0-9\\s-]", " ").trim();
        List<String> tokens = Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());

        // Step 2: Out-of-Scope check (Immediate return if query has no industrial terminology at all)
        boolean hasIndustrialVocab = tokens.stream().anyMatch(INDUSTRIAL_VOCAB::contains);
        if (!hasIndustrialVocab) {
            log.warn("Query is out of scope (no industrial vocabulary matched).");
            return new RoutingResult(QueryIntent.OUT_OF_SCOPE, null, false);
        }

        // Step 3: Detect Asset IDs
        String detectedAssetTag = null;
        
        // A. Check for direct matches to seeded assets first (case-insensitive)
        for (String asset : SEEDED_ASSETS) {
            if (query.toUpperCase().contains(asset)) {
                detectedAssetTag = asset;
                break;
            }
        }

        // B. Sequential entity check (e.g., "Pump XYZ")
        if (detectedAssetTag == null) {
            List<String> indicatorWords = Arrays.asList("pump", "compressor", "motor", "exchanger", "vessel", "equipment", "asset", "tag");
            for (int i = 0; i < tokens.size(); i++) {
                if (indicatorWords.contains(tokens.get(i)) && (i + 1 < tokens.size())) {
                    String candidate = tokens.get(i + 1);
                    if (!STOP_WORDS.contains(candidate)) {
                        detectedAssetTag = candidate.toUpperCase();
                        log.info("Extracted candidate tag '{}' following indicator word '{}'", detectedAssetTag, tokens.get(i));
                        break;
                    }
                }
            }
        }

        // C. Regex fallback for standard formatted tags (e.g. P-102, K-202)
        if (detectedAssetTag == null) {
            Pattern pattern = Pattern.compile("\\b([a-zA-Z]-[0-9]+)\\b");
            Matcher matcher = pattern.matcher(query.toUpperCase());
            if (matcher.find()) {
                detectedAssetTag = matcher.group(1);
            }
        }

        // D. Contextual fallback or missing asset flag check
        boolean hasPronoun = tokens.stream().anyMatch(t -> Arrays.asList("it", "this", "that").contains(t));
        boolean missingAssetContext = false;

        if (detectedAssetTag != null) {
            chatContext.put("lastAssetTag", detectedAssetTag);
            log.info("Detected asset tag '{}' in query. Updated conversation context.", detectedAssetTag);
        } else {
            detectedAssetTag = (String) chatContext.get("lastAssetTag");
            if (detectedAssetTag != null) {
                log.info("No asset tag in query. Resolved pronoun/context to '{}' from conversation context.", detectedAssetTag);
            } else if (hasPronoun) {
                log.warn("Pronoun used but no asset tag exists in conversation history.");
                missingAssetContext = true;
            }
        }

        // Intent override for dynamic Document/RAG lookup queries
        String queryUpper = query.toUpperCase();
        if (queryUpper.contains("SUMMARIZE") || queryUpper.contains("SUMMARY") && (queryUpper.contains("SOP") || queryUpper.contains("UPLOAD"))) {
            log.info("Intent overridden: DOCUMENT_SUMMARY");
            return new RoutingResult(QueryIntent.DOCUMENT_SUMMARY, detectedAssetTag, missingAssetContext);
        }
        if (queryUpper.contains("PROCEDURE") || queryUpper.contains("PROCEDURES") || queryUpper.contains("LUBRICATION")) {
            log.info("Intent overridden: DOCUMENT_PROCEDURES");
            return new RoutingResult(QueryIntent.DOCUMENT_PROCEDURES, detectedAssetTag, missingAssetContext);
        }
        if (queryUpper.contains("DOCUMENTS REFERENCE") || queryUpper.contains("DOCUMENT REFERENCE") || (queryUpper.contains("DOCUMENT") || queryUpper.contains("DOCUMENTS") && queryUpper.contains("REFERENCE"))) {
            log.info("Intent overridden: DOCUMENT_REFERENCES");
            return new RoutingResult(QueryIntent.DOCUMENT_REFERENCES, detectedAssetTag, missingAssetContext);
        }

        // E. Detect ranking query type and route directly to HEALTH_INDEX
        boolean hasLowest = tokens.stream().anyMatch(t -> Arrays.asList("lowest", "worst", "unhealthiest").contains(t));
        boolean hasHighest = tokens.stream().anyMatch(t -> Arrays.asList("highest", "healthiest", "best").contains(t));
        if (hasLowest) {
            chatContext.put("rankingType", "lowest");
            log.info("Query matched ranking keyword 'lowest'. Routing to HEALTH_INDEX.");
            return new RoutingResult(QueryIntent.HEALTH_INDEX, detectedAssetTag, missingAssetContext);
        } else if (hasHighest) {
            chatContext.put("rankingType", "highest");
            log.info("Query matched ranking keyword 'highest'. Routing to HEALTH_INDEX.");
            return new RoutingResult(QueryIntent.HEALTH_INDEX, detectedAssetTag, missingAssetContext);
        } else {
            chatContext.remove("rankingType");
        }

        // Step 4: Intent Classification by Keyword Overlap (Layered Similarity Matcher)
        Map<QueryIntent, Set<String>> intentKeywordsMap = new EnumMap<>(QueryIntent.class);
        intentKeywordsMap.put(QueryIntent.HEALTH_INDEX, new HashSet<>(Arrays.asList("health", "index", "score", "overall", "average", "indices")));
        intentKeywordsMap.put(QueryIntent.HIGHEST_RISK_ASSETS, new HashSet<>(Arrays.asList("risk", "highest", "immediate", "unhealthiest", "worst", "critical", "need", "require")));
        intentKeywordsMap.put(QueryIntent.ASSET_DIAGNOSTIC, new HashSet<>(Arrays.asList("condition", "wrong", "unhealthy", "failed", "fail", "problem", "diagnose", "diagnostic", "why", "broken")));
        intentKeywordsMap.put(QueryIntent.INCIDENT_SUMMARY, new HashSet<>(Arrays.asList("incident", "incidents", "issues", "active", "list", "two", "problems", "happen", "happened")));
        intentKeywordsMap.put(QueryIntent.COMPLIANCE_STATUS, new HashSet<>(Arrays.asList("compliance", "osha", "regulation", "standard", "gap", "violation", "audit", "legal", "rule")));
        intentKeywordsMap.put(QueryIntent.WORKFORCE_RISK, new HashSet<>(Arrays.asList("expert", "retire", "retiring", "experience", "retirement", "loss", "planning", "personnel", "workforce", "retirees")));
        intentKeywordsMap.put(QueryIntent.MAINTENANCE_HISTORY, new HashSet<>(Arrays.asList("maintenance", "service", "repair", "repaired", "serviced", "work order", "history", "records", "completed", "wo")));
        intentKeywordsMap.put(QueryIntent.TELEMETRY_ANOMALIES, new HashSet<>(Arrays.asList("telemetry", "vibration", "temperature", "sensor", "anomalies", "anomaly", "rpm", "flow", "pressure", "reading", "readings", "trend")));
        intentKeywordsMap.put(QueryIntent.KNOWLEDGE_GRAPH, new HashSet<>(Arrays.asList("graph", "neo4j", "relationships", "path", "causal", "traversal", "5-why", "why", "connection", "connections")));
        intentKeywordsMap.put(QueryIntent.ASSET_INVENTORY, new HashSet<>(Arrays.asList("inventory", "monitored", "assets", "equipment", "list", "show", "display", "all", "exist", "pumps", "compressors", "cdu", "hcu", "criticality")));
        intentKeywordsMap.put(QueryIntent.PLANT_SUMMARY, new HashSet<>(Arrays.asList("summary", "status", "today", "overview", "system", "general", "plant")));

        QueryIntent bestIntent = QueryIntent.PLANT_SUMMARY;
        double maxScore = 0.0;

        for (Map.Entry<QueryIntent, Set<String>> entry : intentKeywordsMap.entrySet()) {
            Set<String> keywords = entry.getValue();
            long matchCount = tokens.stream().filter(keywords::contains).count();
            
            if (matchCount > 0) {
                double score = (double) matchCount / (tokens.size() + keywords.size() - matchCount);
                if (score > maxScore) {
                    maxScore = score;
                    bestIntent = entry.getKey();
                }
            }
        }

        // Special handling: if query mentions "maintenance" / "repaired" and an asset tag, it is maintenance history.
        if (bestIntent == QueryIntent.PLANT_SUMMARY && detectedAssetTag != null) {
            boolean isMaint = tokens.stream().anyMatch(t -> Arrays.asList("maintenance", "repair", "service", "repaired", "serviced", "work").contains(t));
            if (isMaint) {
                bestIntent = QueryIntent.MAINTENANCE_HISTORY;
            } else {
                bestIntent = QueryIntent.ASSET_DIAGNOSTIC;
            }
        }

        log.info("QueryRouter determined intent: {} (confidence: {})", bestIntent, maxScore);
        return new RoutingResult(bestIntent, detectedAssetTag, missingAssetContext);
    }
}
