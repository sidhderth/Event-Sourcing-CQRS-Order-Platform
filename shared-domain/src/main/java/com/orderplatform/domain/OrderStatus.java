package com.orderplatform.domain;

/**
 * Represents the lifecycle status of an order.
 */
public enum OrderStatus {
    CREATED,
    APPROVED,
    REJECTED,
    CANCELED,
    SHIPPED
}
