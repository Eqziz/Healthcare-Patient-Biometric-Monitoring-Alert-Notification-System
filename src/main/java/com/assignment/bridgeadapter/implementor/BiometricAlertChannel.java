package com.assignment.bridgeadapter.implementor;

import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;

/**
 * Bridge Implementor Interface.
 */
public interface BiometricAlertChannel {
    String getChannelCode();
    boolean supportsSeverity(com.assignment.bridgeadapter.domain.AlertSeverity severity);
    DeliveryReport transmit(VitalsPayload payload, String destinationEndpoint) throws ChannelDeliveryException;
}