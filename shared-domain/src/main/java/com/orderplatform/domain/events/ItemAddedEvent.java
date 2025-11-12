package com.orderplatform.domain.events;

import com.orderplatform.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an item is added to an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAddedEvent implements DomainEvent {
    
    private UUID eventId;
    private UUID aggregateId;
    private long version;
    private Instant occurredAt;
    
    private OrderItem item;
}
