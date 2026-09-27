package com.assignment.bridgeadapter.legacy;

public final class Hl7TransmissionStatus {
    public static final int STATUS_OK = 0x00;
    public static final int ERR_BUFFER_OVERFLOW = 0x01;
    public static final int ERR_CHECKSUM_MISMATCH = 0x02;
    public static final int ERR_ACK_REJECTED = 0x04;

    private final int statusCode;
    private final String diagnosticBlob;

    public Hl7TransmissionStatus(int statusCode, String diagnosticBlob) {
        this.statusCode = statusCode;
        this.diagnosticBlob = diagnosticBlob;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDiagnosticBlob() {
        return diagnosticBlob;
    }

    public boolean isSuccess() {
        return statusCode == STATUS_OK;
    }
}