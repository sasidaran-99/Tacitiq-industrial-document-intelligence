package com.tacitiq.modules.graph.service;

import com.tacitiq.modules.graph.entity.AssetNode;
import com.tacitiq.modules.graph.entity.IncidentNode;
import com.tacitiq.modules.graph.entity.FailureModeNode;
import com.tacitiq.modules.graph.entity.ProcedureNode;
import com.tacitiq.modules.graph.repository.AssetNodeRepository;
import com.tacitiq.modules.document.repository.DocumentRepository;
import com.tacitiq.modules.asset.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    
    private final AssetNodeRepository assetNodeRepository;
    private final DocumentRepository documentRepository;
    private final AssetRepository assetRepository;

    public GraphService(
            @Autowired(required = false) AssetNodeRepository assetNodeRepository,
            DocumentRepository documentRepository,
            AssetRepository assetRepository) {
        this.assetNodeRepository = assetNodeRepository;
        this.documentRepository = documentRepository;
        this.assetRepository = assetRepository;
    }

    public Map<String, Object> getCytoscapeData() {
        Map<String, Object> data = null;
        if (assetNodeRepository != null) {
            try {
                log.info("Fetching graph data from Neo4j...");
                List<AssetNode> assets = assetNodeRepository.findAll();
                if (!assets.isEmpty()) {
                    data = parseEntitiesToCytoscape(assets);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch graph from Neo4j database. Falling back to mock graph schemas.", e);
            }
        }
        if (data == null) {
            data = generateMockCytoscapeData();
        }
        return appendDocumentNodes(data);
    }

    public Map<String, Object> getAssetRcaPath(String tagNumber) {
        Map<String, Object> data = null;
        if (assetNodeRepository != null) {
            try {
                log.info("Fetching RCA traversal path for asset: {}", tagNumber);
                Optional<AssetNode> assetOpt = assetNodeRepository.findByTagNumber(tagNumber);
                if (assetOpt.isPresent()) {
                    data = parseEntitiesToCytoscape(List.of(assetOpt.get()));
                }
            } catch (Exception e) {
                log.warn("Neo4j path traversal failed. Falling back to mock path generator.", e);
            }
        }
        if (data == null) {
            data = generateMockRcaPath(tagNumber);
        }
        return appendDocumentNodes(data);
    }

    private Map<String, Object> appendDocumentNodes(Map<String, Object> data) {
        if (documentRepository == null || data == null || !data.containsKey("elements")) {
            return data;
        }
        
        List<Map<String, Object>> elements = new ArrayList<>((List<Map<String, Object>>) data.get("elements"));
        Set<String> existingNodes = new HashSet<>();
        for (Map<String, Object> elem : elements) {
            Map<String, Object> elemData = (Map<String, Object>) elem.get("data");
            if (elemData != null && elemData.containsKey("id")) {
                existingNodes.add((String) elemData.get("id"));
            }
        }

        try {
            List<com.tacitiq.modules.document.entity.Document> docs = documentRepository.findAll();
            for (com.tacitiq.modules.document.entity.Document d : docs) {
                String docId = d.getTitle();
                if (!existingNodes.contains(docId)) {
                    addNode(elements, docId, d.getTitle(), "Document");
                    existingNodes.add(docId);
                }
                
                if (d.getRelatedAssets() != null) {
                    for (UUID assetId : d.getRelatedAssets()) {
                        Optional<com.tacitiq.modules.asset.entity.Asset> assetOpt = assetRepository.findById(assetId);
                        if (assetOpt.isPresent()) {
                            addEdge(elements, docId, assetOpt.get().getTagNumber(), "REFERENCES");
                        }
                    }
                }

                // Add relationships for extracted Failure Modes
                if (d.getExtractedFailureModes() != null && !d.getExtractedFailureModes().isBlank() && !"None".equalsIgnoreCase(d.getExtractedFailureModes())) {
                    String[] fms = d.getExtractedFailureModes().split(",");
                    for (String fm : fms) {
                        String fmTrimmed = fm.trim();
                        if (!fmTrimmed.isEmpty()) {
                            if (!existingNodes.contains(fmTrimmed)) {
                                addNode(elements, fmTrimmed, fmTrimmed, "FailureMode");
                                existingNodes.add(fmTrimmed);
                            }
                            addEdge(elements, docId, fmTrimmed, "REFERENCES");
                        }
                    }
                }

                // Add relationships for extracted Procedures
                if (d.getExtractedProcedures() != null && !d.getExtractedProcedures().isBlank() && !"None".equalsIgnoreCase(d.getExtractedProcedures())) {
                    String[] procs = d.getExtractedProcedures().split(",");
                    for (String proc : procs) {
                        String procTrimmed = proc.trim();
                        if (!procTrimmed.isEmpty()) {
                            if (!existingNodes.contains(procTrimmed)) {
                                addNode(elements, procTrimmed, procTrimmed, "Procedure");
                                existingNodes.add(procTrimmed);
                            }
                            addEdge(elements, docId, procTrimmed, "REFERENCES");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to append document nodes to graph", e);
        }
        
        return Map.of("elements", elements);
    }

    private Map<String, Object> parseEntitiesToCytoscape(List<AssetNode> assets) {
        List<Map<String, Object>> elements = new ArrayList<>();
        Map<String, Boolean> addedNodes = new HashMap<>();

        for (AssetNode a : assets) {
            String assetId = a.getTagNumber();
            if (assetId == null) assetId = a.getId();

            if (assetId != null && !addedNodes.containsKey(assetId)) {
                addedNodes.put(assetId, true);
                addNode(elements, assetId, "Pump " + assetId, "Asset");
            }

            if (a.getIncidents() != null) {
                for (IncidentNode i : a.getIncidents()) {
                    String incidentId = i.getId();
                    if (incidentId != null) {
                        if (!addedNodes.containsKey(incidentId)) {
                            addedNodes.put(incidentId, true);
                            addNode(elements, incidentId, i.getDescription(), "Incident");
                        }
                        addEdge(elements, assetId, incidentId, "HAS_INCIDENT");

                        FailureModeNode f = i.getFailureMode();
                        if (f != null) {
                            String fmName = f.getName();
                            if (fmName != null) {
                                if (!addedNodes.containsKey(fmName)) {
                                    addedNodes.put(fmName, true);
                                    addNode(elements, fmName, fmName.equals("FM-01") ? "Why 2: Oil Starvation" : "Why 2: Arc Hazard", "FailureMode");
                                }
                                addEdge(elements, incidentId, fmName, "CAUSED_BY");

                                if (f.getProcedures() != null) {
                                    for (ProcedureNode p : f.getProcedures()) {
                                        String procId = p.getId();
                                        if (procId != null) {
                                            if (!addedNodes.containsKey(procId)) {
                                                addedNodes.put(procId, true);
                                                addNode(elements, procId, p.getTitle(), "Procedure");
                                            }
                                            addEdge(elements, fmName, procId, "MITIGATED_BY");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Map.of("elements", elements);
    }

    private Map<String, Object> generateMockCytoscapeData() {
        List<Map<String, Object>> elements = new ArrayList<>();

        // Add Asset Nodes
        addNode(elements, "P-101", "Pump P-101", "Asset");
        addNode(elements, "K-201", "Compressor K-201", "Asset");
        addNode(elements, "E-205", "Exchanger E-205", "Asset");

        // Add Incident Nodes
        addNode(elements, "INC-01", "Bearing Starvation", "Incident");
        addNode(elements, "INC-02", "LOTO Violation", "Incident");

        // Add Failure Modes
        addNode(elements, "FM-01", "Bearing Seizure", "FailureMode");
        addNode(elements, "FM-02", "Electrical Arc Fault", "FailureMode");

        // Add Procedures
        addNode(elements, "PR-01", "Lubrication Guideline", "Procedure");
        addNode(elements, "PR-02", "LOTO Double Isolation SOP", "Procedure");

        // Add Relationships (Edges)
        addEdge(elements, "P-101", "INC-01", "HAS_INCIDENT");
        addEdge(elements, "K-201", "INC-02", "HAS_INCIDENT");
        addEdge(elements, "INC-01", "FM-01", "CAUSED_BY");
        addEdge(elements, "INC-02", "FM-02", "CAUSED_BY");
        addEdge(elements, "FM-01", "PR-01", "MITIGATED_BY");
        addEdge(elements, "FM-02", "PR-02", "MITIGATED_BY");

        return Map.of("elements", elements);
    }

    private Map<String, Object> generateMockRcaPath(String tagNumber) {
        List<Map<String, Object>> elements = new ArrayList<>();

        // Generate specific 5-Why path for Pump P-101
        addNode(elements, tagNumber, "Asset: " + tagNumber, "Asset");
        addNode(elements, "INC-01", "Why 1: Bearing Temperature Spike", "Incident");
        addNode(elements, "FM-01", "Why 2: Lubricant Starvation", "FailureMode");
        addNode(elements, "FM-02", "Why 3: Lubrication Pipe Blockage", "FailureMode");
        addNode(elements, "PR-01", "Why 4: Missing Maintenance Checks", "Procedure");
        addNode(elements, "PR-02", "Why 5: Outdated Preventive SOP", "Procedure");

        // Connections
        addEdge(elements, tagNumber, "INC-01", "HAS_INCIDENT");
        addEdge(elements, "INC-01", "FM-01", "CAUSED_BY");
        addEdge(elements, "FM-01", "FM-02", "PRECEDED_BY");
        addEdge(elements, "FM-02", "PR-01", "MITIGATED_BY");
        addEdge(elements, "PR-01", "PR-02", "REPLACES");

        return Map.of("elements", elements);
    }

    private void addNode(List<Map<String, Object>> list, String id, String label, String group) {
        Map<String, Object> node = new HashMap<>();
        node.put("data", Map.of("id", id, "label", label, "type", group));
        list.add(node);
    }

    private void addEdge(List<Map<String, Object>> list, String source, String target, String rel) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("data", Map.of(
                "id", source + "-" + target,
                "source", source,
                "target", target,
                "label", rel
        ));
        list.add(edge);
    }
}
