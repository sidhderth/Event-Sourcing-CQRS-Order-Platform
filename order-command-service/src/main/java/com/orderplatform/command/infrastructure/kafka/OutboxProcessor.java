package com.orderplatform.command.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.command.infrastructure.persistence.OutboxEntity;
import com.orderplatform.command.infrastructure.persistence.OutboxJpaRepository;
import com.orderplatform.events.avro.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.outbox.processor.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${app.outbox.processor.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.processor.poll-interval:1000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEntity> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        // Limit batch size
        List<OutboxEntity> batch = pendingEvents.stream()
                .limit(batchSize)
                .toList();

        log.debug("Processing {} pending outbox records", batch.size());

        for (OutboxEntity outboxEntity : batch) {
            try {
                // Convert to Avro event
                OrderEvent avroEvent = convertToAvroEvent(outboxEntity);

                // Publish to Kafka with transaction
                kafkaTemplate.executeInTransaction(operations -> {
                    operations.send(orderEventsTopic, 
                            outboxEntity.getAggregateId().toString(), 
                            avroEvent);
                    return true;
                });

                // Mark as published
                outboxEntity.setStatus("PUBLISHED");
                outboxEntity.setPublishedAt(Instant.now());
                outboxRepository.save(outboxEntity);

                log.debug("Published event {} to Kafka", outboxEntity.getId());

            } catch (Exception e) {
                log.error("Failed to publish event {} to Kafka", outboxEntity.getId(), e);
                outboxEntity.setStatus("FAILED");
                outboxRepository.save(outboxEntity);
            }
        }

        log.info("Processed {} outbox records", batch.size());
    }

    private OrderEvent convertToAvroEvent(OutboxEntity outboxEntity) throws Exception {
        Map<String, Object> payload = outboxEntity.getPayload();
        
        // Parse the occurredAt timestamp
        Object occurredAtObj = payload.get("occurredAt");
        Instant occurredAt;
        if (occurredAtObj instanceof Number) {
            occurredAt = Instant.ofEpochMilli(((Number) occurredAtObj).longValue());
        } else if (occurredAtObj instanceof String) {
            occurredAt = Instant.parse(occurredAtObj.toString());
        } else {
            occurredAt = Instant.now();
        }
        
        return OrderEvent.newBuilder()
                .setEventId(payload.get("eventId").toString())
                .setAggregateId(outboxEntity.getAggregateId().toString())
                .setEventType(outboxEntity.getEventType())
                .setVersion((Long) payload.get("version"))
                .setOccurredAt(occurredAt)
                .setActor(payload.get("actor") != null ? payload.get("actor").toString() : null)
                .setTraceId(payload.get("traceId") != null ? payload.get("traceId").toString() : null)
                .setPayload(objectMapper.writeValueAsString(payload))
                .build();
    }
}
