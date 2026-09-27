package com.assignment.bridgeadapter.adapter;

import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;
import com.assignment.bridgeadapter.legacy.Hl7TransmissionStatus;
import com.assignment.bridgeadapter.legacy.LegacyHl7TcpGateway;
import com.assignment.bridgeadapter.legacy.LegacySocketTimeoutException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * Adapter bringing the incompatible LegacyHl7TcpGateway into the BiometricAlertChannel Implementor role.
 * Encapsulates all legacy format conversions and translates failure models without leaking legacy types.
 */
public class LegacyHl7GatewayAdapter implements BiometricAlertChannel {

    private final LegacyHl7TcpGateway legacyGateway;
    private final int defaultTimeoutMs;
    private final String facilityId;

    public LegacyHl7GatewayAdapter(LegacyHl7TcpGateway legacyGateway, int defaultTimeoutMs, String facilityId) {
        this.legacyGateway = Objects.requireNonNull(legacyGateway, "legacyGateway must not be null");
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId must not be null");
    }

    @Override
    public String getChannelCode() {
        return "LEGACY_HL7_TCP";
    }

    @Override
    public boolean supportsSeverity(AlertSeverity severity) {
        return true; // Supports all levels via legacy central telemetry bus
    }

    @Override
    public DeliveryReport transmit(VitalsPayload payload, String destinationEndpoint) throws ChannelDeliveryException {
        // Step 1: Format VitalsPayload into HL7 MSH/OBX ASCII packet
        byte[] hl7Frame = convertToHl7Frame(payload, destinationEndpoint);

        try {
            // Step 2: Invoke incompatible signature
            Hl7TransmissionStatus status = legacyGateway.sendRawHl7Frame(defaultTimeoutMs, hl7Frame, facilityId);

            // Step 3: Translate bitwise status code into domain exception
            if (!status.isSuccess()) {
                throw translateStatusCode(status);
            }

            return new DeliveryReport(
                "HL7-" + status.getDiagnosticBlob(),
                getChannelCode(),
                Instant.now(),
                true
            );
        } catch (LegacySocketTimeoutException e) {
            // Step 4: Catch legacy unchecked exception and re-throw standard domain checked exception
            throw new ChannelDeliveryException(
                "Legacy HL7 gateway timed out: " + e.getMessage(),
                getChannelCode(),
                504,
                e
            );
        }
    }

    private byte[] convertToHl7Frame(VitalsPayload payload, String targetEndpoint) {
        String frame = String.format(
            "MSH|^~\\&|MONITOR|%s|DEST|%s||ORU^R01|Q%d|P|2.3\nOBX|1|NM|%s||%.2f|||%s||||%s\n",
            facilityId,
            targetEndpoint,
            payload.timestamp().toEpochMilli(),
            payload.metricType(),
            payload.observedValue(),
            payload.severity(),
            payload.patientId()
        );
        return frame.getBytes(StandardCharsets.US_ASCII);
    }

    private ChannelDeliveryException translateStatusCode(Hl7TransmissionStatus status) {
        return switch (status.getStatusCode()) {
            case Hl7TransmissionStatus.ERR_BUFFER_OVERFLOW ->
                new ChannelDeliveryException("Payload rejected by gateway buffer overflow: " + status.getDiagnosticBlob(), getChannelCode(), 413);
            case Hl7TransmissionStatus.ERR_CHECKSUM_MISMATCH ->
                new ChannelDeliveryException("Data transmission corruption detected: " + status.getDiagnosticBlob(), getChannelCode(), 422);
            case Hl7TransmissionStatus.ERR_ACK_REJECTED ->
                new ChannelDeliveryException("Hospital server rejected HL7 message: " + status.getDiagnosticBlob(), getChannelCode(), 502);
            default ->
                new ChannelDeliveryException("Unknown gateway failure: " + status.getDiagnosticBlob(), getChannelCode(), 500);
        };
    }
}