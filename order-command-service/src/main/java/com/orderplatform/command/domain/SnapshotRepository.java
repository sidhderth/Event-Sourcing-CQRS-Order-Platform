package com.orderplatform.command.domain;

import com.orderplatform.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface SnapshotRepository {

    void save(UUID aggregateId, Order order);

    Optional<Order> findByAggregateId(UUID aggregateId);

    void deleteByAggregateId(UUID aggregateId);
}
