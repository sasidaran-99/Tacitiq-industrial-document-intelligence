package com.tacitiq.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final EmbeddingModel embeddingModel;
    private final boolean mockFallback;
    private final Random random = new Random();

    public EmbeddingService(
            @Autowired(required = false) EmbeddingModel embeddingModel,
            @Value("${tacitiq.ai.mock-fallback:true}") boolean mockFallback) {
        this.embeddingModel = embeddingModel;
        this.mockFallback = mockFallback;
    }

    public float[] getEmbedding(String text) {
        if (embeddingModel != null && !mockFallback) {
            try {
                log.info("Generating embedding via Spring AI model...");
                List<Double> doubleList = embeddingModel.embed(text);
                float[] floatVector = new float[doubleList.size()];
                for (int i = 0; i < doubleList.size(); i++) {
                    floatVector[i] = doubleList.get(i).floatValue();
                }
                return floatVector;
            } catch (Exception e) {
                log.warn("Failed to generate embedding via Vertex AI. Falling back to mock generator.", e);
            }
        }
        
        // Fallback: Generate mock deterministic embedding for the vector dimension (768)
        log.info("Generating mock deterministic embedding vector...");
        float[] mockVector = new float[768];
        long hash = text.hashCode();
        Random deterministicRandom = new Random(hash);
        for (int i = 0; i < 768; i++) {
            mockVector[i] = (float) (deterministicRandom.nextGaussian() * 0.1);
        }
        return mockVector;
    }

    public String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
