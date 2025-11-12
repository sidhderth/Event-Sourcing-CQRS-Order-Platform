package com.orderplatform.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an item is removed from an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRemovedEvent implements DomainEvent {
    
    private UUID eventId;
    private UUID aggregateId;
    private long version;
    private Instant occurredAt;
    
    private String sku;
}
