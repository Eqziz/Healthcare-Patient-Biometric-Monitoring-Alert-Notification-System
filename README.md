```mermaid
classDiagram
    direction TB

    %% --- Bridge Abstraction Hierarchy ---
    class VitalAlert {
        <>
        #BiometricAlertChannel channel
        +VitalAlert(channel: BiometricAlertChannel)
        +setChannel(channel: BiometricAlertChannel) void
        +getChannel() BiometricAlertChannel
        +dispatch(payload: VitalsPayload, endpoint: String)* DeliveryReport
    }

    class CriticalIcuEmergencyAlert {
        +CriticalIcuEmergencyAlert(channel: BiometricAlertChannel)
        +dispatch(payload: VitalsPayload, endpoint: String) DeliveryReport
    }

    class RoutineTelemetryAuditAlert {
        -int retryAttempts
        +RoutineTelemetryAuditAlert(channel: BiometricAlertChannel, retryAttempts: int)
        +dispatch(payload: VitalsPayload, endpoint: String) DeliveryReport
    }

    VitalAlert <|-- CriticalIcuEmergencyAlert : extends
    VitalAlert <|-- RoutineTelemetryAuditAlert : extends

    %% --- Bridge Implementor Hierarchy ---
    class BiometricAlertChannel {
        <>
        +getChannelCode()* String
        +supportsSeverity(severity: AlertSeverity)* boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String)* DeliveryReport
    }

    class HospitalRestPushChannel {
        +getChannelCode() String
        +supportsSeverity(severity: AlertSeverity) boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String) DeliveryReport
    }

    class IcuPagerRadioChannel {
        +getChannelCode() String
        +supportsSeverity(severity: AlertSeverity) boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String) DeliveryReport
    }

    class LegacyHl7GatewayAdapter {
        -LegacyHl7TcpGateway legacyGateway
        -int defaultTimeoutMs
        -String facilityId
        +LegacyHl7GatewayAdapter(legacyGateway: LegacyHl7TcpGateway, defaultTimeoutMs: int, facilityId: String)
        +getChannelCode() String
        +supportsSeverity(severity: AlertSeverity) boolean
        +transmit(payload: VitalsPayload, destinationEndpoint: String) DeliveryReport
        -convertToHl7Frame(payload: VitalsPayload, targetEndpoint: String) byte[]
        -translateStatusCode(status: Hl7TransmissionStatus) ChannelDeliveryException
    }

    BiometricAlertChannel <|.. HospitalRestPushChannel : implements
    BiometricAlertChannel <|.. IcuPagerRadioChannel : implements
    BiometricAlertChannel <|.. LegacyHl7GatewayAdapter : implements

    %% --- Bridge Association ---
    VitalAlert o--> BiometricAlertChannel : "Bridge (channel)"

    %% --- Incompatible Legacy Adaptee ---
    class LegacyHl7TcpGateway {
        -String ipAddress
        -int port
        +sendRawHl7Frame(timeoutMillis: int, mllpFrame: byte[], facilityId: String) Hl7TransmissionStatus
        +getIpAddress() String
        +getPort() int
    }

    class Hl7TransmissionStatus {
        +STATUS_OK: int\(+ERR_BUFFER_OVERFLOW: int\)
        +ERR_CHECKSUM_MISMATCH: int\(+ERR_ACK_REJECTED: int\)
        -int statusCode
        -String diagnosticBlob
        +getStatusCode() int
        +getDiagnosticBlob() String
        +isSuccess() boolean
    }

    LegacyHl7GatewayAdapter o--> LegacyHl7TcpGateway : "wraps / adapts"
    LegacyHl7TcpGateway ..> Hl7TransmissionStatus : returns

    %% --- Dynamic Selector ---
    class DynamicChannelRegistry {
        -List~BiometricAlertChannel~ channels
        +registerChannel(channel: BiometricAlertChannel) void
        +resolveChannel(severity: AlertSeverity, preferredChannelCode: String) BiometricAlertChannel
    }

    DynamicChannelRegistry o--> BiometricAlertChannel : manages
