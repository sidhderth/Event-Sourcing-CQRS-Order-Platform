package com.orderplatform.query.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.events.avro.*;
import com.orderplatform.query.infrastructure.elasticsearch.ElasticsearchSink;
import com.orderplatform.query.readmodel.OrderItemReadModel;
import com.orderplatform.query.readmodel.OrderReadModel;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ElasticsearchSink elasticsearchSink;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${maintenance.mode.enabled}")
    private boolean maintenanceMode;

    @Bean
    public KStream<String, OrderEvent> processOrderEvents(StreamsBuilder streamsBuilder) {
        if (maintenanceMode) {
            log.warn("Maintenance mode is enabled. Kafka Streams topology will not process events.");
            return null;
        }

        // Configure Avro Serde
        SpecificAvroSerde<OrderEvent> orderEventSerde = new SpecificAvroSerde<>();
        orderEventSerde.configure(
            Collections.singletonMap(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                schemaRegistryUrl
            ),
            false
        );

        // Create stream from order-events topic
        KStream<String, OrderEvent> eventStream = streamsBuilder.stream(
            orderEventsTopic,
            Consumed.with(Serdes.String(), orderEventSerde)
        );

        // Group by aggregate ID and aggregate events into OrderReadModel
        KTable<String, OrderReadModel> orderTable = eventStream
            .groupByKey(Grouped.with(Serdes.String(), orderEventSerde))
            .aggregate(
                OrderReadModel::new,
                this::aggregateEvent,
                Materialized.as("order-state-store")
            );

        // Convert KTable to KStream for downstream processing
        KStream<String, OrderReadModel> orderStream = orderTable.toStream();

        // Index orders to Elasticsearch
        orderStream.foreach((key, order) -> {
            log.debug("Processed order: {} with status: {}", order.getOrderId(), order.getStatus());
            elasticsearchSink.indexOrder(order);
        });

        return eventStream;
    }

    private OrderReadModel aggregateEvent(String key, OrderEvent event, OrderReadModel aggregate) {
        try {
            log.debug("Aggregating event: {} for order: {}", event.getEventType(), event.getAggregateId());

            switch (event.getEventType()) {
                case "OrderCreated":
                    return handleOrderCreated(event, aggregate);
                case "OrderApproved":
                    return handleOrderApproved(event, aggregate);
                case "OrderRejected":
                    return handleOrderRejected(event, aggregate);
                case "OrderCanceled":
                    return handleOrderCanceled(event, aggregate);
                case "OrderShipped":
                    return handleOrderShipped(event, aggregate);
                case "ItemAdded":
                    return handleItemAdded(event, aggregate);
                case "ItemRemoved":
                    return handleItemRemoved(event, aggregate);
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
                    return aggregate;
            }
        } catch (Exception e) {
            log.error("Error aggregating event: {}", event.getEventType(), e);
            return aggregate;
        }
    }

    private OrderReadModel handleOrderCreated(OrderEvent event, OrderReadModel aggregate) throws Exception {
        OrderCreatedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            OrderCreatedPayload.class
        );

        aggregate.setOrderId(payload.getOrderId().toString());
        aggregate.setCustomerId(payload.getCustomerId().toString());
        aggregate.setStatus("CREATED");
        aggregate.setCurrency(payload.getCurrency().toString());
        
        // Handle timestamp conversion - Avro returns Long for timestamp-millis
        Object createdAtObj = payload.getCreatedAt();
        aggregate.setCreatedAt(createdAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) createdAtObj) : (Instant) createdAtObj);
            
        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        List<OrderItemReadModel> items = new ArrayList<>();
        for (OrderItemAvro itemAvro : payload.getItems()) {
            OrderItemReadModel item = OrderItemReadModel.builder()
                .sku(itemAvro.getSku().toString())
                .productName(itemAvro.getProductName().toString())
                .quantity(itemAvro.getQuantity())
                .unitPrice(new BigDecimal(itemAvro.getUnitPrice().toString()))
                .lineTotal(new BigDecimal(itemAvro.getLineTotal().toString()))
                .build();
            items.add(item);
        }
        aggregate.setItems(items);
        aggregate.setTotalAmount(new BigDecimal(payload.getTotalAmount().toString()));

        return aggregate;
    }

    private OrderReadModel handleOrderApproved(OrderEvent event, OrderReadModel aggregate) throws Exception {
        OrderApprovedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            OrderApprovedPayload.class
        );

        aggregate.setStatus("APPROVED");
        aggregate.setApprovedBy(payload.getApprovedBy().toString());
        
        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }

    private OrderReadModel handleOrderRejected(OrderEvent event, OrderReadModel aggregate) throws Exception {
        OrderRejectedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            OrderRejectedPayload.class
        );

        aggregate.setStatus("REJECTED");
        aggregate.setRejectionReason(payload.getReason() != null ? payload.getReason().toString() : null);
        
        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }

    private OrderReadModel handleOrderCanceled(OrderEvent event, OrderReadModel aggregate) throws Exception {
        OrderCanceledPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            OrderCanceledPayload.class
        );

        aggregate.setStatus("CANCELED");
        
        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }

    private OrderReadModel handleOrderShipped(OrderEvent event, OrderReadModel aggregate) throws Exception {
        OrderShippedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            OrderShippedPayload.class
        );

        aggregate.setStatus("SHIPPED");
        aggregate.setTrackingNumber(payload.getTrackingNumber().toString());
        aggregate.setCarrier(payload.getCarrier().toString());
        
        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }

    private OrderReadModel handleItemAdded(OrderEvent event, OrderReadModel aggregate) throws Exception {
        ItemAddedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            ItemAddedPayload.class
        );

        OrderItemAvro itemAvro = payload.getItem();
        OrderItemReadModel newItem = OrderItemReadModel.builder()
            .sku(itemAvro.getSku().toString())
            .productName(itemAvro.getProductName().toString())
            .quantity(itemAvro.getQuantity())
            .unitPrice(new BigDecimal(itemAvro.getUnitPrice().toString()))
            .lineTotal(new BigDecimal(itemAvro.getLineTotal().toString()))
            .build();

        List<OrderItemReadModel> items = new ArrayList<>(aggregate.getItems());
        items.add(newItem);
        aggregate.setItems(items);

        // Recalculate total amount
        BigDecimal totalAmount = items.stream()
            .map(OrderItemReadModel::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        aggregate.setTotalAmount(totalAmount);

        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }

    private OrderReadModel handleItemRemoved(OrderEvent event, OrderReadModel aggregate) throws Exception {
        ItemRemovedPayload payload = objectMapper.readValue(
            event.getPayload().toString(),
            ItemRemovedPayload.class
        );

        String skuToRemove = payload.getSku().toString();
        List<OrderItemReadModel> items = aggregate.getItems().stream()
            .filter(item -> !item.getSku().equals(skuToRemove))
            .collect(Collectors.toList());
        aggregate.setItems(items);

        // Recalculate total amount
        BigDecimal totalAmount = items.stream()
            .map(OrderItemReadModel::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        aggregate.setTotalAmount(totalAmount);

        Object occurredAtObj = event.getOccurredAt();
        aggregate.setUpdatedAt(occurredAtObj instanceof Long ? 
            Instant.ofEpochMilli((Long) occurredAtObj) : (Instant) occurredAtObj);
            
        aggregate.setVersion(event.getVersion());

        return aggregate;
    }
}
