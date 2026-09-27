# Architectural Design Rationale: Biometric Telemetry & Alert Notification System

## 1\. Chosen Problem Domain

The system models a clinical healthcare infrastructure responsible for dispatching real-time patient biometric alerts (e.g., ventricular tachycardia, oxygen desaturation, routine temperature drift). The architecture decouples the alert classification logic (how alerts are prioritized, validated, and retried) from the communication infrastructure (how data is formatted, routed, and delivered across hospital networks).

---

## 2\. Why Bridge Alone or Adapter Alone Is Insufficient

### Why Bridge Alone Fails:

A standard Bridge pattern requires every concrete implementor to directly adhere to the BiometricAlertChannel interface contract. In enterprise hospital environments, core subsystems depend on legacy telecommunications standards (e.g., HL7 v2 over raw MLLP/TCP sockets) provided as closed-source binary vendor drivers (LegacyHl7TcpGateway). Bridge assumes you control or can write implementations conforming directly to the Implementor interface. Without an Adapter, the third-party driver cannot be placed into the Implementor hierarchy without modifying vendor code (violating the Open/Closed Principle and commercial distribution licenses).

### Why Adapter Alone Fails:

An Adapter pattern only solves point-to-point interface mismatch between a single caller and an incompatible callee. If only Adapter were used without Bridge, every alert type (CriticalIcuEmergencyAlert, RoutineTelemetryAuditAlert) would need direct, tightly coupled bindings to every target communication protocol. Adding M alert types across N communication protocols would lead to a combinatorial explosion of M \* N subclasses (e.g., CriticalIcuRestAlert, CriticalIcuRadioAlert, CriticalIcuLegacyHl7Alert, RoutineTelemetryRestAlert, etc.). Bridge separates the abstraction axis from the implementor axis so both vary independently in M \+ N classes.

---

## 3\. Genuine Incompatibility of the Adapted Class

The LegacyHl7TcpGateway class satisfies the incompatibility threshold across multiple orthogonal dimensions:

1. Method Signature & Parameter Inversion: Rather than accepting domain entities (VitalsPayload, destinationEndpoint), sendRawHl7Frame(int timeoutMillis, byte\[\] mllpFrame, String facilityId) expects the timeout as the first argument, an ASCII-encoded byte buffer as the second, and a facility identifier as the third.  
2. Data Structure Representation: The native implementors consume structured object records (VitalsPayload). The legacy gateway strictly consumes unmarshalled raw MLLP byte frames.  
3. Failure Mechanism & Protocol:  
   - The native bridge contract enforces checked ChannelDeliveryException instances carrying structured channel codes and HTTP/protocol status integers.  
   - The legacy class returns a bitwise status object (Hl7TransmissionStatus) with bitmask error flags (0x01, 0x02, 0x04) and diagnostic strings.  
   - For network dropouts, the legacy class throws an unchecked vendor exception (LegacySocketTimeoutException).  
4. Leak Prevention: LegacyHl7GatewayAdapter completely intercepts LegacySocketTimeoutException and translates bitmask statuses into meaningful ChannelDeliveryException domain instances, preventing vendor dependencies from leaking into the VitalAlert abstraction.

---

## 4\. Required Complexity Module: Dynamic Implementor Selection

We selected Dynamic Implementor Selection (Section 5). The concrete channel used by a VitalAlert instance is resolved at runtime via DynamicChannelRegistry. Resolution is determined dynamically based on the alert's AlertSeverity and client-provided protocol preferences. The client code never hardcodes concrete channel instantiations when dispatching vital metrics, keeping client workflows completely decoupled from network topologies.

---

## 5\. Open/Closed Principle (OCP) Satisfaction

- New Abstraction Variant: Introducing a new alert type (e.g., SurgicalWardEscalationAlert) requires creating a new subclass extending VitalAlert. Zero lines of code in existing implementors or adapters are modified.  
- New Implementor Variant: Introducing a modern messaging backend (e.g., KafkaTelemetryChannel) requires creating a single class implementing BiometricAlertChannel and registering it in DynamicChannelRegistry. No existing abstraction classes are altered.

---

## 6\. One Key Limitation of the Final Design

Lack of Two-Way Asynchronous Acknowledgement Flow: While the Bridge and Adapter cleanly abstract outbound dispatching and error translation, the synchronous request-response contract of BiometricAlertChannel.transmit() imposes a blocking model. If a network channel experiences delayed ACKs, the abstraction thread is held until timeout resolution occurs. Transitioning to a non-blocking reactive stream (e.g., CompletableFuture or Flow API) would require introducing an asynchronous polling callback loop into the legacy MLLP adapter.