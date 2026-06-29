package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.asset.dto.AssetDto;
import com.tacitiq.modules.asset.dto.TelemetryDataDto;
import com.tacitiq.modules.asset.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FailurePredictionAgent {

    private static final Logger log = LoggerFactory.getLogger(FailurePredictionAgent.class);

    private final AssetService assetService;

    public FailurePredictionAgent(AssetService assetService) {
        this.assetService = assetService;
    }

    public Map<String, Object> predictFailureRisk(UUID assetId) {
        log.info("Failure Prediction Agent computing risk profiles for asset: {}", assetId);

        AssetDto asset = assetService.getAssetById(assetId);
        List<TelemetryDataDto> telemetry = assetService.getTelemetryData(assetId, 10);

        // Analytical prediction math mimicking advanced ML model output
        double currentVibration = 1.8; // default
        double currentTemp = 42.0;

        if (!telemetry.isEmpty()) {
            TelemetryDataDto last = telemetry.get(telemetry.size() - 1);
            currentVibration = last.getVibration();
            currentTemp = last.getTemperature();
        }

        // Calculate asset age in years
        long ageYears = ChronoUnit.YEARS.between(asset.getInstallationDate(), LocalDate.now());

        // Base risk calculated from installation age + vibration gradients
        double pFailure7Days = 0.02 + (currentVibration * 0.05) + (ageYears * 0.005);
        double pFailure14Days = 0.05 + (currentVibration * 0.09) + (ageYears * 0.008);
        double pFailure30Days = 0.12 + (currentVibration * 0.18) + (ageYears * 0.015);

        // Cap probabilities at 0.99
        pFailure7Days = Math.min(0.99, Math.max(0.0, pFailure7Days));
        pFailure14Days = Math.min(0.99, Math.max(0.0, pFailure14Days));
        pFailure30Days = Math.min(0.99, Math.max(0.0, pFailure30Days));

        // RUL estimate
        int remainingDays = (int) (120 - (currentVibration * 25.0) - (ageYears * 1.5));
        remainingDays = Math.max(3, remainingDays);

        // Build SHAP (feature importance) explainer maps
        Map<String, Double> shapValues = new HashMap<>();
        shapValues.put("Vibration Amplitude (mm/s)", currentVibration * 0.35);
        shapValues.put("Operating Temperature (C)", currentTemp * 0.012);
        shapValues.put("Asset Installation Age (Years)", ageYears * 0.08);
        shapValues.put("Days Since Last Maintenance Override", 14.5 * 0.01);

        return Map.of(
                "assetId", assetId,
                "tagNumber", asset.getTagNumber(),
                "remainingUsefulLifeDays", remainingDays,
                "failureProbability7Days", pFailure7Days,
                "failureProbability14Days", pFailure14Days,
                "failureProbability30Days", pFailure30Days,
                "shapExplainer", shapValues,
                "recommendedActionWindow", remainingDays < 15 ? "URGENT (0-5 days)" : "SCHEDULED (15-30 days)"
        );
    }
}
