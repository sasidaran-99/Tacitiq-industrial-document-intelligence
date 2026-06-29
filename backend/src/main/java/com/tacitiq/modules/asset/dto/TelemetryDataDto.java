package com.tacitiq.modules.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryDataDto {
    private OffsetDateTime timestamp;
    private Double temperature; // Celsius
    private Double vibration;   // mm/s (velocity RMS)
    private Double pressure;    // bar
    private Double flow;        // m3/h
    private Double rpm;         // RPM
}
