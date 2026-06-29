package com.tacitiq.modules.graph.controller;

import com.tacitiq.modules.graph.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/nodes")
    public ResponseEntity<Map<String, Object>> getGraphNodes() {
        return ResponseEntity.ok(graphService.getCytoscapeData());
    }

    @GetMapping("/traverse")
    public ResponseEntity<Map<String, Object>> getRcaPath(@RequestParam String tagNumber) {
        return ResponseEntity.ok(graphService.getAssetRcaPath(tagNumber));
    }
}
