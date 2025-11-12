package com.orderplatform.command.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.command.domain.SnapshotRepository;
import com.orderplatform.command.infrastructure.persistence.SnapshotEntity;
import com.orderplatform.command.infrastructure.persistence.SnapshotJpaRepository;
import com.orderplatform.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SnapshotRepositoryImpl implements SnapshotRepository {

    private final SnapshotJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(UUID aggregateId, Order order) {
        Map<String, Object> state = objectMapper.convertValue(order, Map.class);
        
        SnapshotEntity entity = SnapshotEntity.builder()
                .aggregateId(aggregateId)
                .aggregateType("Order")
                .version(order.getVersion())
                .state(state)
                .createdAt(Instant.now())
                .build();

        jpaRepository.save(entity);
        log.debug("Saved snapshot for aggregate {} at version {}", aggregateId, order.getVersion());
    }

    @Override
    public Optional<Order> findByAggregateId(UUID aggregateId) {
        return jpaRepository.findById(aggregateId)
                .map(entity -> {
                    Order order = objectMapper.convertValue(entity.getState(), Order.class);
                    log.debug("Loaded snapshot for aggregate {} at version {}", aggregateId, order.getVersion());
                    return order;
                });
    }

    @Override
    public void deleteByAggregateId(UUID aggregateId) {
        jpaRepository.deleteById(aggregateId);
        log.debug("Deleted snapshot for aggregate {}", aggregateId);
    }
}
