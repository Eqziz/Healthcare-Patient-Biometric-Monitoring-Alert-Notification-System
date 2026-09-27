package com.assignment.bridgeadapter;

import com.assignment.bridgeadapter.abstraction.CriticalIcuEmergencyAlert;
import com.assignment.bridgeadapter.abstraction.RoutineTelemetryAuditAlert;
import com.assignment.bridgeadapter.abstraction.VitalAlert;
import com.assignment.bridgeadapter.adapter.LegacyHl7GatewayAdapter;
import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.domain.DeliveryReport;
import com.assignment.bridgeadapter.domain.VitalsPayload;
import com.assignment.bridgeadapter.exception.ChannelDeliveryException;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;
import com.assignment.bridgeadapter.implementor.HospitalRestPushChannel;
import com.assignment.bridgeadapter.implementor.IcuPagerRadioChannel;
import com.assignment.bridgeadapter.legacy.Hl7TransmissionStatus;
import com.assignment.bridgeadapter.legacy.LegacyHl7TcpGateway;
import com.assignment.bridgeadapter.legacy.LegacySocketTimeoutException;
import com.assignment.bridgeadapter.selection.DynamicChannelRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BridgeAdapterComprehensiveTest {

    @Mock
    private BiometricAlertChannel mockImplementor;

    @Mock
    private LegacyHl7TcpGateway mockLegacyGateway;

    private VitalsPayload criticalPayload;
    private VitalsPayload routinePayload;

    @BeforeEach
    void setUp() {
        criticalPayload = new VitalsPayload(
            "PATIENT-99",
            "HEART_RATE_BPM",
            185.0,
            120.0,
            AlertSeverity.CRITICAL,
            Instant.now()
        );

        routinePayload = new VitalsPayload(
            "PATIENT-12",
            "BODY_TEMP_C",
            36.8,
            38.0,
            AlertSeverity.LOW,
            Instant.now()
        );
    }

    @Nested
    @DisplayName("Bridge Abstraction Tests (Mocked Implementor)")
    class BridgeAbstractionTests {

        @Test
        @DisplayName("CriticalIcuEmergencyAlert successfully delegates dispatch to mock implementor")
        void testCriticalEmergencyAlertDelegation() throws ChannelDeliveryException {
            DeliveryReport expectedReport = new DeliveryReport("ID-123", "MOCK", Instant.now(), true);
            when(mockImplementor.transmit(eq(criticalPayload), eq("ICU-WARD-4"))).thenReturn(expectedReport);

            VitalAlert alert = new CriticalIcuEmergencyAlert(mockImplementor);
            DeliveryReport result = alert.dispatch(criticalPayload, "ICU-WARD-4");

            assertNotNull(result);
            assertEquals("ID-123", result.trackingId());
            verify(mockImplementor, times(1)).transmit(criticalPayload, "ICU-WARD-4");
        }

        @Test
        @DisplayName("CriticalIcuEmergencyAlert throws IllegalArgumentException on non-critical payload")
        void testCriticalEmergencyValidation() {
            VitalAlert alert = new CriticalIcuEmergencyAlert(mockImplementor);
            assertThrows(IllegalArgumentException.class, () -> alert.dispatch(routinePayload, "ICU-WARD-4"));
            verifyNoInteractions(mockImplementor);
        }

        @Test
        @DisplayName("RoutineTelemetryAuditAlert succeeds within retry attempts")
        void testRoutineAlertRetriesAndSucceeds() throws ChannelDeliveryException {
            DeliveryReport expectedReport = new DeliveryReport("RETRY-OK", "MOCK", Instant.now(), true);
            when(mockImplementor.transmit(any(), any()))
                .thenThrow(new ChannelDeliveryException("Transient fail", "MOCK", 503))
                .thenReturn(expectedReport);

            VitalAlert alert = new RoutineTelemetryAuditAlert(mockImplementor, 2);
            DeliveryReport report = alert.dispatch(routinePayload, "EP-AUDIT");

            assertEquals("RETRY-OK", report.trackingId());
            verify(mockImplementor, times(2)).transmit(routinePayload, "EP-AUDIT");
        }

        @Test
        @DisplayName("RoutineTelemetryAuditAlert exhausts retries and raises standard domain exception")
        void testRoutineAlertExhaustsRetries() throws ChannelDeliveryException {
            when(mockImplementor.transmit(any(), any()))
                .thenThrow(new ChannelDeliveryException("Permanent fail", "MOCK", 500));
            when(mockImplementor.getChannelCode()).thenReturn("MOCK");

            VitalAlert alert = new RoutineTelemetryAuditAlert(mockImplementor, 3);
            ChannelDeliveryException thrown = assertThrows(
                ChannelDeliveryException.class,
                () -> alert.dispatch(routinePayload, "EP-AUDIT")
            );

            assertEquals(500, thrown.getFailureCode());
            assertTrue(thrown.getMessage().contains("after 3 attempts"));
            verify(mockImplementor, times(3)).transmit(routinePayload, "EP-AUDIT");
        }
    }

    @Nested
    @DisplayName("Adapter Translation & Encapsulation Tests (Mocked Legacy Gateway)")
    class AdapterTranslationTests {

        @Test
        @DisplayName("Adapter correctly formats and translates successful legacy transmission")
        void testSuccessfulLegacyAdapterTransmission() throws ChannelDeliveryException {
            when(mockLegacyGateway.sendRawHl7Frame(eq(5000), any(byte[].class), eq("HOSP-BASE")))
                .thenReturn(new Hl7TransmissionStatus(Hl7TransmissionStatus.STATUS_OK, "ACK_SUCCESS"));

            LegacyHl7GatewayAdapter adapter = new LegacyHl7GatewayAdapter(mockLegacyGateway, 5000, "HOSP-BASE");
            DeliveryReport report = adapter.transmit(criticalPayload, "CENTRAL_MONITOR");

            assertNotNull(report);
            assertEquals("HL7-ACK_SUCCESS", report.trackingId());
            assertEquals("LEGACY_HL7_TCP", report.channelName());
            assertTrue(report.acknowledged());
        }

        @Test
        @DisplayName("Adapter translates LegacySocketTimeoutException into ChannelDeliveryException without leakage")
        void testTimeoutExceptionTranslation() {
            when(mockLegacyGateway.sendRawHl7Frame(anyInt(), any(byte[].class), anyString()))
                .thenThrow(new LegacySocketTimeoutException("Socket timed out after 5000ms"));

            LegacyHl7GatewayAdapter adapter = new LegacyHl7GatewayAdapter(mockLegacyGateway, 5000, "HOSP-BASE");

            ChannelDeliveryException ex = assertThrows(
                ChannelDeliveryException.class,
                () -> adapter.transmit(criticalPayload, "ENDPOINT")
            );

            assertEquals(504, ex.getFailureCode());
            assertEquals("LEGACY_HL7_TCP", ex.getChannelName());
            assertTrue(ex.getCause() instanceof LegacySocketTimeoutException);
        }

        @Test
        @DisplayName("Adapter translates checksum mismatch status code into domain exception")
        void testChecksumMismatchTranslation() {
            when(mockLegacyGateway.sendRawHl7Frame(anyInt(), any(byte[].class), anyString()))
                .thenReturn(new Hl7TransmissionStatus(Hl7TransmissionStatus.ERR_CHECKSUM_MISMATCH, "CSUM_FAIL"));

            LegacyHl7GatewayAdapter adapter = new LegacyHl7GatewayAdapter(mockLegacyGateway, 5000, "HOSP-BASE");

            ChannelDeliveryException ex = assertThrows(
                ChannelDeliveryException.class,
                () -> adapter.transmit(criticalPayload, "ENDPOINT")
            );

            assertEquals(422, ex.getFailureCode());
            assertTrue(ex.getMessage().contains("Data transmission corruption detected"));
        }

        @Test
        @DisplayName("Adapter translates NAK rejection status code into domain exception")
        void testNakRejectionTranslation() {
            when(mockLegacyGateway.sendRawHl7Frame(anyInt(), any(byte[].class), anyString()))
                .thenReturn(new Hl7TransmissionStatus(Hl7TransmissionStatus.ERR_ACK_REJECTED, "NAK_REJECTED"));

            LegacyHl7GatewayAdapter adapter = new LegacyHl7GatewayAdapter(mockLegacyGateway, 5000, "HOSP-BASE");

            ChannelDeliveryException ex = assertThrows(
                ChannelDeliveryException.class,
                () -> adapter.transmit(criticalPayload, "ENDPOINT")
            );

            assertEquals(502, ex.getFailureCode());
            assertTrue(ex.getMessage().contains("Hospital server rejected HL7 message"));
        }
    }

    @Nested
    @DisplayName("Dynamic Implementor Selection & End-to-End Tests")
    class DynamicSelectionAndEndToEndTests {

        @Test
        @DisplayName("DynamicRegistry resolves appropriate implementor at runtime and integrates with Bridge")
        void testDynamicImplementorSelectionWithBridge() throws ChannelDeliveryException {
            HospitalRestPushChannel restChannel = new HospitalRestPushChannel();
            IcuPagerRadioChannel pagerChannel = new IcuPagerRadioChannel();
            LegacyHl7GatewayAdapter adapter = new LegacyHl7GatewayAdapter(new LegacyHl7TcpGateway("10.0.0.1", 2575), 3000, "HOSP-01");

            DynamicChannelRegistry registry = new DynamicChannelRegistry(List.of(restChannel, pagerChannel, adapter));

            // Select channel dynamically based on LOW severity
            BiometricAlertChannel chosenRoutineChannel = registry.resolveChannel(AlertSeverity.LOW, null);
            assertEquals("REST_EMR_PUSH", chosenRoutineChannel.getChannelCode());

            VitalAlert routineAlert = new RoutineTelemetryAuditAlert(chosenRoutineChannel, 1);
            DeliveryReport routineReport = routineAlert.dispatch(routinePayload, "http://emr.internal/vitals");
            assertTrue(routineReport.acknowledged());

            // Select channel dynamically based on explicit preferred channel (the adapted one)
            BiometricAlertChannel chosenLegacyChannel = registry.resolveChannel(AlertSeverity.CRITICAL, "LEGACY_HL7_TCP");
            assertEquals("LEGACY_HL7_TCP", chosenLegacyChannel.getChannelCode());

            VitalAlert emergencyAlert = new CriticalIcuEmergencyAlert(chosenLegacyChannel);
            DeliveryReport emergencyReport = emergencyAlert.dispatch(criticalPayload, "TCP_RECEIVER");
            assertNotNull(emergencyReport.trackingId());
        }
    }
}