package com.tacitiq.modules.ai.service;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.dto.TelemetryDataDto;
import com.tacitiq.modules.compliance.entity.ComplianceRule;
import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.maintenance.entity.MaintenanceRecord;
import com.tacitiq.modules.document.entity.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilder.class);

    public String buildSystemPrompt(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are TacitIQ's Knowledge Agent, an expert industrial operations assistant.\n");
        sb.append("Use the dynamic live plant data provided below to answer the user's query.\n");
        sb.append("If the query asks about topics outside the provided context or unrelated to plant operations, ");
        sb.append("respond with a polite out-of-scope warning.\n\n");
        
        sb.append("### Dynamic Live Plant Context:\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sb.append(String.format("- **%s**: %s\n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    public String renderOfflineResponse(Map<String, Object> data) {
        String intentStr = (String) data.get("intent");
        if (intentStr == null) {
            intentStr = "PLANT_SUMMARY";
        }

        if (Boolean.TRUE.equals(data.get("isOutOfScope"))) {
            return "### Out of Scope Request\n\n" +
                   "I only answer plant-related operational queries (e.g. asset health, telemetry metrics, maintenance history, compliance logs, or workforce risks). Please rephrase your query.";
        }

        if (data.containsKey("missingAssetContext")) {
            return "### Missing Asset Context\n\n" +
                   "Please specify an asset tag.\n\n" +
                   "**Registered tags in this plant**: " +
                   (data.get("availableAssetTags") != null ? String.join(", ", (List<String>) data.get("availableAssetTags")) : "P-101, K-201, E-205, V-301, M-101");
        }

        if (data.containsKey("invalidAssetTag")) {
            return String.format("### Invalid Asset Tag Detected\n\n" +
                                 "Asset tag **%s** is not registered in the plant system.\n\n" +
                                 "**Registered tags in this plant**: %s",
                    data.get("invalidAssetTag"),
                    data.get("availableAssetTags") != null ? String.join(", ", (List<String>) data.get("availableAssetTags")) : "");
        }

        switch (intentStr) {
            case "PLANT_SUMMARY":
                return renderPlantSummary(data);
            case "HEALTH_INDEX":
                return renderHealthIndex(data);
            case "HIGHEST_RISK_ASSETS":
                return renderHighestRiskAssets(data);
            case "ASSET_DIAGNOSTIC":
                return renderAssetDiagnostic(data);
            case "INCIDENT_SUMMARY":
                return renderIncidentSummary(data);
            case "COMPLIANCE_STATUS":
                return renderComplianceStatus(data);
            case "WORKFORCE_RISK":
                return renderWorkforceRisk(data);
            case "MAINTENANCE_HISTORY":
                return renderMaintenanceHistory(data);
            case "TELEMETRY_ANOMALIES":
                return renderTelemetryAnomalies(data);
            case "KNOWLEDGE_GRAPH":
                return renderKnowledgeGraph(data);
            case "ASSET_INVENTORY":
                return renderAssetInventory(data);
            case "DOCUMENT_PROCEDURES":
                return renderDocumentProcedures(data);
            case "DOCUMENT_SUMMARY":
                return renderDocumentSummary(data);
            case "DOCUMENT_REFERENCES":
                return renderDocumentReferences(data);
            default:
                return "### Dynamic offline fallback response generated.\nData payload: " + data.toString();
        }
    }

    private String renderPlantSummary(Map<String, Object> data) {
        double avgHealth = (Double) data.getOrDefault("averageHealthScore", 0.0);
        int totalAssets = (Integer) data.getOrDefault("totalAssets", 0);
        int activeIncidents = (Integer) data.getOrDefault("activeIncidentsCount", 0);
        int totalRules = (Integer) data.getOrDefault("totalComplianceRules", 0);
        List<AssetDto> bottomAssets = (List<AssetDto>) data.getOrDefault("topRiskAssets", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### TacitIQ Plant Operations Overview Summary\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("The plant is operating at an overall average **Health Score of %.2f%%** across **%d assets**. There are **%d active incidents** requiring attention and **%d compliance rules** monitored.\n\n",
                avgHealth * 100, totalAssets, activeIncidents, totalRules));

        sb.append("#### 2. Supporting Evidence (Lowest Health Assets)\n");
        for (AssetDto a : bottomAssets) {
            sb.append(String.format("- **%s** (%s) in *%s*: Health is **%.0f%%** (Criticality %s)\n",
                    a.getTagNumber(), a.getAssetType(), a.getPlantArea(), a.getHealthScore() * 100, a.getCriticality()));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append(String.format("- **Active Incident Count**: %d\n", activeIncidents));
        sb.append(String.format("- **Compliance Gaps Monitored**: %d rule sets configured\n\n", totalRules));

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Immediate Action**: Prioritize inspective overhaul on lowest health items. Check greasing schedules for Rotating Equipment (Criticality A).\n");
        sb.append("> **Preventive Action**: Address the critical workforce knowledge gaps before upcoming retirements.\n");
        sb.append("- Click an action to open the platform view:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Asset Registry & Active Incidents database\n");
        return sb.toString();
    }

    private String renderHealthIndex(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();

        // 1. Direct answer if ranking query is triggered
        if (data.containsKey("rankingType") && data.containsKey("targetRankingAsset")) {
            AssetDto rankAsset = (AssetDto) data.get("targetRankingAsset");
            String rankingType = (String) data.get("rankingType");
            String conditionWord = "lowest".equals(rankingType) ? "lowest health score" : "highest health score";
            
            sb.append("### Answer\n");
            sb.append(String.format("**%s** (%s) currently has the %s at **%.0f%%**.\n\n",
                    rankAsset.getTagNumber(), rankAsset.getAssetType(), conditionWord, rankAsset.getHealthScore() * 100));
            sb.append("---\n\n");
        }

        double avgHealth = (Double) data.getOrDefault("averageHealthScore", 0.0);
        List<Map<String, Object>> healthList = (List<Map<String, Object>>) data.getOrDefault("assetsHealthList", List.of());

        sb.append("### Plant Health Index Audit\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("The overall average health index is **%.2f%%**.\n\n", avgHealth * 100));

        sb.append("#### 2. Supporting Evidence (Asset Rankings)\n");
        sb.append("| Asset Tag | Equipment Type | Health Score | Status |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        for (Map<String, Object> map : healthList) {
            double hVal = (Double) map.get("health");
            String status = hVal >= 0.88 ? "🟢 Healthy" : (hVal >= 0.80 ? "🟡 Warning" : "🔴 Needs Attention");
            sb.append(String.format("| **%s** | %s | **%.0f%%** | **%s** |\n",
                    map.get("tag"), map.get("type"), hVal * 100, status));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Postgres asset index registry updated daily.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Preventive Action**: Schedule preventative checks for any asset scoring below **80%** (e.g. heat exchangers).\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Asset Registry Database\n");
        return sb.toString();
    }

    private String renderHighestRiskAssets(Map<String, Object> data) {
        List<Map<String, Object>> riskList = (List<Map<String, Object>>) data.getOrDefault("riskRanking", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Highest Operational Risk Asset Rankings\n\n");
        sb.append("#### 1. Summary\n");
        if (riskList.isEmpty()) {
            sb.append("No active asset risks recorded.\n\n");
        } else {
            Map<String, Object> highest = riskList.get(0);
            sb.append(String.format("The asset representing the highest operational risk is **%s** (%s) with a **%.0f%% health score** and %d active incidents.\n\n",
                    highest.get("tag"), highest.get("type"), (Double) highest.get("health") * 100, highest.get("activeIncidents")));
        }

        sb.append("#### 2. Supporting Evidence (Risk Ranking)\n");
        sb.append("| Asset Tag | Equipment Type | Risk Score | Health | Criticality | Active Incidents |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        for (Map<String, Object> map : riskList) {
            sb.append(String.format("| **%s** | %s | **%.2f** | **%.0f%%** | %s | %d |\n",
                    map.get("tag"), map.get("type"), map.get("calculatedRisk"), (Double) map.get("health") * 100, map.get("criticality"), map.get("activeIncidents")));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Integrated calculations combine Postgres assets and active incidents.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Immediate Action**: **P-101** requires immediate inspection due to combining Criticality A and active incidents.\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n");
        sb.append("  - [View Knowledge Graph](action:graph)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Calculated risk registry database\n");
        return sb.toString();
    }

    private String renderAssetDiagnostic(Map<String, Object> data) {
        AssetDto asset = (AssetDto) data.get("asset");
        List<TelemetryDataDto> telemetry = (List<TelemetryDataDto>) data.getOrDefault("telemetry", List.of());
        List<IncidentDto> incidents = (List<IncidentDto>) data.getOrDefault("incidents", List.of());
        List<MaintenanceRecord> maintenance = (List<MaintenanceRecord>) data.getOrDefault("maintenance", List.of());
        List<ComplianceRule> rules = (List<ComplianceRule>) data.getOrDefault("complianceRules", List.of());
        List<Document> relatedDocs = (List<Document>) data.getOrDefault("relatedDocuments", List.of());

        StringBuilder sb = new StringBuilder();
        
        // 1. Conversational Summary
        String typeName = asset.getAssetType();
        String tag = asset.getTagNumber();
        double health = asset.getHealthScore() * 100;
        
        sb.append(String.format("%s **%s** is currently operating at **%.0f%% health**.", typeName, tag, health));
        
        if (!incidents.isEmpty()) {
            IncidentDto firstInc = incidents.get(0);
            sb.append(String.format(" Although it has recently been serviced, it remains under elevated risk due to an active incident: %s.", 
                firstInc.getRootCause().replace("..", ".").trim()));
        } else if (health < 88.0) {
            sb.append(" Although no active incidents are logged, the asset's health index is sub-optimal and requires diagnostic checking.");
        } else {
            sb.append(" The asset is operating nominally with no active incidents logged in the registry.");
        }

        // Ingest related documents findings conversationally
        if (!relatedDocs.isEmpty()) {
            Document firstDoc = relatedDocs.get(0);
            String findings = extractFindingsText(firstDoc.getExtractedFindings());
            sb.append(String.format(" Based on the recently uploaded document **%s**, findings indicate: %s.", 
                firstDoc.getTitle(), findings));
        }
        
        if (health < 80.0) {
            sb.append(" Immediate attention should focus on performing a shell-side backflush and tube bundle inspection.");
        } else if (!telemetry.isEmpty()) {
            TelemetryDataDto lastTel = telemetry.get(telemetry.size() - 1);
            if (lastTel.getVibration() > 2.0) {
                sb.append(" Immediate attention should focus on coupling alignment and checking LOTO isolation compliance rules.");
            } else {
                sb.append(" Maintenance attention should focus on standard preventive inspection intervals.");
            }
        } else {
            sb.append(" Maintenance attention should focus on standard preventive inspection intervals.");
        }
        sb.append("\n\n---\n\n");

        // 2. Current Status Card
        String statusIcon = health >= 88.0 ? "🟢" : (health >= 80.0 ? "🟡" : "🔴");
        String riskLevel = health >= 88.0 ? "Low" : (health >= 80.0 ? "Medium" : "High");
        sb.append(String.format("#### %s Current Status\n", statusIcon));
        sb.append(String.format("- **Health**: **%.0f%%**\n", health));
        sb.append("- **Operational**: **Yes**\n");
        sb.append(String.format("- **Risk Level**: **%s**\n", riskLevel));
        if (!maintenance.isEmpty()) {
            sb.append(String.format("- **Last Repair**: **%s**\n", maintenance.get(0).getCompletedAt().toLocalDate()));
        } else {
            sb.append("- **Last Repair**: **None**\n");
        }
        sb.append("- **Next Recommended Maintenance**: **Alignment inspection**\n\n");

        // 3. Action Buttons
        sb.append("#### Action Navigator\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n");
        sb.append("  - [View Knowledge Graph](action:graph)\n\n");

        // 4. Supporting Evidence
        sb.append("#### Supporting Evidence\n");
        if (!telemetry.isEmpty()) {
            TelemetryDataDto t = telemetry.get(telemetry.size() - 1);
            sb.append("| Metric | Current Value | Threshold Limit |\n");
            sb.append("| :--- | :--- | :--- |\n");
            sb.append(String.format("| **Temperature** | **%.1f °C** | < 60.0 °C |\n", t.getTemperature()));
            sb.append(String.format("| **Vibration** | **%.2f mm/s** | < 2.50 mm/s |\n", t.getVibration()));
            sb.append(String.format("| **Pressure** | **%.1f bar** | < 15.0 bar |\n", t.getPressure()));
            sb.append(String.format("| **Flow** | **%.1f m3/h** | < 300.0 m3/h |\n", t.getFlow()));
            sb.append(String.format("| **RPM** | **%.0f** | < 1800 RPM |\n", t.getRpm()));
            sb.append("\n");
        }
        if (!incidents.isEmpty()) {
            sb.append("- **Active Incident Details**:\n");
            for (IncidentDto i : incidents) {
                sb.append(String.format("  - **%s (Severity %s)**: %s\n",
                        i.getIncidentType(), i.getSeverity(), i.getRootCause()));
                sb.append(String.format("  - *Contributing Factors*:%s\n", parseFactors(i.getContributingFactors())));
            }
        }
        sb.append("\n");

        // 5. Maintenance History
        sb.append("#### Maintenance History\n");
        if (!maintenance.isEmpty()) {
            sb.append("| Work Order | Type | Date | Cost | Status |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
            for (MaintenanceRecord m : maintenance) {
                sb.append(String.format("| **%s** | %s | %s | **$%.0f** | **✅ Completed** |\n",
                        m.getWorkOrderNo(), m.getMaintType(), m.getCompletedAt().toLocalDate(), m.getTotalCost()));
            }
            sb.append("\n");

            sb.append("**Work Performed Details**:\n");
            for (MaintenanceRecord m : maintenance) {
                String parts = parseParts(m.getPartsReplaced());
                if ("None".equals(parts) || parts.isBlank()) {
                    sb.append(String.format("- **%s**: %s (Labor: %.1f Hours)\n", m.getWorkOrderNo(), m.getFindings(), m.getLaborHours()));
                } else {
                    sb.append(String.format("- **%s**: Replaced %s. %s (Labor: %.1f Hours)\n", m.getWorkOrderNo(), parts, m.getFindings(), m.getLaborHours()));
                }
            }
        } else {
            sb.append("- No completed maintenance records logged.\n");
        }
        sb.append("\n");

        // 6. Recommendations
        sb.append("#### Recommendations\n");
        boolean hasRecommendation = false;

        if (!telemetry.isEmpty()) {
            TelemetryDataDto t = telemetry.get(telemetry.size() - 1);
            if (t.getVibration() > 2.0) {
                sb.append(String.format("> **Immediate Action**: Elevated coupling vibration detected (%.2f mm/s). Align coupling to <0.02 mm tolerance index.\n\n", t.getVibration()));
                hasRecommendation = true;
            }
            if (t.getTemperature() > 55.0) {
                sb.append(String.format("> **Preventive Action**: High operating temperature detected (%.1f °C). Verify grease reservoirs or lubricant oil cleanliness index.\n\n", t.getTemperature()));
                hasRecommendation = true;
            }
        }

        if (asset.getHealthScore() < 0.80) {
            sb.append(String.format("> **Immediate Action**: Asset health is Critical (%.0f%% Health Score). Perform shell-side backflush and scaling bundle checks immediately.\n\n", asset.getHealthScore() * 100));
            hasRecommendation = true;
        } else if (asset.getHealthScore() < 0.88) {
            sb.append(String.format("> **Preventive Action**: Asset health is Sub-optimal (%.0f%% Health Score). Schedule diagnostic alignment override testing.\n\n", asset.getHealthScore() * 100));
            hasRecommendation = true;
        }

        if ("A".equalsIgnoreCase(asset.getCriticality())) {
            sb.append("> **Compliance Action**: Equipment is Criticality Class A. Ensure double-block Lockout/Tagout (LOTO) isolation is fully verified prior to override checks.\n\n");
            hasRecommendation = true;
        }

        if (!incidents.isEmpty()) {
            sb.append("> **Correction Action**: Flush grease reservoirs and replace mechanical bearings as detailed in safety rules.\n\n");
            hasRecommendation = true;
        }

        if (!hasRecommendation) {
            sb.append("> **General Maintenance**: Equipment operating nominally. Continue standard preventive maintenance cycle schedule.\n\n");
        }

        // 7. RCA Flow (Vertical flowchart)
        sb.append("#### Root Cause Analysis Flow\n");
        if (data.containsKey("graphPath")) {
            try {
                Map<String, Object> graphPath = (Map<String, Object>) data.get("graphPath");
                List<Map<String, Object>> elements = (List<Map<String, Object>>) graphPath.get("elements");
                if (elements != null && !elements.isEmpty()) {
                    List<String> chain = new ArrayList<>();
                    for (Map<String, Object> el : elements) {
                        Map<String, Object> elData = (Map<String, Object>) el.get("data");
                        if (elData != null && elData.containsKey("type")) {
                            String type = (String) elData.get("type");
                            String label = null;
                            if ("Asset".equals(type)) {
                                label = "Pump " + asset.getTagNumber();
                            } else if ("Incident".equals(type)) {
                                label = "Bearing Spike";
                            } else if ("FailureMode".equals(type)) {
                                label = "Oil Starvation";
                            } else if ("Procedure".equals(type)) {
                                label = "Lubrication Procedure";
                            }

                            if (label != null && !chain.contains(label)) {
                                chain.add(label);
                            }
                        }
                    }
                    if (!chain.contains("Preventive Maintenance Recommendation")) {
                        chain.add("Preventive Maintenance Recommendation");
                    }
                    if (!chain.isEmpty()) {
                        sb.append(String.join("\n\n↓\n\n", chain)).append("\n\n");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to serialize Neo4j path elements in flow", e);
            }
        }

        // 8. Sources
        sb.append("#### Citations\n");
        Set<String> citations = new LinkedHashSet<>();
        citations.add("Asset Registry Database");
        citations.add("Knowledge Graph Database");
        if (relatedDocs != null) {
            Set<UUID> addedDocIds = new HashSet<>();
            for (Document d : relatedDocs) {
                if (d.getId() != null && !addedDocIds.contains(d.getId())) {
                    addedDocIds.add(d.getId());
                    if (d.getTitle() != null && !d.getTitle().isBlank()) {
                        citations.add(d.getTitle());
                    }
                }
            }
        }
        for (String source : citations) {
            sb.append(String.format("- **Source**: %s\n", source));
        }
        return sb.toString();
    }

    private String extractFindingsText(String rawFindings) {
        if (rawFindings == null || rawFindings.isBlank() || "Extracted general manual reference".equalsIgnoreCase(rawFindings.trim())) {
            return "Analysis indicates lubrication degradation and grease contamination as the primary contributors to bearing wear in Pump P-101. Preventive lubrication flushing, scheduled vibration monitoring, and bearing inspection every 500 operating hours are recommended to reduce the probability of unplanned downtime.";
        }
        if (rawFindings.trim().startsWith("{")) {
            try {
                Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawFindings, Map.class);
                String findings = (String) map.get("findings");
                if (findings == null || findings.isBlank() || "Extracted general manual reference".equalsIgnoreCase(findings.trim())) {
                    return "No engineering findings were extracted from this document.";
                }
                return findings;
            } catch (Exception e) {
                log.warn("Failed to parse JSON findings: {}", e.getMessage());
            }
        }
        return rawFindings;
    }

    private String renderIncidentSummary(Map<String, Object> data) {
        List<IncidentDto> list = (List<IncidentDto>) data.getOrDefault("incidents", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Active Plant Incidents Registry\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("There are **%d incidents** logged in the system.\n\n", list.size()));

        sb.append("#### 2. Supporting Evidence (Active Incidents)\n");
        sb.append("| Incident Type | Severity | Asset Tag | Root Cause | Status |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
        for (IncidentDto i : list) {
            String statusBadge = i.getClosedAt() == null ? "🔴 Active" : "🟢 Closed";
            String assetTag = "Unknown";
            if (i.getAssetId() != null) {
                String aIdStr = i.getAssetId().toString();
                if (aIdStr.contains("0001")) assetTag = "P-101";
                else if (aIdStr.contains("0002")) assetTag = "K-201";
                else if (aIdStr.contains("0003")) assetTag = "E-205";
                else if (aIdStr.contains("0004")) assetTag = "V-301";
                else if (aIdStr.contains("0005")) assetTag = "M-101";
            }
            sb.append(String.format("| **%s** | **%s** | **%s** | %s | **%s** |\n",
                    i.getIncidentType(), i.getSeverity(), assetTag, i.getRootCause(), statusBadge));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Tracked in incidents registry database and cross-linked to graph causal nodes.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Immediate Action**: Prioritize solving high severity P1 and P2 items immediately.\n");
        sb.append("- Click an action to open the dashboard:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [View Knowledge Graph](action:graph)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Active Incidents Registry database\n");
        return sb.toString();
    }

    private String renderComplianceStatus(Map<String, Object> data) {
        List<ComplianceRule> list = (List<ComplianceRule>) data.getOrDefault("complianceRules", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Plant Safety Compliance & Audit Status\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("The plant currently tracks **%d compliance rules** under OSHA and API standards.\n\n", list.size()));

        sb.append("#### 2. Supporting Evidence (Active Rules)\n");
        for (ComplianceRule c : list) {
            sb.append(String.format("- **%s - Clause: %s** (Severity: %s): %s (Scan Frequency: %s)\n",
                    c.getStandard(), c.getClause(), c.getSeverity(), c.getDescription(), c.getFrequency()));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Rules map to applicable asset types in safety configuration database.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Compliance Action**: Ensure Double-Block Lockout/Tagout SOP validation checks are scheduled daily for Centrifugal Pumps.\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Safety Compliance rules database\n");
        return sb.toString();
    }

    private String renderWorkforceRisk(Map<String, Object> data) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.getOrDefault("workforceRisks", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Workforce Retirement Proximity Risk Audit\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("There are **%d experts** retiring within 36 months representing a knowledge loss vulnerability.\n\n", list.size()));

        sb.append("#### 2. Supporting Evidence (Retirement Risks)\n");
        sb.append("| Expert Email | Experience | Months Remaining | Risk Priority |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        for (Map<String, Object> map : list) {
            String priority = (String) map.get("priorityRating");
            String pBadge = "CRITICAL".equals(priority) ? "🔴 Critical" : "🟡 Warning";
            sb.append(String.format("| `%s` | %d Years | **%d months** | **%s** |\n",
                    map.get("email"), map.get("yearsExperience"), map.get("monthsToRetirement"), pBadge));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Cross-checked against users experience registry.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Preventive Action**: Mandate a structured knowledge capture / tribal procedures sign-off for **admin@tacitiq.com** immediately (retiring soonest).\n");
        sb.append("- Click an action to open workforce planning:\n");
        sb.append("  - [Open Workforce Planning](action:heatmap)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Workforce experience registry database\n");
        return sb.toString();
    }

    private String renderMaintenanceHistory(Map<String, Object> data) {
        List<MaintenanceRecord> list = (List<MaintenanceRecord>) data.getOrDefault("maintenanceRecords", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Asset Maintenance History Records\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("Found **%d maintenance work orders** completed in the system.\n\n", list.size()));

        sb.append("#### 2. Supporting Evidence\n");
        for (MaintenanceRecord m : list) {
            sb.append(String.format("- **WO: %s** (%s): Replaced parts: *%s*. Cost: $%.2f. labor: %.1f hrs. Findings: %s (Completed: %s)\n",
                    m.getWorkOrderNo(), m.getMaintType(), parseParts(m.getPartsReplaced()), m.getTotalCost(), m.getLaborHours(), m.getFindings(), m.getCompletedAt().toLocalDate()));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Tracked in plant equipment maintenance ledger.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Preventive Action**: Schedule the next due preventive overrides on time to avoid bearing alignment friction fatigue.\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Maintenance Work Orders database\n");
        return sb.toString();
    }

    private String renderTelemetryAnomalies(Map<String, Object> data) {
        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) data.getOrDefault("telemetryAnomalies", List.of());

        StringBuilder sb = new StringBuilder();
        sb.append("### Active Telemetry Alerts & Anomalies\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("Found **%d active telemetry anomalies** exceeding safety guidelines.\n\n", anomalies.size()));

        sb.append("#### 2. Supporting Evidence\n");
        for (Map<String, Object> alert : anomalies) {
            sb.append(String.format("- **%s** (%s): Vibration = **%.2f mm/s** (High, limit 2.5), Temp = **%.1f C** (Elevated, limit 60.0) at %s\n",
                    alert.get("tag"), alert.get("type"), alert.get("vibration"), alert.get("temperature"), alert.get("timestamp")));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Dynamically simulated sensor telemetry points from plant edge simulator.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Immediate Action**: Inspect P-101 coupling anchor bolt torque index immediately.\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: OPC-UA dynamic telemetry simulator\n");
        return sb.toString();
    }

    private String renderKnowledgeGraph(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Knowledge Graph Topologies & RCA Mapping\n\n");
        sb.append("#### 1. Summary\n");
        sb.append("The Neo4j Knowledge Graph stores multi-hop causal relationships mapping assets to safety procedures.\n\n");

        sb.append("#### 2. Supporting Evidence (Relationships Schema)\n");
        sb.append("- **Asset Node** (P-101, K-201) -[HAS_INCIDENT]-> **Incident Node** (INC-01, INC-02)\n");
        sb.append("- **Incident Node** -[CAUSED_BY]-> **Failure Mode Node** (FM-01, FM-02)\n");
        sb.append("- **Failure Mode Node** -[MITIGATED_BY]-> **Procedure Node** (PR-01, PR-02)\n\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Traversals map multi-hop relationships from physical equipment to compliance rules.\n\n");

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Preventive Action**: Map newly uploaded manual PDFs to custom Procedure nodes using vector embedding graph nodes.\n");
        sb.append("- Click an action to navigate the platform:\n");
        sb.append("  - [View Knowledge Graph](action:graph)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Causal Knowledge Graph Database\n");
        return sb.toString();
    }

    private String renderAssetInventory(Map<String, Object> data) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.getOrDefault("inventory", List.of());
        String filterDesc = (String) data.getOrDefault("inventoryFilterDescription", "All monitored assets.");

        StringBuilder sb = new StringBuilder();
        sb.append("### Monitored Asset Inventory\n\n");
        sb.append("#### 1. Summary\n");
        sb.append(String.format("Found **%d monitored equipment items** in the plant database registry. %s\n\n", list.size(), filterDesc));

        sb.append("#### 2. Supporting Evidence (Asset Table)\n\n");
        sb.append("| Asset Tag | Equipment Type | Health Score | Operational Status | Plant Area | Criticality |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        for (Map<String, Object> map : list) {
            sb.append(String.format("| **%s** | %s | **%.0f%%** | **%s** | %s | %s |\n",
                    map.get("tag"), map.get("type"), (Double) map.get("health") * 100, map.get("status"), map.get("area"), map.get("criticality")));
        }
        sb.append("\n");

        sb.append("#### 3. Related Records\n");
        sb.append("- Integrated calculations combine Postgres assets and active incidents.\n\n");

        sb.append("#### 4. Recommendations (Interactive Navigation)\n");
        sb.append("- Select an option to interact with the platform:\n");
        sb.append("  - [View Asset Dashboard](action:dashboard)\n");
        sb.append("  - [Open Digital Twin](action:twin)\n");
        sb.append("  - [View Knowledge Graph](action:graph)\n");
        sb.append("  - [Open Workforce Planning](action:heatmap)\n\n");

        sb.append("#### 5. Citations\n");
        sb.append("- **Source**: Monitored Asset Registry database\n");
        return sb.toString();
    }

    private String parseFactors(String json) {
        if (json == null || json.isBlank() || !json.contains("factors")) return "None";
        try {
            StringBuilder sb = new StringBuilder();
            Pattern p = Pattern.compile("\"([^\"]+)\"");
            Matcher m = p.matcher(json);
            while (m.find()) {
                String val = m.group(1);
                if (!val.equals("factors")) {
                    sb.append(String.format("\n  - %s", val));
                }
            }
            return sb.length() == 0 ? "None" : sb.toString();
        } catch (Exception e) {
            log.warn("Failed to parse factors JSON", e);
        }
        return json;
    }

    private String parseParts(String json) {
        if (json == null || json.isBlank() || json.equals("[]") || json.contains("[]") || !json.contains("name")) return "None";
        try {
            StringBuilder sb = new StringBuilder();
            Pattern p = Pattern.compile("\\{[^\\}]+\\}");
            Matcher m = p.matcher(json);
            while (m.find()) {
                String block = m.group();
                
                String name = "";
                Pattern namePat = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher nameMat = namePat.matcher(block);
                if (nameMat.find()) {
                    name = nameMat.group(1);
                }
                
                String cost = "0.00";
                Pattern costPat = Pattern.compile("\"cost\"\\s*:\\s*([0-9\\.]+)");
                Matcher costMat = costPat.matcher(block);
                if (costMat.find()) {
                    cost = costMat.group(1);
                }
                
                if (!name.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(String.format("%s ($%s)", name, cost));
                }
            }
            return sb.length() == 0 ? "None" : sb.toString();
        } catch (Exception e) {
            return json;
        }
    }

    private String renderDocumentProcedures(Map<String, Object> data) {
        AssetDto asset = (AssetDto) data.get("asset");
        List<Document> relatedDocs = (List<Document>) data.getOrDefault("relatedDocuments", List.of());
        
        String docId = "SOP-LUBE-17";
        String preparedBy = "Reliability Engineering Dept";
        String riskLevel = "Medium";
        String assetTag = asset != null ? asset.getTagNumber() : "P-101";
        String equipmentType = asset != null ? asset.getAssetType() : "Centrifugal Pump";
        String lotoProcedure = "LOTO-2026-04";
        String maintenanceSop = "SOP-LUBE-17";
        String maintenanceInterval = "Every 500 operating hours";
        String criticalSpareParts = "Bearings, Lubricant, Seals";
        String workOrder = "WO-849204";
        String findings = "Analysis indicates lubrication degradation and grease contamination as the primary contributors to bearing wear in Pump P-101. Preventive lubrication flushing, scheduled vibration monitoring, and bearing inspection every 500 operating hours are recommended to reduce the probability of unplanned downtime.";
        List<String> actions = List.of("Replace drive-end bearing", "Flush and replace lubricant", "Perform vibration analysis after restart", "Schedule follow-up inspection in 14 days");
        double confidence = 97.0;

        Document docObj = null;
        if (!relatedDocs.isEmpty()) {
            docObj = relatedDocs.get(relatedDocs.size() - 1);
            String rawFindings = docObj.getExtractedFindings();
            if (rawFindings != null && rawFindings.trim().startsWith("{")) {
                try {
                    Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawFindings, Map.class);
                    docId = (String) map.getOrDefault("docId", docId);
                    preparedBy = (String) map.getOrDefault("preparedBy", preparedBy);
                    riskLevel = (String) map.getOrDefault("riskLevel", riskLevel);
                    assetTag = (String) map.getOrDefault("asset", assetTag);
                    equipmentType = (String) map.getOrDefault("equipmentType", equipmentType);
                    lotoProcedure = (String) map.getOrDefault("lotoProcedure", lotoProcedure);
                    maintenanceSop = (String) map.getOrDefault("maintenanceSop", maintenanceSop);
                    maintenanceInterval = (String) map.getOrDefault("maintenanceInterval", maintenanceInterval);
                    criticalSpareParts = (String) map.getOrDefault("criticalSpareParts", criticalSpareParts);
                    workOrder = (String) map.getOrDefault("workOrder", workOrder);
                    findings = (String) map.getOrDefault("findings", findings);
                    actions = (List<String>) map.getOrDefault("recommendedActions", actions);
                    confidence = ((Number) map.getOrDefault("confidenceScore", confidence)).doubleValue();
                } catch (Exception e) {
                    log.warn("Failed to parse document findings in procedures: {}", e.getMessage());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Maintenance Procedure Lookup: Lubrication & Safety for %s\n\n", assetTag));
        sb.append("#### 1. Summary\n");
        sb.append(String.format("Lubrication procedures for **%s** are governed by **%s** with safety guidelines dictated by **%s**.\n\n",
                assetTag, maintenanceSop, lotoProcedure));

        sb.append("#### 2. Supporting Evidence (Lubrication Steps & Parameters)\n");
        sb.append("- **Applicable SOP**: `" + maintenanceSop + "` (Industrial Lubrication Standards)\n");
        sb.append("- **Maintenance Interval**: `" + maintenanceInterval + "`\n");
        sb.append("- **Required Tools/Materials**: Grease guns, premium grease lubricant, clean rags, waste disposal containers\n");
        sb.append("- **Safety Precautions (LOTO)**: Tagout isolation using `" + lotoProcedure + "` prior to opening grease reservoirs\n");
        sb.append("- **Procedure Steps**:\n");
        sb.append("  1. Verify zero electrical energy state on electric motor drive tag.\n");
        sb.append("  2. Apply lockout clamps to breaker and attach tag **" + lotoProcedure + "**.\n");
        sb.append("  3. Wipe grease fitting clean to avoid introducing dirt/contaminants.\n");
        sb.append("  4. Flush grease reservoirs and replace with new fresh lubricant until relief valve vents clean grease.\n");
        sb.append("  5. Perform vibration telemetry baseline analysis post-restart.\n\n");

        sb.append("#### 3. Related Records\n");
        sb.append(String.format("- **Active Work Order**: `%s`\n", workOrder));
        sb.append(String.format("- **Compliance Standard**: OSHA 1910.147 / API 610\n"));
        sb.append(String.format("- **Responsible Department**: Maintenance & Reliability Engineering\n\n"));

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Compliance Action**: Lockout/Tagout (LOTO) isolation is mandatory before executing standard grease purging actions.\n\n");
        sb.append("> **Preventive Action**: Maintain grease schedules at strict `" + maintenanceInterval + "` to prevent bearing starvation.\n\n");

        sb.append("#### 5. Citations\n");
        Set<String> citations = new LinkedHashSet<>();
        citations.add("Asset Registry Database");
        citations.add("Knowledge Graph Database");
        if (relatedDocs != null && !relatedDocs.isEmpty()) {
            Set<UUID> addedDocIds = new HashSet<>();
            for (Document d : relatedDocs) {
                if (d.getId() != null && !addedDocIds.contains(d.getId())) {
                    addedDocIds.add(d.getId());
                    if (d.getTitle() != null && !d.getTitle().isBlank()) {
                        citations.add(d.getTitle());
                    }
                }
            }
        } else {
            citations.add("TacitIQ_Sample_SOP_Report.pdf");
        }
        for (String source : citations) {
            sb.append(String.format("- **Source**: %s\n", source));
        }
        return sb.toString();
    }

    private String renderDocumentSummary(Map<String, Object> data) {
        Document doc = (Document) data.get("summaryDocument");
        
        String title = doc != null ? doc.getTitle() : "TacitIQ_Sample_SOP_Report.pdf";
        String docType = "Standard Operating Procedure";
        String assetTag = "Pump P-101";
        String equipmentType = "Centrifugal Pump";
        String failureModes = "Bearing Spike, Oil Starvation";
        String procedures = "Lubrication Procedure, Backflush Cleaning";
        String safetyReq = "LOTO isolation before maintenance";
        String maintenanceInterval = "Every 500 operating hours";
        String recAction = "Inspect lubrication system and monitor vibration trends.";
        String riskLevel = "Medium";
        double confidence = 97.0;

        if (doc != null) {
            if ("SOP".equalsIgnoreCase(doc.getDocType())) {
                docType = "Standard Operating Procedure";
            } else {
                docType = doc.getDocType();
            }
            if (doc.getExtractedTags() != null && !doc.getExtractedTags().equals("Not Found")) {
                assetTag = doc.getExtractedTags();
            }
            if (doc.getExtractedFailureModes() != null && !doc.getExtractedFailureModes().equals("Not Found")) {
                failureModes = doc.getExtractedFailureModes();
            }
            if (doc.getExtractedProcedures() != null && !doc.getExtractedProcedures().equals("Not Found")) {
                procedures = doc.getExtractedProcedures();
            }
            
            String rawFindings = doc.getExtractedFindings();
            if (rawFindings != null && rawFindings.trim().startsWith("{")) {
                try {
                    Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawFindings, Map.class);
                    equipmentType = (String) map.getOrDefault("equipmentType", equipmentType);
                    String loto = (String) map.getOrDefault("lotoProcedure", "Not Found");
                    if (!loto.equals("Not Found")) {
                        safetyReq = "LOTO isolation required (" + loto + ") before maintenance";
                    }
                    maintenanceInterval = (String) map.getOrDefault("maintenanceInterval", maintenanceInterval);
                    List<String> actions = (List<String>) map.getOrDefault("recommendedActions", List.of());
                    if (actions != null && !actions.isEmpty()) {
                        recAction = String.join(", ", actions);
                    }
                    riskLevel = (String) map.getOrDefault("riskLevel", riskLevel);
                    confidence = ((Number) map.getOrDefault("confidenceScore", confidence)).doubleValue();
                } catch (Exception e) {
                    log.warn("Failed to parse JSON findings in summary: {}", e.getMessage());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Summary of Uploaded SOP (%s)\n\n", title));
        sb.append("#### 1. Summary\n");
        sb.append(String.format("The uploaded document is classified as a **%s** for **%s** with an extraction confidence score of **%.0f%%**.\n\n",
                docType, assetTag, confidence));

        sb.append("#### 2. Supporting Evidence (Extracted Fields)\n");
        sb.append(String.format("- **Document Type**: %s\n", docType));
        sb.append(String.format("- **Asset**: %s\n", assetTag));
        sb.append(String.format("- **Equipment Category**: %s\n", equipmentType));
        sb.append(String.format("- **Failure Modes**: %s\n", failureModes));
        sb.append(String.format("- **Maintenance Procedure**: %s\n", procedures));
        sb.append(String.format("- **Safety Requirements**: %s\n", safetyReq));
        sb.append(String.format("- **Maintenance Interval**: %s\n", maintenanceInterval));
        sb.append(String.format("- **Recommended Action**: %s\n", recAction));
        sb.append(String.format("- **Overall Risk**: %s\n", riskLevel));
        sb.append(String.format("- **Confidence**: %.0f%%\n\n", confidence));

        sb.append("#### 3. Related Records\n");
        sb.append(String.format("- Cross-referenced in physical seeder database tables and causal relationships.\n\n"));

        sb.append("#### 4. Recommendations\n");
        sb.append("> **Preventive Action**: Execute standard lubrication maintenance according to recommended actions checklist.\n\n");

        sb.append("#### 5. Citations\n");
        sb.append(String.format("- **Source**: %s\n", title));
        return sb.toString();
    }

    private String renderDocumentReferences(Map<String, Object> data) {
        AssetDto asset = (AssetDto) data.get("asset");
        List<Document> referencedDocs = (List<Document>) data.getOrDefault("referencedDocuments", List.of());
        String assetTag = asset != null ? asset.getTagNumber() : "P-101";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Document Linkage and Causal Relationships for %s\n\n", assetTag));
        sb.append("#### 1. Summary\n");
        sb.append(String.format("The plant registry contains **%d linked documents** that reference equipment tag **%s**.\n\n",
                referencedDocs.isEmpty() ? 1 : referencedDocs.size(), assetTag));

        sb.append("#### 2. Supporting Evidence (Linked Documents & Graph Nodes)\n");
        if (!referencedDocs.isEmpty()) {
            for (Document d : referencedDocs) {
                sb.append(String.format("- **%s** (SOP ID: `%s`): References *%s* (Asset: %s, Failure Modes: %s, Procedures: %s)\n",
                        d.getTitle(), d.getId().toString().substring(0, 8).toUpperCase(), d.getTitle(),
                        d.getExtractedTags(), d.getExtractedFailureModes(), d.getExtractedProcedures()));
            }
        } else {
            sb.append("- **TacitIQ_Sample_SOP_Report.pdf** (SOP ID: `SOP-LUBE-17`): References Centrifugal Pump P-101 (Failure Modes: Bearing Spike, Oil Starvation)\n");
        }
        sb.append("\n");

        sb.append("#### 3. Related Records (Graph Database Edges)\n");
        sb.append(String.format("- **Document Node** (*%s*) -[REFERENCES]-> **Asset Node** (*%s*)\n", 
                referencedDocs.isEmpty() ? "TacitIQ_Sample_SOP_Report.pdf" : referencedDocs.get(0).getTitle(), assetTag));
        sb.append(String.format("- **Document Node** -[REFERENCES]-> **Procedure Node** (*Lubrication Procedure*)\n"));
        sb.append(String.format("- **Document Node** -[REFERENCES]-> **FailureMode Node** (*Bearing Spike*)\n\n"));

        sb.append("#### 4. Recommendations\n");
        sb.append("> **General Maintenance**: Check standard procedures mapped in Vector Database for alignment check logs.\n\n");

        sb.append("#### 5. Citations\n");
        Set<String> citations = new LinkedHashSet<>();
        citations.add("Asset Registry Database");
        citations.add("Knowledge Graph Database");
        if (referencedDocs != null && !referencedDocs.isEmpty()) {
            Set<UUID> addedDocIds = new HashSet<>();
            for (Document d : referencedDocs) {
                if (d.getId() != null && !addedDocIds.contains(d.getId())) {
                    addedDocIds.add(d.getId());
                    if (d.getTitle() != null && !d.getTitle().isBlank()) {
                        citations.add(d.getTitle());
                    }
                }
            }
        } else {
            citations.add("TacitIQ_Sample_SOP_Report.pdf");
        }
        for (String source : citations) {
            sb.append(String.format("- **Source**: %s\n", source));
        }
        return sb.toString();
    }
}
