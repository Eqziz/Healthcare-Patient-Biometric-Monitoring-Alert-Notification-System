package com.assignment.bridgeadapter.implementor;

import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;

import java.time.Instant;
import java.util.UUID;

public class IcuPagerRadioChannel implements BiometricAlertChannel {

    @Override
    public String getChannelCode() {
        return "ICU_RADIO_PAGER";
    }

    @Override
    public boolean supportsSeverity(AlertSeverity severity) {
        return severity == AlertSeverity.CRITICAL;
    }

    @Override
    public DeliveryReport transmit(VitalsPayload payload, String destinationEndpoint) throws ChannelDeliveryException {
        if (payload.severity() != AlertSeverity.CRITICAL) {
            throw new ChannelDeliveryException("Radio pager restricted to CRITICAL alerts", getChannelCode(), 403);
        }
        return new DeliveryReport(
            "PAGER-" + UUID.randomUUID(),
            getChannelCode(),
            Instant.now(),
            true
        );
    }
}