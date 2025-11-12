package com.orderplatform.domain.commands;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to approve an order.
 */
public record ApproveOrderCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotNull(message = "Approved by is required")
    UUID approvedBy,
    
    String reason
) {}
