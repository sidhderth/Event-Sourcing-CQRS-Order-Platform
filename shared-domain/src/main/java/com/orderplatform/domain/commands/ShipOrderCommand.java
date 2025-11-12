package com.orderplatform.domain.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to ship an order.
 */
public record ShipOrderCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotBlank(message = "Tracking number is required")
    String trackingNumber,
    
    @NotBlank(message = "Carrier is required")
    String carrier
) {}
