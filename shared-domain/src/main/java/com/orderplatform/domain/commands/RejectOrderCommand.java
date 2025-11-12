package com.orderplatform.domain.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to reject an order.
 */
public record RejectOrderCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotNull(message = "Rejected by is required")
    UUID rejectedBy,
    
    @NotBlank(message = "Rejection reason is required")
    String reason
) {}
