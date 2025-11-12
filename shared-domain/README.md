# Shared Domain Module

This module contains shared domain models, value objects, commands, events, and Avro schemas used across all services in the Event-Sourced Order Platform.

## Contents

### Domain Models
- **Order**: Aggregate root that enforces business invariants
- **OrderStatus**: Enum representing order lifecycle states
- **Money**: Value object for monetary amounts with currency
- **OrderItem**: Value object representing an item in an order

### Commands
All command DTOs with validation annotations:
- `CreateOrderCommand`
- `ApproveOrderCommand`
- `RejectOrderCommand`
- `CancelOrderCommand`
- `ShipOrderCommand`
- `AddItemCommand`
- `RemoveItemCommand`

### Events
Domain events implementing the `DomainEvent` interface:
- `OrderCreatedEvent`
- `OrderApprovedEvent`
- `OrderRejectedEvent`
- `OrderCanceledEvent`
- `OrderShippedEvent`
- `ItemAddedEvent`
- `ItemRemovedEvent`

### Avro Schemas

Located in `src/main/avro/`:

- **OrderEvent.avsc**: Envelope schema for all events with metadata
- **OrderItemAvro.avsc**: Schema for order items
- **OrderCreatedPayload.avsc**: Payload for order creation
- **OrderApprovedPayload.avsc**: Payload for order approval
- **OrderRejectedPayload.avsc**: Payload for order rejection
- **OrderCanceledPayload.avsc**: Payload for order cancellation
- **OrderShippedPayload.avsc**: Payload for order shipment
- **ItemAddedPayload.avsc**: Payload for item addition
- **ItemRemovedPayload.avsc**: Payload for item removal

## Schema Registry Integration

### Configuration

The Avro schemas are compiled during the Maven build process using the `avro-maven-plugin`. Generated Java classes are placed in `target/generated-sources/avro/`.

### Kafka Producer Configuration

```properties
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.properties.schema.registry.url=http://schema-registry:8081
```

### Kafka Consumer Configuration

```properties
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer
spring.kafka.properties.schema.registry.url=http://schema-registry:8081
spring.kafka.properties.specific.avro.reader=true
```

### Schema Evolution Strategy

- **Backward Compatibility**: New schemas can read data written by old schemas
- **Forward Compatibility**: Old schemas can read data written by new schemas
- Use optional fields (with defaults) for new fields
- Never remove required fields
- Never change field types

### Example Usage

```java
// Creating an Avro event
OrderEvent avroEvent = OrderEvent.newBuilder()
    .setEventId(event.getEventId().toString())
    .setAggregateId(event.getAggregateId().toString())
    .setEventType("OrderCreated")
    .setVersion(event.getVersion())
    .setOccurredAt(event.getOccurredAt().toEpochMilli())
    .setActor(null)
    .setTraceId(traceId)
    .setPayload(objectMapper.writeValueAsString(payload))
    .build();

// Publishing to Kafka
kafkaTemplate.send("order-events", orderId.toString(), avroEvent);
```

## Building

```bash
mvn clean install
```

This will:
1. Compile Avro schemas to Java classes
2. Compile Java source code
3. Run tests
4. Package as JAR

## Testing

Run unit tests:
```bash
mvn test
```

The module includes tests for:
- Value object validation (Money, OrderItem)
- Aggregate business logic (Order state transitions)
- Command validation
