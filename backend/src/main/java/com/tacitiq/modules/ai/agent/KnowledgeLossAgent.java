package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.auth.entity.User;
import com.tacitiq.modules.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeLossAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLossAgent.class);

    private final UserRepository userRepository;

    public KnowledgeLossAgent(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> evaluateRetirementRisk() {
        log.info("Knowledge Loss Agent scanning engineer profiles for retirement proximity risk...");

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> riskQueue = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            if (user.getRetirementDate() == null) {
                continue;
            }

            long monthsToRetirement = ChronoUnit.MONTHS.between(today, user.getRetirementDate());
            if (monthsToRetirement <= 36) { // Analyze anyone retiring within 3 years
                double departureProximityWeight = Math.max(0.1, 1.0 - (monthsToRetirement / 36.0));
                
                // Calculate risk score: Criticality * Departure proximity * Document gap
                // Mock: document coverage gap is higher for niche areas
                double docGapScore = user.getYearsExperience() > 30 ? 0.75 : 0.40;
                double compositeRiskScore = departureProximityWeight * docGapScore;

                Map<String, Object> record = new HashMap<>();
                record.put("engineerId", user.getId());
                record.put("email", user.getEmail());
                record.put("monthsToRetirement", monthsToRetirement);
                record.put("yearsExperience", user.getYearsExperience());
                record.put("expertiseAreas", user.getExpertiseAreas());
                record.put("compositeRiskScore", Math.min(1.0, compositeRiskScore));
                record.put("priorityRating", compositeRiskScore > 0.6 ? "CRITICAL" : "ELEVATED");
                riskQueue.add(record);
            }
        }
        return riskQueue;
    }
}
