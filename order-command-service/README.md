# Order Command Service

The Order Command Service is responsible for handling all write operations (commands) on orders in the Event-Sourced Order Platform. It implements event sourcing, CQRS, and the transactional outbox pattern to ensure exactly-once semantics.

## Architecture

### Key Components

1. **REST Controllers** (`api/`)
   - `OrderCommandController`: Exposes REST endpoints for all order commands
   - Validates input using Jakarta Bean Validation
   - Returns RFC 7807 Problem Details for errors

2. **Application Services** (`application/`)
   - `OrderCommandService`: Orchestrates command processing
   - Implements idempotency checking
   - Manages transactional boundaries
   - Coordinates event persistence and outbox insertion

3. **Domain Layer** (`domain/`)
   - `AggregateLoader`: Loads aggregates from snapshots and events
   - `EventStoreRepository`: Interface for event persistence
   - `SnapshotRepository`: Interface for snapshot management

4. **Infrastructure Layer** (`infrastructure/`)
   - `EventStoreRepositoryImpl`: JPA-based event store implementation
   - `SnapshotRepositoryImpl`: JPA-based snapshot implementation
   - `OutboxProcessor`: Scheduled processor for publishing events to Kafka
   - `KafkaConfig`: Kafka producer configuration with exactly-once semantics

5. **Persistence Entities** (`infrastructure/persistence/`)
   - `EventEntity`: JPA entity for events table
   - `SnapshotEntity`: JPA entity for snapshots table
   - `OutboxEntity`: JPA entity for outbox table
   - `CommandDeduplicationEntity`: JPA entity for idempotency tracking

6. **Configuration** (`config/`)
   - `SecurityConfig`: OAuth2 resource server configuration
   - `UserContextHolder`: Utility for extracting user context from JWT

## Features Implemented

### Subtask 2.1: Spring Boot Application Setup
- Created Spring Boot application with all required dependencies
- Configured `application.yml` with PostgreSQL, Kafka, and JPA settings
- Set up structured JSON logging with Logstash encoder
- Configured OpenTelemetry integration (agent-based)

### Subtask 2.2: Database Schema
- Created Flyway migrations for all tables:
  - `V1__create_events_table.sql`: Event store with optimistic locking
  - `V2__create_snapshots_table.sql`: Aggregate snapshots
  - `V3__create_command_deduplication_table.sql`: Idempotency tracking
  - `V4__create_outbox_table.sql`: Transactional outbox pattern

### Subtask 2.3: Event Store Repository
- Implemented `EventStoreRepository` with methods:
  - `append(DomainEvent)`: Persists events with optimistic locking
  - `findByAggregateId(UUID)`: Loads all events for an aggregate
  - `findAllOrderByOccurredAt(Instant)`: Supports event replay
  - `findByOccurredAtBetween(Instant, Instant)`: Time-range queries
- Implemented `SnapshotRepository` for snapshot management

### Subtask 2.4: Aggregate Loading
- Created `AggregateLoader` that:
  - Loads snapshots for optimization
  - Replays events to reconstruct aggregate state
  - Creates snapshots every 50 events
  - Supports snapshot rebuilding for maintenance

### Subtask 2.5: Command Handlers
- Implemented `OrderCommandService` with handlers for:
  - `createOrder`: Creates new orders with idempotency support
  - `approveOrder`: Approves orders
  - `rejectOrder`: Rejects orders
  - `cancelOrder`: Cancels orders
  - `shipOrder`: Ships orders
  - `addItem`: Adds items to orders
  - `removeItem`: Removes items from orders
- All handlers use transactional outbox pattern

### Subtask 2.6: Kafka Producer
- Implemented `OutboxProcessor` that:
  - Polls outbox table for PENDING events
  - Publishes to Kafka with transactional producer
  - Marks events as PUBLISHED
  - Handles failures gracefully
- Configured Kafka producer with exactly-once semantics:
  - `enable.idempotence=true`
  - `acks=all`
  - `transactional.id` configured

### Subtask 2.7: REST Controllers
- Created `OrderCommandController` with endpoints:
  - `POST /api/v1/orders`: Create order
  - `POST /api/v1/orders/{id}/approve`: Approve order
  - `POST /api/v1/orders/{id}/reject`: Reject order
  - `POST /api/v1/orders/{id}/cancel`: Cancel order
  - `POST /api/v1/orders/{id}/ship`: Ship order
  - `POST /api/v1/orders/{id}/items`: Add item
  - `DELETE /api/v1/orders/{id}/items/{sku}`: Remove item
- All endpoints use `@Valid` for input validation

### Subtask 2.8: Exception Handling
- Implemented `GlobalExceptionHandler` with RFC 7807 Problem Details:
  - `MethodArgumentNotValidException` → 400 Bad Request
  - `ConstraintViolationException` → 400 Bad Request
  - `InvalidOrderStateException` → 400 Bad Request
  - `OptimisticLockException` → 409 Conflict
  - `Exception` → 500 Internal Server Error
- All responses include trace IDs for debugging

### Subtask 2.9: Security Configuration
- Configured Spring Security as OAuth2 resource server
- JWT validation against Keycloak
- Role extraction from `realm_access.roles` claim
- Created `UserContextHolder` for extracting user context

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orderdb
DB_USERNAME=orderuser
DB_PASSWORD=orderpass

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SCHEMA_REGISTRY_URL=http://localhost:8081

# Keycloak
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/order-platform
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/order-platform/protocol/openid-connect/certs

# Server
SERVER_PORT=8081
```

### Application Properties

Key configuration in `application.yml`:
- Snapshot interval: 50 events
- Outbox processor poll interval: 1000ms
- Outbox batch size: 100 events
- Kafka topic: `order-events`

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 16
- Kafka 3.6+
- Schema Registry
- Keycloak (for authentication)

### Local Development

1. Start infrastructure services (PostgreSQL, Kafka, Keycloak)
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. The service will be available at `http://localhost:8081`

### Health Checks

- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`
- Metrics: `GET /actuator/prometheus`

## API Examples

### Create Order

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "items": [
      {
        "sku": "SKU-001",
        "productName": "Product A",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ],
    "currency": "USD",
    "idempotencyKey": "unique-key-123"
  }'
```

### Approve Order

```bash
curl -X POST http://localhost:8081/api/v1/orders/{orderId}/approve \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "approvedBy": "123e4567-e89b-12d3-a456-426614174000",
    "reason": "Customer verified"
  }'
```

## Exactly-Once Semantics

The service implements exactly-once semantics through:

1. **Idempotency**: Commands with idempotency keys are deduplicated
2. **Optimistic Locking**: Events have unique constraint on (aggregate_id, version)
3. **Transactional Outbox**: Events are persisted and published atomically
4. **Kafka Transactions**: Producer uses transactional.id for exactly-once delivery

## Monitoring

The service exposes Prometheus metrics including:
- Command processing time
- Event store append time
- Kafka publish time
- Database connection pool metrics
- JVM metrics

## Next Steps

- Task 3: Implement Order Query Service
- Task 4: Implement API Gateway
- Task 5: Set up Keycloak configuration
- Task 6: Create local development environment with docker-compose
