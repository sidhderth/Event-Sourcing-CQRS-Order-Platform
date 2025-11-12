package com.orderplatform.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an order is approved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderApprovedEvent implements DomainEvent {
    
    private UUID eventId;
    private UUID aggregateId;
    private long version;
    private Instant occurredAt;
    
    private UUID approvedBy;
    private String reason;
}
