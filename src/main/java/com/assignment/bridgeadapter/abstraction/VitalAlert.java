package com.assignment.bridgeadapter.abstraction;

import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;

import java.util.Objects;

/**
 * Bridge Base Abstraction.
 * Strictly decoupled from any concrete implementation or adapted classes.
 */
public abstract class VitalAlert {

    protected BiometricAlertChannel channel;

    protected VitalAlert(BiometricAlertChannel channel) {
        this.channel = Objects.requireNonNull(channel, "BiometricAlertChannel must not be null");
    }

    public void setChannel(BiometricAlertChannel channel) {
        this.channel = Objects.requireNonNull(channel, "BiometricAlertChannel must not be null");
    }

    public BiometricAlertChannel getChannel() {
        return channel;
    }

    public abstract DeliveryReport dispatch(VitalsPayload payload, String endpoint) throws ChannelDeliveryException;
}