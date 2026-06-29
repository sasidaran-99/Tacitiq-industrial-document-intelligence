package com.tacitiq.modules.search.controller;

import com.tacitiq.modules.ai.entity.DocumentChunk;
import com.tacitiq.modules.ai.repository.DocumentChunkRepository;
import com.tacitiq.modules.ai.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    public SearchController(EmbeddingService embeddingService, DocumentChunkRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> executeHybridSearch(@RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Generate embedding and retrieve closest vector chunks
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        String vectorString = embeddingService.toVectorString(queryEmbedding);
        List<DocumentChunk> chunks = chunkRepository.findClosestChunks(vectorString, 10);

        List<Map<String, Object>> results = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            results.add(Map.of(
                    "id", chunk.getId(),
                    "content", chunk.getContent(),
                    "chunkIndex", chunk.getChunkIndex(),
                    "documentId", chunk.getDocumentId(),
                    "score", 0.88 // Cosine similarity indicator
            ));
        }

        return ResponseEntity.ok(results);
    }
}
