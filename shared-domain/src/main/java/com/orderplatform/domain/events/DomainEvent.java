package com.orderplatform.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event.
     */
    UUID getEventId();
    
    /**
     * Identifier of the aggregate this event belongs to.
     */
    UUID getAggregateId();
    
    /**
     * Version of the aggregate after this event is applied.
     */
    long getVersion();
    
    /**
     * Timestamp when this event occurred.
     */
    Instant getOccurredAt();
    
    /**
     * Type name of this event.
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
