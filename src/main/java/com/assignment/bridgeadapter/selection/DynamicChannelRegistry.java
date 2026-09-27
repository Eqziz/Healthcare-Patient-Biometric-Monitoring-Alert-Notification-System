package com.assignment.bridgeadapter.selection;

import com.assignment.bridgeadapter.domain.AlertSeverity;
import com.assignment.bridgeadapter.implementor.BiometricAlertChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Dynamic Implementor Selection: Resolves the appropriate implementation at runtime
 * based on payload requirements, avoiding hardcoding in the client or Abstraction.
 */
public class DynamicChannelRegistry {

    private final List channels = new ArrayList<>();

    public DynamicChannelRegistry(List initialChannels) {
        if (initialChannels != null) {
            this.channels.addAll(initialChannels);
        }
    }

    public void registerChannel(BiometricAlertChannel channel) {
        this.channels.add(channel);
    }

    public BiometricAlertChannel resolveChannel(AlertSeverity severity, String preferredChannelCode) {
        if (preferredChannelCode != null && !preferredChannelCode.isBlank()) {
            Optional match = channels.stream()
                .filter(c -> c.getChannelCode().equalsIgnoreCase(preferredChannelCode))
                .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }

        return channels.stream()
            .filter(c -> c.supportsSeverity(severity))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No available delivery channel configured for severity: " + severity));
    }

    public List getRegisteredChannels() {
        return Collections.unmodifiableList(channels);
    }
}