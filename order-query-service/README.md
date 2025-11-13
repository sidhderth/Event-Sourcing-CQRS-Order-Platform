# Order Query Service

The Order Query Service is responsible for materializing read models from domain events and serving queries with low latency. It uses Kafka Streams for event processing with exactly-once semantics and Elasticsearch for optimized read operations.

## Features

- **Event Processing**: Consumes events from Kafka using Kafka Streams with exactly-once-v2 semantics
- **Read Model Materialization**: Builds and maintains Elasticsearch indices from domain events
- **Query API**: REST endpoints for filtering, searching, and retrieving orders
- **Full-Text Search**: Search across order items (SKU, product name)
- **Maintenance Mode**: Support for event replay without serving queries
- **Security**: OAuth2/OIDC authentication with JWT validation
- **Observability**: OpenTelemetry integration for distributed tracing and metrics

## Architecture

```
Kafka (order-events topic)
    ↓
Kafka Streams Topology
    ↓
Aggregate Events → OrderReadModel
    ↓
Elasticsearch (orders_v1 index)
    ↓
REST API (Query Endpoints)
```

## Technology Stack

- **Java 21**
- **Spring Boot 3.x**
- **Kafka Streams** (exactly-once-v2)
- **Elasticsearch 8.x**
- **Spring Security OAuth2 Resource Server**
- **OpenTelemetry Java Agent**

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | `localhost:9092` |
| `SCHEMA_REGISTRY_URL` | Schema Registry URL | `http://localhost:8081` |
| `ELASTICSEARCH_HOST` | Elasticsearch host | `localhost` |
| `ELASTICSEARCH_PORT` | Elasticsearch port | `9200` |
| `ELASTICSEARCH_USERNAME` | Elasticsearch username | `elastic` |
| `ELASTICSEARCH_PASSWORD` | Elasticsearch password | `changeme` |
| `KEYCLOAK_ISSUER_URI` | Keycloak issuer URI | `http://localhost:8080/realms/order-platform` |
| `MAINTENANCE_MODE` | Enable maintenance mode | `false` |
| `OTEL_SERVICE_NAME` | Service name for tracing | `order-query-service` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry collector endpoint | `http://localhost:4317` |

## API Endpoints

### Query Orders

```http
GET /api/v1/orders?status={status}&customerId={id}&fromDate={date}&toDate={date}&page={page}&size={size}
Authorization: Bearer {jwt}
```

**Query Parameters:**
- `status` (optional): Filter by order status (CREATED, APPROVED, REJECTED, CANCELED, SHIPPED)
- `customerId` (optional): Filter by customer ID
- `fromDate` (optional): Filter orders created after this date (ISO 8601)
- `toDate` (optional): Filter orders created before this date (ISO 8601)
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sortBy` (optional): Sort field (default: createdAt)
- `sortDirection` (optional): Sort direction (ASC/DESC, default: DESC)

**Response:**
```json
{
  "content": [
    {
      "orderId": "uuid",
      "customerId": "uuid",
      "status": "APPROVED",
      "items": [...],
      "totalAmount": 59.98,
      "currency": "USD",
      "createdAt": "2025-11-11T10:00:00Z",
      "updatedAt": "2025-11-11T10:05:00Z",
      "version": 2
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### Search Orders

```http
GET /api/v1/orders/search?q={query}&page={page}&size={size}
Authorization: Bearer {jwt}
```

**Query Parameters:**
- `q` (required): Search query (searches across SKU, product name, order ID, customer ID)
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

### Get Order by ID

```http
GET /api/v1/orders/{id}
Authorization: Bearer {jwt}
```

**Response:**
```json
{
  "orderId": "uuid",
  "customerId": "uuid",
  "status": "SHIPPED",
  "items": [
    {
      "sku": "SKU-001",
      "productName": "Product A",
      "quantity": 2,
      "unitPrice": 29.99,
      "lineTotal": 59.98
    }
  ],
  "totalAmount": 59.98,
  "currency": "USD",
  "createdAt": "2025-11-11T10:00:00Z",
  "updatedAt": "2025-11-11T10:15:00Z",
  "version": 3,
  "trackingNumber": "TRACK123",
  "carrier": "FedEx"
}
```

## Maintenance Mode

When `MAINTENANCE_MODE=true`:
- Kafka Streams topology will not process events
- All query endpoints return `503 Service Unavailable`
- Health checks remain available

This mode is used during event replay operations to prevent serving stale data.

## Building and Running

### Build

```bash
mvn clean package
```

### Run Locally

```bash
java -jar target/order-query-service-1.0.0-SNAPSHOT.jar
```

### Run with Docker

```bash
docker build -t order-query-service:latest .
docker run -p 8080:8080 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e ELASTICSEARCH_HOST=elasticsearch \
  order-query-service:latest
```

## Kafka Streams State Store

The service maintains a RocksDB state store named `order-state-store` that contains the aggregated order state. This state store:
- Is backed by a changelog topic for recovery
- Enables exactly-once processing semantics
- Survives application restarts
- Can be rebuilt from the event log if corrupted

## Elasticsearch Index

The service creates and manages the `orders_v1` index with the following mapping:
- `orderId`, `customerId`, `status`: keyword fields for exact matching
- `items`: nested objects with SKU and product name
- `totalAmount`: scaled float for precise decimal handling
- `createdAt`, `updatedAt`: date fields for range queries
- Full-text search on `items.productName`

## Monitoring

### Health Check

```http
GET /actuator/health
```

### Metrics

```http
GET /actuator/prometheus
```

**Key Metrics:**
- `kafka.consumer.lag`: Consumer lag per partition
- `kafka.streams.task.process.latency`: Event processing latency
- `elasticsearch.index.time`: Elasticsearch indexing time
- `query.execution.time`: Query execution time

## Security

All endpoints (except health checks) require a valid JWT token from Keycloak. The token must contain:
- Valid signature
- Non-expired timestamp
- Correct issuer
- Roles in `realm_access.roles` claim

All authenticated users can query orders (no specific role required for read operations).

## Troubleshooting

### Consumer Lag Increasing

Check Kafka Streams metrics and logs. Possible causes:
- Elasticsearch indexing slow
- Insufficient stream threads
- Rebalancing issues

### Elasticsearch Connection Errors

Verify:
- Elasticsearch is running and accessible
- Credentials are correct
- Index exists (created on startup)

### Events Not Appearing in Queries

Check:
- Kafka Streams is running (not in maintenance mode)
- Events are being published to `order-events` topic
- No errors in application logs
- Elasticsearch index is not full

## Development

### Running Tests

```bash
mvn test
```

### Integration Tests

Integration tests use Testcontainers to spin up Kafka and Elasticsearch:

```bash
mvn verify -P integration-tests
```
