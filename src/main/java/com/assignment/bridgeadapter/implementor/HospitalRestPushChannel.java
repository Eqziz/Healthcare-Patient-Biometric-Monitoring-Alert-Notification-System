package com.assignment.bridgeadapter.implementor;

import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;

import java.time.Instant;
import java.util.UUID;

public class HospitalRestPushChannel implements BiometricAlertChannel {

    @Override
    public String getChannelCode() {
        return "REST_EMR_PUSH";
    }

    @Override
    public boolean supportsSeverity(AlertSeverity severity) {
        return severity == AlertSeverity.LOW || severity == AlertSeverity.MODERATE;
    }

    @Override
    public DeliveryReport transmit(VitalsPayload payload, String destinationEndpoint) throws ChannelDeliveryException {
        if (destinationEndpoint == null || destinationEndpoint.isBlank()) {
            throw new ChannelDeliveryException("Invalid EMR REST endpoint", getChannelCode(), 400);
        }
        // Simulated network transmission
        return new DeliveryReport(
            "REST-" + UUID.randomUUID(),
            getChannelCode(),
            Instant.now(),
            true
        );
    }
}