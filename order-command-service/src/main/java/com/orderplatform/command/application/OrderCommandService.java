package com.orderplatform.command.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.command.application.dto.OrderResponse;
import com.orderplatform.command.domain.AggregateLoader;
import com.orderplatform.command.infrastructure.persistence.*;
import com.orderplatform.domain.Order;
import com.orderplatform.domain.commands.*;
import com.orderplatform.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final AggregateLoader aggregateLoader;
    private final CommandDeduplicationJpaRepository deduplicationRepository;
    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderCommand command) {
        log.info("Processing CreateOrder command for customer {}", command.customerId());

        // Check for idempotency
        if (command.idempotencyKey() != null) {
            Optional<CommandDeduplicationEntity> existing = 
                    deduplicationRepository.findById(command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate command detected with idempotency key: {}", command.idempotencyKey());
                return objectMapper.convertValue(existing.get().getResponse(), OrderResponse.class);
            }
        }

        // Create new order aggregate
        Order order = new Order();
        UUID orderId = UUID.randomUUID();
        
        // Convert command items to domain OrderItems
        List<com.orderplatform.domain.OrderItem> orderItems = command.items().stream()
                .map(item -> new com.orderplatform.domain.OrderItem(
                        item.sku(),
                        item.productName(),
                        item.quantity(),
                        new com.orderplatform.domain.Money(item.unitPrice(), command.currency()),
                        new com.orderplatform.domain.Money(
                                item.unitPrice().multiply(java.math.BigDecimal.valueOf(item.quantity())),
                                command.currency()
                        )
                ))
                .toList();
        
        DomainEvent event = order.create(orderId, command.customerId(), orderItems, command.currency());

        // Save event and snapshot
        aggregateLoader.saveEventAndSnapshot(event, order);

        // Insert into outbox for Kafka publishing
        insertIntoOutbox(event);

        // Create response
        OrderResponse response = new OrderResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );

        // Save deduplication record
        if (command.idempotencyKey() != null) {
            saveDeduplicationRecord(command.idempotencyKey(), order.getOrderId(), 
                    "CreateOrder", response);
        }

        log.info("Created order {} with version {}", order.getOrderId(), order.getVersion());
        return response;
    }

    @Transactional
    public OrderResponse approveOrder(ApproveOrderCommand command) {
        log.info("Processing ApproveOrder command for order {}", command.orderId());

        // Load aggregate
        Order order = loadAggregateOrThrow(command.orderId());

        // Execute business logic
        DomainEvent event = order.approve(command.approvedBy(), command.reason());

        // Save event and snapshot
        aggregateLoader.saveEventAndSnapshot(event, order);

        // Insert into outbox
        insertIntoOutbox(event);

        log.info("Approved order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(RejectOrderCommand command) {
        log.info("Processing RejectOrder command for order {}", command.orderId());

        Order order = loadAggregateOrThrow(command.orderId());
        DomainEvent event = order.reject(command.rejectedBy(), command.reason());

        aggregateLoader.saveEventAndSnapshot(event, order);
        insertIntoOutbox(event);

        log.info("Rejected order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(CancelOrderCommand command) {
        log.info("Processing CancelOrder command for order {}", command.orderId());

        Order order = loadAggregateOrThrow(command.orderId());
        DomainEvent event = order.cancel(command.canceledBy(), command.reason());

        aggregateLoader.saveEventAndSnapshot(event, order);
        insertIntoOutbox(event);

        log.info("Canceled order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse shipOrder(ShipOrderCommand command) {
        log.info("Processing ShipOrder command for order {}", command.orderId());

        Order order = loadAggregateOrThrow(command.orderId());
        DomainEvent event = order.ship(command.trackingNumber(), command.carrier());

        aggregateLoader.saveEventAndSnapshot(event, order);
        insertIntoOutbox(event);

        log.info("Shipped order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse addItem(AddItemCommand command) {
        log.info("Processing AddItem command for order {}", command.orderId());

        Order order = loadAggregateOrThrow(command.orderId());
        
        // Convert command to OrderItem
        com.orderplatform.domain.OrderItem orderItem = new com.orderplatform.domain.OrderItem(
                command.sku(),
                command.productName(),
                command.quantity(),
                new com.orderplatform.domain.Money(command.unitPrice(), order.getCurrency()),
                new com.orderplatform.domain.Money(
                        command.unitPrice().multiply(java.math.BigDecimal.valueOf(command.quantity())),
                        order.getCurrency()
                )
        );
        
        DomainEvent event = order.addItem(orderItem);

        aggregateLoader.saveEventAndSnapshot(event, order);
        insertIntoOutbox(event);

        log.info("Added item to order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse removeItem(RemoveItemCommand command) {
        log.info("Processing RemoveItem command for order {}", command.orderId());

        Order order = loadAggregateOrThrow(command.orderId());
        DomainEvent event = order.removeItem(command.sku());

        aggregateLoader.saveEventAndSnapshot(event, order);
        insertIntoOutbox(event);

        log.info("Removed item from order {} with version {}", order.getOrderId(), order.getVersion());
        return toResponse(order);
    }

    private Order loadAggregateOrThrow(UUID orderId) {
        return aggregateLoader.loadAggregate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private void insertIntoOutbox(DomainEvent event) {
        OutboxEntity outboxEntity = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(objectMapper.convertValue(event, Map.class))
                .createdAt(Instant.now())
                .status("PENDING")
                .build();

        outboxRepository.save(outboxEntity);
        log.debug("Inserted event {} into outbox", event.getEventId());
    }

    private void saveDeduplicationRecord(String idempotencyKey, UUID aggregateId, 
                                        String commandType, OrderResponse response) {
        CommandDeduplicationEntity entity = CommandDeduplicationEntity.builder()
                .idempotencyKey(idempotencyKey)
                .aggregateId(aggregateId)
                .commandType(commandType)
                .response(objectMapper.convertValue(response, Map.class))
                .processedAt(Instant.now())
                .build();

        deduplicationRepository.save(entity);
        log.debug("Saved deduplication record for key: {}", idempotencyKey);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
