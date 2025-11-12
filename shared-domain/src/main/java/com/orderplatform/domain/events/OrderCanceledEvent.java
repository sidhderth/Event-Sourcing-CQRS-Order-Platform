package com.orderplatform.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an order is canceled.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCanceledEvent implements DomainEvent {
    
    private UUID eventId;
    private UUID aggregateId;
    private long version;
    private Instant occurredAt;
    
    private UUID canceledBy;
    private String reason;
}
