package com.orderplatform.command.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.command.domain.EventStoreRepository;
import com.orderplatform.command.infrastructure.persistence.EventEntity;
import com.orderplatform.command.infrastructure.persistence.EventJpaRepository;
import com.orderplatform.domain.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventStoreRepositoryImpl implements EventStoreRepository {

    private final EventJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void append(DomainEvent event) {
        EventEntity entity = EventEntity.builder()
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .version(event.getVersion())
                .payload(objectMapper.convertValue(event, Map.class))
                .metadata(Map.of())
                .occurredAt(event.getOccurredAt())
                .traceId(null) // TODO: Extract from MDC or OpenTelemetry context
                .actor(null) // TODO: Extract from security context
                .build();

        jpaRepository.save(entity);
        log.debug("Appended event {} for aggregate {} at version {}", 
                event.getEventType(), event.getAggregateId(), event.getVersion());
    }

    @Override
    public List<DomainEvent> findByAggregateId(UUID aggregateId) {
        List<EventEntity> entities = jpaRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        return entities.stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> findAllOrderByOccurredAt(Instant fromTime) {
        List<EventEntity> entities = jpaRepository.findAllOrderByOccurredAt(fromTime);
        return entities.stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> findByOccurredAtBetween(Instant fromTime, Instant toTime) {
        List<EventEntity> entities = jpaRepository.findByOccurredAtBetween(fromTime, toTime);
        return entities.stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    private DomainEvent toDomainEvent(EventEntity entity) {
        return switch (entity.getEventType()) {
            case "OrderCreated" -> objectMapper.convertValue(entity.getPayload(), OrderCreatedEvent.class);
            case "OrderApproved" -> objectMapper.convertValue(entity.getPayload(), OrderApprovedEvent.class);
            case "OrderRejected" -> objectMapper.convertValue(entity.getPayload(), OrderRejectedEvent.class);
            case "OrderCanceled" -> objectMapper.convertValue(entity.getPayload(), OrderCanceledEvent.class);
            case "OrderShipped" -> objectMapper.convertValue(entity.getPayload(), OrderShippedEvent.class);
            case "ItemAdded" -> objectMapper.convertValue(entity.getPayload(), ItemAddedEvent.class);
            case "ItemRemoved" -> objectMapper.convertValue(entity.getPayload(), ItemRemovedEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + entity.getEventType());
        };
    }
}
