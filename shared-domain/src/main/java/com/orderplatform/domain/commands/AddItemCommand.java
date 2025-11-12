package com.orderplatform.domain.commands;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to add an item to an order.
 */
public record AddItemCommand(
    
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotBlank(message = "SKU is required")
    String sku,
    
    @NotBlank(message = "Product name is required")
    String productName,
    
    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity,
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
    BigDecimal unitPrice
) {}
