package com.assignment.bridgeadapter.abstraction;

import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;

public class RoutineTelemetryAuditAlert extends VitalAlert {

    private final int retryAttempts;

    public RoutineTelemetryAuditAlert(BiometricAlertChannel channel, int retryAttempts) {
        super(channel);
        this.retryAttempts = Math.max(1, retryAttempts);
    }

    @Override
    public DeliveryReport dispatch(VitalsPayload payload, String endpoint) throws ChannelDeliveryException {
        ChannelDeliveryException lastException = null;
        for (int i = 0; i < retryAttempts; i++) {
            try {
                return channel.transmit(payload, endpoint);
            } catch (ChannelDeliveryException e) {
                lastException = e;
            }
        }
        throw new ChannelDeliveryException(
            "Routine telemetry batch failed after " + retryAttempts + " attempts",
            channel.getChannelCode(),
            500,
            lastException
        );
    }
}