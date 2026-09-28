package com.assignment.bridgeadapter.exception;

/**
 * Standard checked domain exception enforced by the BiometricAlertChannel contract.
 */
public class ChannelDeliveryException extends Exception {
    private final String channelName;
    private final int failureCode;

    public ChannelDeliveryException(String message, String channelName, int failureCode) {
        super(message);
        this.channelName = channelName;
        this.failureCode = failureCode;
    }

    public ChannelDeliveryException(String message, String channelName, int failureCode, Throwable cause) {
        super(message, cause);
        this.channelName = channelName;
        this.failureCode = failureCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public int getFailureCode() {
        return failureCode;
    }
}