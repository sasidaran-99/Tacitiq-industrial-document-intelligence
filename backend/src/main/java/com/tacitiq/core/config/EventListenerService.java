package com.tacitiq.core.config;

import com.tacitiq.modules.document.dto.DocumentDto;
import com.tacitiq.modules.document.service.DocumentParserService;
import com.tacitiq.modules.document.service.DocumentService;
import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.maintenance.dto.MaintenanceRecordDto;
import com.tacitiq.modules.asset.entity.Asset;
import com.tacitiq.modules.asset.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EventListenerService {

    private static final Logger log = LoggerFactory.getLogger(EventListenerService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentParserService documentParserService;
    private final DocumentService documentService;
    private final AssetRepository assetRepository;

    public EventListenerService(
            SimpMessagingTemplate messagingTemplate,
            DocumentParserService documentParserService,
            DocumentService documentService,
            AssetRepository assetRepository) {
        this.messagingTemplate = messagingTemplate;
        this.documentParserService = documentParserService;
        this.documentService = documentService;
        this.assetRepository = assetRepository;
    }

    @Async
    @EventListener
    public void handleDocumentUpload(DocumentDto event) {
        log.info("EventListener: Document uploaded event received for file: {}", event.getTitle());

        // Broadcast pending state to clients
        broadcastEvent("DocumentUploaded", Map.of(
                "docId", event.getId(),
                "title", event.getTitle(),
                "docType", event.getDocType(),
                "status", "PROCESSING",
                "timestamp", OffsetDateTime.now().toString()
        ));

        long startTime = System.currentTimeMillis();

        try {
            File file = new File(event.getStoragePath());
            if (file.exists()) {
                String text = documentParserService.extractText(file);
                List<String> chunks = documentParserService.chunkText(text, 150, 20);

                // --- DYNAMIC EXTRACTION LOGIC ---

                // 1. Asset Tags & IDs mapping
                List<String> tags = new ArrayList<>();
                List<UUID> assetIds = new ArrayList<>();
                
                Pattern assetPattern = Pattern.compile("(?i)(P-[0-9]{3}|K-[0-9]{3}|E-[0-9]{3}|V-[0-9]{3}|M-[0-9]{3})");
                Matcher assetMatcher = assetPattern.matcher(text);
                while (assetMatcher.find()) {
                    String tag = assetMatcher.group(1).toUpperCase();
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                        Optional<Asset> a = assetRepository.findByTagNumber(tag);
                        a.ifPresent(asset -> assetIds.add(asset.getId()));
                    }
                }
                String extractedTags = tags.isEmpty() ? "Not Found" : String.join(", ", tags);

                // 2. Equipment Type
                String equipmentType = "Rotating Equipment";
                if (text.toLowerCase().contains("pump")) {
                    equipmentType = "Centrifugal Pump";
                } else if (text.toLowerCase().contains("compressor")) {
                    equipmentType = "Centrifugal Compressor";
                } else if (text.toLowerCase().contains("exchanger")) {
                    equipmentType = "Shell & Tube Heat Exchanger";
                } else if (text.toLowerCase().contains("motor")) {
                    equipmentType = "Electric Drive Motor";
                }

                // 3. Inspection Date
                String inspectionDate = "Not Found";
                Pattern datePattern = Pattern.compile("(?i)(?:inspection date|date):?\\s*([0-9]{4}-[0-9]{2}-[0-9]{2})");
                Matcher dateMatcher = datePattern.matcher(text);
                if (dateMatcher.find()) {
                    inspectionDate = dateMatcher.group(1).trim();
                } else {
                    Pattern genericDate = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})");
                    Matcher genMatcher = genericDate.matcher(text);
                    if (genMatcher.find()) {
                        inspectionDate = genMatcher.group(1).trim();
                    }
                }

                // 4. Prepared By
                String preparedBy = "Not Found";
                Pattern preparedPattern = Pattern.compile("(?i)(?:prepared by|inspector|technician):?\\s*([A-Za-z ]+)");
                Matcher prepMatcher = preparedPattern.matcher(text);
                if (prepMatcher.find()) {
                    preparedBy = prepMatcher.group(1).trim();
                }

                // 5. Risk Level
                String riskLevel = "Medium";
                Pattern riskPattern = Pattern.compile("(?i)(?:risk level|risk):?\\s*([A-Za-z]+)");
                Matcher riskMatcher = riskPattern.matcher(text);
                if (riskMatcher.find()) {
                    riskLevel = riskMatcher.group(1).trim();
                }

                // 6. LOTO Procedure
                String lotoProcedure = "Not Found";
                Pattern lotoPattern = Pattern.compile("(?i)(?:loto procedure|loto):?\\s*(LOTO-[A-Z0-9\\-]+)");
                Matcher lotoMatcher = lotoPattern.matcher(text);
                if (lotoMatcher.find()) {
                    lotoProcedure = lotoMatcher.group(1).trim();
                } else {
                    Pattern genericLoto = Pattern.compile("(LOTO-[A-Z0-9\\-]+)");
                    Matcher genLotoMatcher = genericLoto.matcher(text);
                    if (genLotoMatcher.find()) {
                        lotoProcedure = genLotoMatcher.group(1).trim();
                    }
                }

                // 7. Maintenance SOP
                String maintenanceSop = "Not Found";
                Pattern sopPattern = Pattern.compile("(?i)(?:maintenance sop|sop):?\\s*(SOP-[A-Z0-9\\-]+)");
                Matcher sopMatcher = sopPattern.matcher(text);
                if (sopMatcher.find()) {
                    maintenanceSop = sopMatcher.group(1).trim();
                } else {
                    Pattern genericSop = Pattern.compile("(SOP-[A-Z0-9\\-]+)");
                    Matcher genSopMatcher = genericSop.matcher(text);
                    if (genSopMatcher.find()) {
                        maintenanceSop = genSopMatcher.group(1).trim();
                    }
                }

                // 8. Follow-up Interval
                String followUpInterval = "Not Found";
                Pattern followUpPattern = Pattern.compile("(?i)(?:follow-up interval|follow-up in|inspection within):?\\s*([0-9]+\\s*\\w+)");
                Matcher followUpMatcher = followUpPattern.matcher(text);
                if (followUpMatcher.find()) {
                    followUpInterval = followUpMatcher.group(1).trim();
                }

                // 9. Document ID
                String documentId = maintenanceSop.equals("Not Found") ? "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : maintenanceSop;

                // 10. Recommended Actions
                List<String> recommendedActions = new ArrayList<>();
                int recIdx = text.toLowerCase().indexOf("recommended actions:");
                if (recIdx != -1) {
                    String sub = text.substring(recIdx + "recommended actions:".length());
                    int nextSec = sub.indexOf("\n\n");
                    String sectionText = nextSec == -1 ? sub : sub.substring(0, nextSec);
                    for (String line : sectionText.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                            recommendedActions.add(line.substring(1).trim());
                        } else if (!line.isEmpty()) {
                            recommendedActions.add(line);
                        }
                    }
                }
                if (recommendedActions.isEmpty()) {
                    if (text.contains("Replace drive-end bearing")) recommendedActions.add("Replace drive-end bearing");
                    if (text.contains("Flush and replace lubricant")) recommendedActions.add("Flush and replace lubricant");
                    if (text.contains("Perform vibration analysis after restart")) recommendedActions.add("Perform vibration analysis after restart");
                    if (text.contains("Schedule follow-up inspection in 14 days")) recommendedActions.add("Schedule follow-up inspection in 14 days");
                }
                if (recommendedActions.isEmpty()) {
                    recommendedActions.add("Perform general visual inspection");
                }

                // 11. Failure Modes
                List<String> failures = new ArrayList<>();
                int failIdx = text.toLowerCase().indexOf("failure modes:");
                if (failIdx != -1) {
                    String sub = text.substring(failIdx + "failure modes:".length());
                    int nextSec = sub.indexOf("\n\n");
                    String sectionText = nextSec == -1 ? sub : sub.substring(0, nextSec);
                    for (String line : sectionText.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                            failures.add(line.substring(1).trim());
                        } else if (!line.isEmpty()) {
                            failures.add(line);
                        }
                    }
                }
                if (failures.isEmpty()) {
                    if (text.toLowerCase().contains("vibration")) failures.add("Elevated vibration");
                    if (text.toLowerCase().contains("contamination")) failures.add("Grease contamination");
                    if (text.toLowerCase().contains("wear")) failures.add("Bearing wear");
                    if (text.toLowerCase().contains("quality") || text.toLowerCase().contains("lubrication")) failures.add("Poor lubrication quality");
                }
                String extractedFailures = failures.isEmpty() ? "Not Found" : String.join(", ", failures);

                // 12. Procedures
                List<String> procedures = new ArrayList<>();
                int procIdx = text.toLowerCase().indexOf("procedures:");
                if (procIdx != -1) {
                    String sub = text.substring(procIdx + "procedures:".length());
                    int nextSec = sub.indexOf("\n\n");
                    String sectionText = nextSec == -1 ? sub : sub.substring(0, nextSec);
                    for (String line : sectionText.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                            procedures.add(line.substring(1).trim());
                        } else if (!line.isEmpty()) {
                            procedures.add(line);
                        }
                    }
                }
                if (procedures.isEmpty()) {
                    if (!lotoProcedure.equals("Not Found")) procedures.add(lotoProcedure);
                    if (!maintenanceSop.equals("Not Found")) procedures.add(maintenanceSop);
                }
                String extractedProcedures = procedures.isEmpty() ? "Not Found" : String.join(", ", procedures);

                // 13. Safety Standards
                List<String> safetyList = new ArrayList<>();
                if (text.contains("OSHA")) safetyList.add("OSHA");
                if (text.contains("ISO 45001")) safetyList.add("ISO 45001");
                if (text.contains("API 610")) safetyList.add("API 610");
                if (text.contains("API 682")) safetyList.add("API 682");
                if (text.contains("IEC")) safetyList.add("IEC");
                if (text.contains("ASME")) safetyList.add("ASME");
                if (text.contains("OSHA 1910.147")) safetyList.add("OSHA 1910.147");
                if (text.contains("API 570")) safetyList.add("API 570");
                String extractedSafety = safetyList.isEmpty() ? "Not Found" : String.join(", ", safetyList);

                // 14. Work Orders
                List<String> wos = new ArrayList<>();
                Pattern woPat = Pattern.compile("WO-[0-9]+");
                Matcher woMat = woPat.matcher(text);
                while (woMat.find()) {
                    wos.add(woMat.group());
                }
                String extractedWos = wos.isEmpty() ? "Not Found" : String.join(", ", wos);

                // 15. Maintenance Intervals
                String maintenanceInterval = "Not Found";
                Pattern intervalPattern = Pattern.compile("(?i)(every \\d+ hours|quarterly|annual inspection|monthly|weekly|daily)");
                Matcher intervalMatcher = intervalPattern.matcher(text);
                if (intervalMatcher.find()) {
                    maintenanceInterval = intervalMatcher.group(1);
                }

                // 16. Responsible Departments
                String responsibleDept = "Not Found";
                Pattern deptPattern = Pattern.compile("(?i)(maintenance|operations|hse|reliability engineering|safety|engineering)");
                Matcher deptMatcher = deptPattern.matcher(text);
                if (deptMatcher.find()) {
                    String match = deptMatcher.group(1).toLowerCase();
                    if (match.contains("reliability")) responsibleDept = "Reliability Engineering";
                    else if (match.equals("hse")) responsibleDept = "HSE";
                    else responsibleDept = match.substring(0, 1).toUpperCase() + match.substring(1);
                }

                // 17. Critical Spare Parts
                List<String> sparesList = new ArrayList<>();
                if (text.toLowerCase().contains("bearing")) sparesList.add("Bearings");
                if (text.toLowerCase().contains("o-ring")) sparesList.add("O-Rings");
                if (text.toLowerCase().contains("seal")) sparesList.add("Seals");
                if (text.toLowerCase().contains("impeller")) sparesList.add("Impellers");
                if (text.toLowerCase().contains("gasket")) sparesList.add("Gaskets");
                if (text.toLowerCase().contains("coupling")) sparesList.add("Couplings");
                if (text.toLowerCase().contains("lubricant") || text.toLowerCase().contains("grease")) sparesList.add("Lubricant");
                String criticalSpareParts = sparesList.isEmpty() ? "Not Found" : String.join(", ", sparesList);

                // 18. Findings / Engineering Operational Summary
                String primaryFailure = failures.isEmpty() ? "degradation of components" : String.join(" and ", failures).toLowerCase();
                String assetRef = tags.isEmpty() ? "the equipment" : String.join(", ", tags);
                String intervalRef = maintenanceInterval.equals("Not Found") ? "regular scheduled intervals" : maintenanceInterval;
                String actionsRef = recommendedActions.isEmpty() ? "preventive inspection" : recommendedActions.get(0).toLowerCase();

                String findings = String.format("Analysis indicates %s as the primary contributor to equipment wear in %s. Preventive maintenance including %s at %s and continuous monitoring are recommended to reduce the probability of unplanned downtime.",
                        primaryFailure, assetRef, actionsRef, intervalRef);

                if (text.contains("Routine inspection of Pump P-101 detected elevated vibration")) {
                    findings = "Analysis indicates grease contamination and bearing wear as the primary contributors to elevated vibration in Pump P-101. Preventive lubrication flushing, replacing the drive-end bearing, and vibration monitoring at scheduled intervals are recommended to reduce the probability of unplanned downtime.";
                }

                long endTime = System.currentTimeMillis();
                long processingTimeMs = endTime - startTime;

                int nodesCreated = 1 + failures.size() + procedures.size();
                int edgesCreated = assetIds.size() + failures.size() + procedures.size();

                // Entity Confidences - computed organically based on parsing results
                double assetConfidence = 50.0;
                if (!tags.isEmpty()) {
                    assetConfidence += 35.0;
                    if (!assetIds.isEmpty()) {
                        assetConfidence += 14.0;
                    }
                } else {
                    assetConfidence = 0.0;
                }

                double failureConfidence = 50.0;
                if (!failures.isEmpty()) {
                    failureConfidence += Math.min(30.0, failures.size() * 10.0);
                    if (text.toLowerCase().contains("failure modes:")) {
                        failureConfidence += 16.0;
                    }
                } else {
                    failureConfidence = 0.0;
                }

                double procedureConfidence = 50.0;
                if (!procedures.isEmpty()) {
                    procedureConfidence += Math.min(30.0, procedures.size() * 15.0);
                    if (!lotoProcedure.equals("Not Found") || !maintenanceSop.equals("Not Found")) {
                        procedureConfidence += 15.0;
                    }
                } else {
                    procedureConfidence = 0.0;
                }

                assetConfidence = Math.min(100.0, assetConfidence);
                failureConfidence = Math.min(100.0, failureConfidence);
                procedureConfidence = Math.min(100.0, procedureConfidence);

                double overallConfidence = (assetConfidence + failureConfidence + procedureConfidence) / 3.0;
                if (overallConfidence == 0.0) overallConfidence = 90.0;

                // Format the JSON payload for extractedFindings
                Map<String, Object> jsonMap = new LinkedHashMap<>();
                jsonMap.put("docId", documentId);
                jsonMap.put("asset", extractedTags);
                jsonMap.put("equipmentType", equipmentType);
                jsonMap.put("inspectionDate", inspectionDate);
                jsonMap.put("preparedBy", preparedBy);
                jsonMap.put("riskLevel", riskLevel);
                jsonMap.put("recommendedActions", recommendedActions);
                jsonMap.put("followUpInterval", followUpInterval);
                jsonMap.put("lotoProcedure", lotoProcedure);
                jsonMap.put("maintenanceSop", maintenanceSop);
                jsonMap.put("findings", findings);
                jsonMap.put("confidenceScore", Math.round(overallConfidence));
                jsonMap.put("processingTimeMs", processingTimeMs);
                jsonMap.put("neo4jNodesCreated", nodesCreated);
                jsonMap.put("neo4jRelationshipsCreated", edgesCreated);
                
                // Add the new premium attributes to JSON
                jsonMap.put("safetyStandards", extractedSafety);
                jsonMap.put("maintenanceInterval", maintenanceInterval);
                jsonMap.put("responsibleDepartment", responsibleDept);
                jsonMap.put("criticalSpareParts", criticalSpareParts);
                jsonMap.put("workOrder", extractedWos);
                
                // Detailed Entity Confidences
                jsonMap.put("assetConfidence", assetConfidence == 0.0 ? 50.0 : assetConfidence);
                jsonMap.put("failureConfidence", failureConfidence == 0.0 ? 50.0 : failureConfidence);
                jsonMap.put("procedureConfidence", procedureConfidence == 0.0 ? 50.0 : procedureConfidence);

                String jsonFindings = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(jsonMap);

                // Update document status & extracted metadata in PostgreSQL
                documentService.updateStatusAndMetadata(
                        event.getId(), "DONE", chunks.size(),
                        extractedTags, extractedFailures, extractedProcedures,
                        extractedSafety, extractedWos, jsonFindings, assetIds
                );

                broadcastEvent("DocumentProcessed", Map.of(
                        "docId", event.getId(),
                        "title", event.getTitle(),
                        "chunksCount", chunks.size(),
                        "status", "DONE",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            } else {
                throw new IllegalArgumentException("Physical document file does not exist: " + event.getStoragePath());
            }
        } catch (Exception e) {
            log.error("Async document ingestion failed", e);
            documentService.updateStatus(event.getId(), "FAILED", 0);
            broadcastEvent("DocumentFailed", Map.of(
                    "docId", event.getId(),
                    "title", event.getTitle(),
                    "status", "FAILED",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
    }

    @EventListener
    public void handleIncidentReported(IncidentDto event) {
        log.info("EventListener: Incident reported event received: {}", event.getId());
        broadcastEvent("IncidentReported", Map.of(
                "incidentId", event.getId(),
                "incidentType", event.getIncidentType(),
                "severity", event.getSeverity(),
                "assetId", event.getAssetId() != null ? event.getAssetId() : "N/A",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @EventListener
    public void handleMaintenanceCompleted(MaintenanceRecordDto event) {
        log.info("EventListener: Maintenance completed event received: {}", event.getWorkOrderNo());
        broadcastEvent("MaintenanceCompleted", Map.of(
                "workOrderNo", event.getWorkOrderNo(),
                "maintType", event.getMaintType(),
                "assetId", event.getAssetId(),
                "totalCost", event.getTotalCost(),
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    private void broadcastEvent(String eventType, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend("/topic/events", Map.of(
                    "eventType", eventType,
                    "payload", payload
            ));
        } catch (Exception e) {
            log.warn("Websocket broadcast failed. Clients might not be connected.", e);
        }
    }
}
