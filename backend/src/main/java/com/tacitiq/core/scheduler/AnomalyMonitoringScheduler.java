package com.tacitiq.core.scheduler;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class AnomalyMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnomalyMonitoringScheduler.class);
    
    private final AssetService assetService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    public AnomalyMonitoringScheduler(AssetService assetService, SimpMessagingTemplate messagingTemplate) {
        this.assetService = assetService;
        this.messagingTemplate = messagingTemplate;
    }

    // Run telemetry checks every 30 seconds to simulate real-time edge triggers
    @Scheduled(fixedRate = 30000)
    public void monitorTelemetryAnomalies() {
        log.info("Executing background telemetry anomaly detection check...");

        List<AssetDto> assets = assetService.getAllAssets();
        if (assets.isEmpty()) {
            return;
        }

        // Select a random asset to evaluate
        AssetDto asset = assets.get(random.nextInt(assets.size()));
        
        // Randomly simulate an anomaly if health score is lower
        double anomalyThreshold = asset.getHealthScore() < 0.85 ? 0.35 : 0.05;
        if (random.nextDouble() < anomalyThreshold) {
            log.warn("Anomaly detected on asset: {}", asset.getTagNumber());
            
            double value = 4.2 + (random.nextDouble() * 2.0); // mm/s vibration surge
            broadcastAnomalyEvent(asset.getTagNumber(), "Vibration", value, "mm/s");
        }
    }

    private void broadcastAnomalyEvent(String tagNumber, String sensorType, double value, String unit) {
        try {
            messagingTemplate.convertAndSend("/topic/events", Map.of(
                    "eventType", "TelemetryAnomaly",
                    "payload", Map.of(
                            "tagNumber", tagNumber,
                            "sensorType", sensorType,
                            "value", String.format("%.2f", value),
                            "unit", unit,
                            "timestamp", OffsetDateTime.now().toString()
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast telemetry anomaly over websocket: {}", e.getMessage());
        }
    }
}
