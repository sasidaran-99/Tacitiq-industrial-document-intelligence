package com.tacitiq.modules.ai.service;

import com.tacitiq.modules.ai.service.QueryRouter.QueryIntent;
import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.dto.TelemetryDataDto;
import com.tacitiq.modules.asset.service.AssetService;
import com.tacitiq.modules.auth.entity.User;
import com.tacitiq.modules.auth.repository.UserRepository;
import com.tacitiq.modules.compliance.entity.ComplianceRule;
import com.tacitiq.modules.compliance.repository.ComplianceRuleRepository;
import com.tacitiq.modules.graph.service.GraphService;
import com.tacitiq.modules.incident.dto.IncidentDto;
import com.tacitiq.modules.incident.service.IncidentService;
import com.tacitiq.modules.maintenance.entity.MaintenanceRecord;
import com.tacitiq.modules.maintenance.repository.MaintenanceRecordRepository;
import com.tacitiq.modules.document.repository.DocumentRepository;
import com.tacitiq.modules.document.entity.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    private final AssetService assetService;
    private final IncidentService incidentService;
    private final UserRepository userRepository;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final GraphService graphService;
    private final DocumentRepository documentRepository;

    public ContextBuilder(
            AssetService assetService,
            IncidentService incidentService,
            UserRepository userRepository,
            ComplianceRuleRepository complianceRuleRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            GraphService graphService,
            DocumentRepository documentRepository) {
        this.assetService = assetService;
        this.incidentService = incidentService;
        this.userRepository = userRepository;
        this.complianceRuleRepository = complianceRuleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.graphService = graphService;
        this.documentRepository = documentRepository;
    }

    public Map<String, Object> buildContext(QueryIntent intent, String targetAssetTag, boolean missingAssetContext, Map<String, Object> chatContext, String query) {
        Map<String, Object> context = new HashMap<>();
        context.put("intent", intent.name());

        // Get general lists for mapping/checks
        List<AssetDto> allAssets = assetService.getAllAssets();
        List<IncidentDto> allIncidents = incidentService.getAllIncidents();
        
        // Populate missing asset context flag and options
        if (missingAssetContext) {
            context.put("missingAssetContext", true);
            context.put("availableAssetTags", allAssets.stream().map(AssetDto::getTagNumber).collect(Collectors.toList()));
            return context;
        }

        // Find matching asset if a tag is detected
        AssetDto matchingAsset = null;
        if (targetAssetTag != null) {
            matchingAsset = allAssets.stream()
                    .filter(a -> a.getTagNumber().equalsIgnoreCase(targetAssetTag))
                    .findFirst()
                    .orElse(null);
            
            if (matchingAsset != null) {
                context.put("targetAsset", matchingAsset);
            } else {
                context.put("invalidAssetTag", targetAssetTag);
                context.put("availableAssetTags", allAssets.stream().map(AssetDto::getTagNumber).collect(Collectors.toList()));
            }
        }

        // Handle ranking check
        if (chatContext.containsKey("rankingType")) {
            String rankingType = (String) chatContext.get("rankingType");
            context.put("rankingType", rankingType);
            
            // Sort to find targeted asset
            List<AssetDto> sorted = new ArrayList<>(allAssets);
            if ("lowest".equals(rankingType)) {
                sorted.sort(Comparator.comparing(AssetDto::getHealthScore));
            } else {
                sorted.sort(Comparator.comparing(AssetDto::getHealthScore).reversed());
            }
            if (!sorted.isEmpty()) {
                AssetDto targetRankAsset = sorted.get(0);
                context.put("targetRankingAsset", targetRankAsset);
                
                // Fetch dynamic simulator telemetry for it
                List<TelemetryDataDto> telemetry = assetService.getTelemetryData(targetRankAsset.getId(), 5);
                context.put("rankingTelemetry", telemetry);
            }
        }

        switch (intent) {
            case PLANT_SUMMARY:
                buildPlantSummary(context, allAssets, allIncidents);
                break;
            case HEALTH_INDEX:
                buildHealthIndex(context, allAssets);
                break;
            case HIGHEST_RISK_ASSETS:
                buildHighestRiskAssets(context, allAssets, allIncidents);
                break;
            case ASSET_DIAGNOSTIC:
                if (matchingAsset != null) {
                    buildAssetDiagnostic(context, matchingAsset, allIncidents);
                } else {
                    context.put("missingAssetContext", true);
                    context.put("availableAssetTags", allAssets.stream().map(AssetDto::getTagNumber).collect(Collectors.toList()));
                }
                break;
            case INCIDENT_SUMMARY:
                context.put("incidents", allIncidents);
                break;
            case COMPLIANCE_STATUS:
                context.put("complianceRules", complianceRuleRepository.findAll());
                break;
            case WORKFORCE_RISK:
                buildWorkforceRisk(context);
                break;
            case MAINTENANCE_HISTORY:
                if (matchingAsset != null) {
                    List<MaintenanceRecord> records = maintenanceRecordRepository.findByAssetId(matchingAsset.getId());
                    context.put("maintenanceRecords", records);
                } else if (!context.containsKey("invalidAssetTag")) {
                    context.put("missingAssetContext", true);
                    context.put("availableAssetTags", allAssets.stream().map(AssetDto::getTagNumber).collect(Collectors.toList()));
                } else {
                    context.put("maintenanceRecords", List.of());
                }
                break;
            case TELEMETRY_ANOMALIES:
                buildTelemetryAnomalies(context, allAssets);
                break;
            case KNOWLEDGE_GRAPH:
                context.put("graph", graphService.getCytoscapeData());
                break;
            case ASSET_INVENTORY:
                buildAssetInventory(context, allAssets, allIncidents, query);
                break;
            case DOCUMENT_PROCEDURES:
                buildDocumentProcedures(context, matchingAsset);
                break;
            case DOCUMENT_SUMMARY:
                buildDocumentSummary(context);
                break;
            case DOCUMENT_REFERENCES:
                buildDocumentReferences(context, matchingAsset);
                break;
            case OUT_OF_SCOPE:
                context.put("isOutOfScope", true);
                break;
            default:
                break;
        }

        return context;
    }

    private void buildPlantSummary(Map<String, Object> context, List<AssetDto> assets, List<IncidentDto> incidents) {
        int assetCount = assets.size();
        double avgHealth = assets.stream().mapToDouble(AssetDto::getHealthScore).average().orElse(0.0);
        int activeIncidentsCount = incidents.size();
        int totalComplianceRules = complianceRuleRepository.findAll().size();
        
        context.put("totalAssets", assetCount);
        context.put("averageHealthScore", avgHealth);
        context.put("activeIncidentsCount", activeIncidentsCount);
        context.put("totalComplianceRules", totalComplianceRules);
        context.put("criticalAssetsCount", assets.stream().filter(a -> "A".equals(a.getCriticality())).count());

        List<AssetDto> bottomAssets = assets.stream()
                .sorted(Comparator.comparing(AssetDto::getHealthScore))
                .limit(3)
                .collect(Collectors.toList());
        context.put("topRiskAssets", bottomAssets);
    }

    private void buildHealthIndex(Map<String, Object> context, List<AssetDto> assets) {
        double avgHealth = assets.stream().mapToDouble(AssetDto::getHealthScore).average().orElse(0.0);
        context.put("averageHealthScore", avgHealth);
        context.put("assetsHealthList", assets.stream()
                .map(a -> Map.of("tag", a.getTagNumber(), "type", a.getAssetType(), "health", a.getHealthScore()))
                .collect(Collectors.toList()));
    }

    private void buildHighestRiskAssets(Map<String, Object> context, List<AssetDto> assets, List<IncidentDto> incidents) {
        List<Map<String, Object>> riskList = new ArrayList<>();
        for (AssetDto asset : assets) {
            long assetIncidents = incidents.stream()
                    .filter(i -> asset.getId().equals(i.getAssetId()))
                    .count();
            double riskScore = (1.0 - asset.getHealthScore()) * ("A".equals(asset.getCriticality()) ? 1.5 : 1.0);
            
            Map<String, Object> riskInfo = new HashMap<>();
            riskInfo.put("tag", asset.getTagNumber());
            riskInfo.put("type", asset.getAssetType());
            riskInfo.put("health", asset.getHealthScore());
            riskInfo.put("criticality", asset.getCriticality());
            riskInfo.put("activeIncidents", assetIncidents);
            riskInfo.put("calculatedRisk", riskScore);
            riskList.add(riskInfo);
        }

        riskList.sort((r1, r2) -> Double.compare((Double) r2.get("calculatedRisk"), (Double) r1.get("calculatedRisk")));
        context.put("riskRanking", riskList);
    }

    private void buildAssetDiagnostic(Map<String, Object> context, AssetDto asset, List<IncidentDto> incidents) {
        UUID id = asset.getId();
        context.put("asset", asset);

        List<TelemetryDataDto> telemetry = assetService.getTelemetryData(id, 5);
        context.put("telemetry", telemetry);

        List<IncidentDto> assetIncidents = incidents.stream()
                .filter(i -> id.equals(i.getAssetId()))
                .collect(Collectors.toList());
        context.put("incidents", assetIncidents);

        List<MaintenanceRecord> maintenance = maintenanceRecordRepository.findByAssetId(id);
        context.put("maintenance", maintenance);

        Map<String, Object> graphPath = graphService.getAssetRcaPath(asset.getTagNumber());
        context.put("graphPath", graphPath);

        List<ComplianceRule> complianceRules = complianceRuleRepository.findAll().stream()
                .filter(r -> r.getApplicableAssets() != null && r.getApplicableAssets().contains(asset.getAssetType()))
                .collect(Collectors.toList());
        context.put("complianceRules", complianceRules);

        // Fetch related industrial documents
        try {
            List<Document> docs = documentRepository.findAll().stream()
                    .filter(d -> d.getRelatedAssets() != null && d.getRelatedAssets().contains(id))
                    .collect(Collectors.toList());
            context.put("relatedDocuments", docs);
        } catch (Exception e) {
            log.warn("Failed to fetch related documents for diagnostic context", e);
            context.put("relatedDocuments", List.of());
        }
    }

    private void buildWorkforceRisk(Map<String, Object> context) {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> riskQueue = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            if (user.getRetirementDate() == null) {
                continue;
            }
            long monthsToRetirement = ChronoUnit.MONTHS.between(today, user.getRetirementDate());
            if (monthsToRetirement <= 36) {
                Map<String, Object> record = new HashMap<>();
                record.put("email", user.getEmail());
                record.put("monthsToRetirement", monthsToRetirement);
                record.put("yearsExperience", user.getYearsExperience());
                record.put("expertiseAreas", user.getExpertiseAreas());
                record.put("priorityRating", monthsToRetirement <= 12 ? "CRITICAL" : "ELEVATED");
                riskQueue.add(record);
            }
        }
        context.put("workforceRisks", riskQueue);
    }

    private void buildTelemetryAnomalies(Map<String, Object> context, List<AssetDto> assets) {
        List<Map<String, Object>> anomaliesList = new ArrayList<>();
        for (AssetDto asset : assets) {
            List<TelemetryDataDto> data = assetService.getTelemetryData(asset.getId(), 3);
            if (!data.isEmpty()) {
                TelemetryDataDto latest = data.get(data.size() - 1);
                boolean isAnomaly = latest.getVibration() > 2.5 || latest.getTemperature() > 60.0;
                
                if (isAnomaly) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("tag", asset.getTagNumber());
                    alert.put("type", asset.getAssetType());
                    alert.put("vibration", latest.getVibration());
                    alert.put("temperature", latest.getTemperature());
                    alert.put("pressure", latest.getPressure());
                    alert.put("timestamp", latest.getTimestamp());
                    anomaliesList.add(alert);
                }
            }
        }
        context.put("telemetryAnomalies", anomaliesList);
    }

    private void buildAssetInventory(Map<String, Object> context, List<AssetDto> assets, List<IncidentDto> incidents, String query) {
        String lowerQuery = query.toLowerCase();
        List<AssetDto> filtered = new ArrayList<>(assets);
        String filterDesc = "All monitored assets registered in the plant system.";

        if (lowerQuery.contains("pump")) {
            filtered = filtered.stream().filter(a -> a.getAssetType().toLowerCase().contains("pump")).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: Pumps only.";
        } else if (lowerQuery.contains("compressor")) {
            filtered = filtered.stream().filter(a -> a.getAssetType().toLowerCase().contains("compressor")).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: Compressors only.";
        } else if (lowerQuery.contains("cdu")) {
            filtered = filtered.stream().filter(a -> a.getPlantArea().toLowerCase().contains("cdu")).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: crude distillation unit (CDU) plant area.";
        } else if (lowerQuery.contains("criticality a")) {
            filtered = filtered.stream().filter(a -> "A".equalsIgnoreCase(a.getCriticality())).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: Criticality Class A equipment.";
        } else if (lowerQuery.contains("unhealthy") || lowerQuery.contains("poor") || lowerQuery.contains("maintenance") || lowerQuery.contains("require")) {
            filtered = filtered.stream().filter(a -> a.getHealthScore() < 0.88).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: Assets requiring immediate overhaul or with sub-nominal health score (<88%).";
        } else if (lowerQuery.contains("healthy") || lowerQuery.contains("good")) {
            filtered = filtered.stream().filter(a -> a.getHealthScore() >= 0.88).collect(Collectors.toList());
            filterDesc = "Filtered monitored assets: Healthy equipment (health score >= 88%).";
        }

        List<Map<String, Object>> inventoryList = new ArrayList<>();
        for (AssetDto asset : filtered) {
            long activeIncidents = incidents.stream()
                    .filter(i -> asset.getId().equals(i.getAssetId()))
                    .count();
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", asset.getId().toString());
            info.put("tag", asset.getTagNumber());
            info.put("type", asset.getAssetType());
            info.put("health", asset.getHealthScore());
            info.put("criticality", asset.getCriticality());
            info.put("area", asset.getPlantArea());
            info.put("oem", asset.getOemModel());
            info.put("status", asset.getHealthScore() >= 0.88 ? "🟢 Healthy" : (asset.getHealthScore() >= 0.80 ? "🟡 Warning" : "🔴 Needs Attention"));
            info.put("activeIncidents", activeIncidents);
            inventoryList.add(info);
        }

        context.put("inventory", inventoryList);
        context.put("inventoryFilterDescription", filterDesc);
    }

    private void buildDocumentProcedures(Map<String, Object> context, AssetDto asset) {
        List<Document> allDocs = documentRepository.findAll();
        List<Document> matchingDocs = allDocs.stream()
                .filter(d -> (asset != null && d.getRelatedAssets() != null && d.getRelatedAssets().contains(asset.getId()))
                        || d.getTitle().toLowerCase().contains("sop")
                        || d.getTitle().toLowerCase().contains("lube"))
                .collect(Collectors.toList());
        context.put("relatedDocuments", matchingDocs);
        if (asset != null) {
            context.put("asset", asset);
        }
    }

    private void buildDocumentSummary(Map<String, Object> context) {
        List<Document> allDocs = documentRepository.findAll();
        if (!allDocs.isEmpty()) {
            Document doc = allDocs.get(allDocs.size() - 1);
            context.put("summaryDocument", doc);
        }
    }

    private void buildDocumentReferences(Map<String, Object> context, AssetDto asset) {
        List<Document> allDocs = documentRepository.findAll();
        List<Document> matchingDocs = allDocs.stream()
                .filter(d -> asset != null && d.getRelatedAssets() != null && d.getRelatedAssets().contains(asset.getId()))
                .collect(Collectors.toList());
        context.put("referencedDocuments", matchingDocs);
        if (asset != null) {
            context.put("asset", asset);
        }
    }
}

