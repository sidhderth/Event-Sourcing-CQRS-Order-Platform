package com.orderplatform.domain;

/**
 * Value object representing an item in an order.
 */
public record OrderItem(
    String sku,
    String productName,
    int quantity,
    Money unitPrice,
    Money lineTotal
) {
    
    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
        if (lineTotal == null) {
            throw new IllegalArgumentException("Line total cannot be null");
        }
        
        // Validate line total calculation
        Money expectedTotal = unitPrice.multiply(quantity);
        if (!lineTotal.equals(expectedTotal)) {
            throw new IllegalArgumentException(
                String.format("Line total mismatch: expected %s but got %s", 
                    expectedTotal, lineTotal));
        }
    }
}
