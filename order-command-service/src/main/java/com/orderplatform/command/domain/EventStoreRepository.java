package com.orderplatform.command.domain;

import com.orderplatform.domain.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventStoreRepository {

    void append(DomainEvent event);

    List<DomainEvent> findByAggregateId(UUID aggregateId);

    List<DomainEvent> findAllOrderByOccurredAt(Instant fromTime);

    List<DomainEvent> findByOccurredAtBetween(Instant fromTime, Instant toTime);
}
