package com.assignment.bridgeadapter.legacy;

import java.nio.charset.StandardCharsets;

/**
 * Third-party library representing an incompatible legacy HL7 v2 over raw MLLP/TCP connection.
 * Cannot be modified directly.
 */
public class LegacyHl7TcpGateway {

    private final String ipAddress;
    private final int port;

    public LegacyHl7TcpGateway(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Incompatible API:
     * 1. Reversed order of parameters compared to BiometricAlertChannel.
     * 2. Takes raw byte buffer instead of domain record.
     * 3. Uses timeout parameter.
     * 4. Returns status code object instead of throwing checked domain exceptions.
     * 5. Throws LegacySocketTimeoutException.
     */
    public Hl7TransmissionStatus sendRawHl7Frame(int timeoutMillis, byte[] mllpFrame, String facilityId)
            throws LegacySocketTimeoutException {
        if (timeoutMillis <= 0) {
            throw new LegacySocketTimeoutException("MLLP socket timeout after " + timeoutMillis + "ms");
        }
        if (mllpFrame == null || mllpFrame.length == 0) {
            return new Hl7TransmissionStatus(Hl7TransmissionStatus.ERR_BUFFER_OVERFLOW, "Empty MLLP payload");
        }
        String content = new String(mllpFrame, StandardCharsets.US_ASCII);
        if (content.contains("CORRUPT")) {
            return new Hl7TransmissionStatus(Hl7TransmissionStatus.ERR_CHECKSUM_MISMATCH, "Frame checksum rejected");
        }
        if (content.contains("REJECT")) {
            return new Hl7TransmissionStatus(Hl7TransmissionStatus.ERR_ACK_REJECTED, "HL7 Application NAK received");
        }
        return new Hl7TransmissionStatus(Hl7TransmissionStatus.STATUS_OK, "ACK_ACK_001");
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}