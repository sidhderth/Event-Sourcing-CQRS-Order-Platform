package com.orderplatform.command.application.dto;

import com.orderplatform.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
