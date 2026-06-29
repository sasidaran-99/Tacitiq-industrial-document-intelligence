package com.tacitiq.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;

@Component
public class ComplianceScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(ComplianceScanScheduler.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    public ComplianceScanScheduler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Run compliance scans every 60 seconds to demonstrate background check updates
    @Scheduled(fixedRate = 60000)
    public void executeRegulatoryAudit() {
        log.info("Executing background regulatory compliance check...");

        if (random.nextDouble() < 0.20) { // 20% chance of flagging compliance alerts
            log.warn("Compliance governance rule violation detected!");
            
            broadcastComplianceEvent(
                    "OSHA 1910.147",
                    "P-101",
                    "Asset procedure lacks isolation lockout signature validation."
            );
        }
    }

    private void broadcastComplianceEvent(String standard, String assetTag, String description) {
        try {
            messagingTemplate.convertAndSend("/topic/events", Map.of(
                    "eventType", "ComplianceAlert",
                    "payload", Map.of(
                            "standard", standard,
                            "assetTag", assetTag,
                            "description", description,
                            "timestamp", OffsetDateTime.now().toString()
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast compliance alert over websocket: {}", e.getMessage());
        }
    }
}
