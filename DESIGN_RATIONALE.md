# Architectural Design Rationale: Biometric Telemetry & Alert Notification System

## 1. Chosen Problem Domain
The system models a clinical healthcare infrastructure responsible for dispatching real-time patient biometric alerts (e.g., ventricular tachycardia, oxygen desaturation, routine temperature drift). The architecture decouples the alert classification logic (how alerts are prioritized, validated, and retried) from the communication infrastructure (how data is formatted, routed, and delivered across hospital networks).
