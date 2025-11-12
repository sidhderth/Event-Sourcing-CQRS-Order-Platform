package com.orderplatform.domain.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to remove an item from an order.
 */
public record RemoveItemCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotBlank(message = "SKU is required")
    String sku
) {}
