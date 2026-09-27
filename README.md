@startuml
skinparam classAttributeIconSize 0
skinparam monochrome true
skinparam packageStyle rectangle

package "Domain Layer" {
    enum AlertSeverity {
        LOW
        MODERATE
        CRITICAL
    }

    class VitalsPayload <> {
        +patientId: String
        +metricType: String
        +observedValue: double
        +thresholdValue: double
        +severity: AlertSeverity
        +timestamp: Instant
    }

    class DeliveryReport <> {
        +trackingId: String
        +channelName: String
        +deliveredAt: Instant
        +acknowledged: boolean
    }

    class ChannelDeliveryException {
        -channelName: String
        -failureCode: int
        +getChannelName(): String
        +getFailureCode(): int
    }
}

package "Implementor Hierarchy (Bridge Side B)" {
    interface BiometricAlertChannel <> {
        +{abstract} getChannelCode(): String
        +{abstract} supportsSeverity(severity: AlertSeverity): boolean
        +{abstract} transmit(payload: VitalsPayload, destinationEndpoint: String): DeliveryReport
    }

    class HospitalRestPushChannel implements BiometricAlertChannel {
        +getChannelCode(): String
        +supportsSeverity(severity: AlertSeverity): boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String): DeliveryReport
    }

    class IcuPagerRadioChannel implements BiometricAlertChannel {
        +getChannelCode(): String
        +supportsSeverity(severity: AlertSeverity): boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String): DeliveryReport
    }

    class LegacyHl7GatewayAdapter implements BiometricAlertChannel {
        -legacyGateway: LegacyHl7TcpGateway
        -defaultTimeoutMs: int
        -facilityId: String
        +LegacyHl7GatewayAdapter(legacyGateway: LegacyHl7TcpGateway, defaultTimeoutMs: int, facilityId: String)
        +getChannelCode(): String
        +supportsSeverity(severity: AlertSeverity): boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String): DeliveryReport
        -convertToHl7Frame(payload: VitalsPayload, targetEndpoint: String): byte[]
        -translateStatusCode(status: Hl7TransmissionStatus): ChannelDeliveryException
    }
}

package "Third-Party Subsystem (Adaptee)" {
    class LegacyHl7TcpGateway {
        -ipAddress: String
        -port: int
        +sendRawHl7Frame(timeoutMillis: int, mllpFrame: byte[], facilityId: String): Hl7TransmissionStatus
    }

    class Hl7TransmissionStatus {
        +{static} STATUS_OK: int
        +{static} ERR_BUFFER_OVERFLOW: int
        +{static} ERR_CHECKSUM_MISMATCH: int
        +{static} ERR_ACK_REJECTED: int
        -statusCode: int
        -diagnosticBlob: String
        +isSuccess(): boolean
    }

    class LegacySocketTimeoutException <> {
    }

    LegacyHl7GatewayAdapter o--> LegacyHl7TcpGateway : wraps
    LegacyHl7TcpGateway ..> Hl7TransmissionStatus : returns
    LegacyHl7TcpGateway ..> LegacySocketTimeoutException : throws
}

package "Abstraction Hierarchy (Bridge Side A)" {
    abstract class VitalAlert <> {
        #channel: BiometricAlertChannel
        +VitalAlert(channel: BiometricAlertChannel)
        +setChannel(channel: BiometricAlertChannel): void
        +getChannel(): BiometricAlertChannel
        +{abstract} dispatch(payload: VitalsPayload, endpoint: String): DeliveryReport
    }

    class CriticalIcuEmergencyAlert extends VitalAlert {
        +CriticalIcuEmergencyAlert(channel: BiometricAlertChannel)
        +dispatch(payload: VitalsPayload, endpoint: String): DeliveryReport
    }

    class RoutineTelemetryAuditAlert extends VitalAlert {
        -retryAttempts: int
        +RoutineTelemetryAuditAlert(channel: BiometricAlertChannel, retryAttempts: int)
        +dispatch(payload: VitalsPayload, endpoint: String): DeliveryReport
    }

    VitalAlert o--> BiometricAlertChannel : bridge reference
}

package "Dynamic Implementor Selection" {
    class DynamicChannelRegistry {
        -channels: List
        +registerChannel(channel: BiometricAlertChannel): void
        +resolveChannel(severity: AlertSeverity, preferredChannelCode: String): BiometricAlertChannel
    }

    DynamicChannelRegistry o--> BiometricAlertChannel
}

@enduml
