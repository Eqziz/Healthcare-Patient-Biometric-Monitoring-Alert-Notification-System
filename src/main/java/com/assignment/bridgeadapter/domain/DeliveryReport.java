package com.assignment.bridgeadapter.domain;

import java.time.Instant;

public record DeliveryReport(
    String trackingId,
    String channelName,
    Instant deliveredAt,
    boolean acknowledged
) {}