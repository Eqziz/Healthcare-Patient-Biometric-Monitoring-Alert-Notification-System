package com.assignment.bridgeadapter.domain;

import java.time.Instant;
import java.util.Objects;

public record VitalsPayload(
    String patientId,
    String metricType,
    double observedValue,
    double thresholdValue,
    AlertSeverity severity,
    Instant timestamp
) {
    public VitalsPayload {
        Objects.requireNonNull(patientId, "patientId must not be null");
        Objects.requireNonNull(metricType, "metricType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}