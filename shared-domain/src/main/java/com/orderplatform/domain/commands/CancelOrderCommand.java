package com.orderplatform.domain.commands;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to cancel an order.
 */
public record CancelOrderCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotNull(message = "Canceled by is required")
    UUID canceledBy,
    
    String reason
) {}
