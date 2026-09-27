package com.assignment.bridgeadapter.abstraction;

import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;

public class CriticalIcuEmergencyAlert extends VitalAlert {

    public CriticalIcuEmergencyAlert(BiometricAlertChannel channel) {
        super(channel);
    }

    @Override
    public DeliveryReport dispatch(VitalsPayload payload, String endpoint) throws ChannelDeliveryException {
        if (payload.severity() != AlertSeverity.CRITICAL) {
            throw new IllegalArgumentException("CriticalIcuEmergencyAlert requires CRITICAL severity level");
        }
        // Business logic before delegating to bridge implementor
        return channel.transmit(payload, endpoint);
    }
}