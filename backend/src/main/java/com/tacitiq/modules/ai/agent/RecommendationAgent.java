package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecommendationAgent {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAgent.class);

    private final AssetService assetService;
    private final FailurePredictionAgent failurePredictionAgent;

    public RecommendationAgent(AssetService assetService, FailurePredictionAgent failurePredictionAgent) {
        this.assetService = assetService;
        this.failurePredictionAgent = failurePredictionAgent;
    }

    public List<Map<String, Object>> generateAssetRecommendations(UUID assetId) {
        log.info("Recommendation Agent aggregating operational indicators for asset: {}", assetId);

        AssetDto asset = assetService.getAssetById(assetId);
        Map<String, Object> prediction = failurePredictionAgent.predictFailureRisk(assetId);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        double failureProb = (double) prediction.get("failureProbability30Days");

        if (failureProb > 0.5) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", UUID.randomUUID());
            rec.put("title", "Initiate Bearing Alignment Verification");
            rec.put("category", "Maintenance");
            rec.put("rationale", "Inboard vibration levels are elevated. Combined failure risk within 30 days is " + Math.round(failureProb * 100) + "%.");
            rec.put("suggestedAction", "Inspect coupling alignment. Torque anchor bolts to 120 Nm. Validate lubricant oil cleanliness index.");
            rec.put("sparePartsPreorder", List.of(Map.of("name", "SKF Radial Bearing", "sku", "SKF-6312", "estCost", 180.00)));
            rec.put("urgency", "HIGH");
            recommendations.add(rec);
        }

        // Add compliance related recommendations if it's a critical pump/compressor
        if (asset.getAssetType().contains("Pump") || asset.getAssetType().contains("Compressor")) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", UUID.randomUUID());
            rec.put("title", "Review LOTO Verification Checklist");
            rec.put("category", "Compliance");
            rec.put("rationale", "Asset matches OSHA 1910.147 requirements. Active compliance audit has flagged LOTO checks gaps on secondary switchboards.");
            rec.put("suggestedAction", "Verify isolation locks on circuit breakers before allowing engineering clearance sign-offs.");
            rec.put("sparePartsPreorder", List.of());
            rec.put("urgency", "MEDIUM");
            recommendations.add(rec);
        }

        return recommendations;
    }
}
