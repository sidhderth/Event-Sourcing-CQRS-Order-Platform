package com.orderplatform.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing a monetary amount with currency.
 */
public record Money(BigDecimal amount, String currency) {
    
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        // Normalize to 2 decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Adds another Money value to this one.
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", currency, other.currency));
        }
        return new Money(amount.add(other.amount), currency);
    }
    
    /**
     * Multiplies this Money by an integer factor.
     */
    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
    
    /**
     * Subtracts another Money value from this one.
     * @throws IllegalArgumentException if currencies don't match or result is negative
     */
    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", currency, other.currency));
        }
        BigDecimal result = amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, currency);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", amount.toPlainString(), currency);
    }
}
