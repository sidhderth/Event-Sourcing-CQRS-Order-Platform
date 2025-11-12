package com.orderplatform.domain.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Command to create a new order.
 */
public record CreateOrderCommand(
    
    @NotNull(message = "Customer ID is required")
    UUID customerId,
    
    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 100, message = "Order cannot have more than 100 items")
    List<@Valid OrderItemDto> items,
    
    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    String currency,
    
    @Size(max = 255, message = "Idempotency key cannot exceed 255 characters")
    String idempotencyKey
) {}
