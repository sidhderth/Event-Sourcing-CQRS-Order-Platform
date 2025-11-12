package com.orderplatform.domain.events;

import com.orderplatform.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when an order is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements DomainEvent {
    
    private UUID eventId;
    private UUID aggregateId;
    private long version;
    private Instant occurredAt;
    
    private UUID customerId;
    private List<OrderItem> items;
    private String currency;
}
