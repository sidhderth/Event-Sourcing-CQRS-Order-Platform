# Event-Sourced Order Platform

A production-grade, cloud-native microservices system implementing CQRS and Event Sourcing patterns for order management. This platform demonstrates enterprise-level architecture with exactly-once semantics, distributed tracing, and comprehensive observability.

## 🏗️ Architecture Overview

This platform separates write operations (commands) from read operations (queries), using Kafka as the event backbone and maintaining separate data stores optimized for each concern.

### Core Components

- **API Gateway**: Entry point with OAuth2 authentication, rate limiting, and routing
- **Order Command Service**: Handles write operations, enforces business rules, and publishes domain events
- **Order Query Service**: Materializes read models from events using Kafka Streams with exactly-once semantics
- **Shared Domain**: Common domain models, commands, events, and Avro schemas

### Technology Stack

- **Runtime**: Java 21, Spring Boot 3.x
- **Build**: Maven 3.9+
- **Messaging**: Apache Kafka 7.5, Kafka Streams, Confluent Schema Registry (Avro)
- **Databases**: PostgreSQL 16 (event store), Elasticsearch 8 (read models)
- **Security**: Keycloak 24, Spring Security OAuth2/OIDC
- **Observability**: OpenTelemetry, Prometheus, Grafana, Jaeger
- **Containerization**: Docker, Docker Compose
- **Orchestration**: Kubernetes, Helm, Argo CD (production)

## 📁 Project Structure

```
event-sourced-order-platform/
├── pom.xml                          # Parent POM with dependency management
├── docker-compose.yml               # Local development environment
├── Makefile                         # Convenience commands
├── shared-domain/                   # Shared domain models and Avro schemas
│   ├── src/main/java/              # Domain models, commands, events
│   └── src/main/avro/              # Avro schema definitions
├── order-command-service/           # Write-side service
│   ├── src/main/java/              # Command handlers, event store, aggregates
│   ├── src/main/resources/         # Configuration and Flyway migrations
│   └── Dockerfile                   # Multi-stage Docker build
├── order-query-service/             # Read-side service
│   ├── src/main/java/              # Kafka Streams topology, Elasticsearch
│   ├── src/main/resources/         # Configuration
│   └── Dockerfile                   # Multi-stage Docker build
├── api-gateway/                     # API Gateway
│   ├── src/main/java/              # Routing, authentication, rate limiting
│   ├── src/main/resources/         # Configuration
│   └── Dockerfile                   # Multi-stage Docker build
├── docker/                          # Docker configuration files
│   ├── observability/              # Prometheus, Grafana, OTel configs
│   ├── postgres/                   # PostgreSQL init scripts
│   └── pgadmin/                    # pgAdmin configuration
├── keycloak/                        # Keycloak realm configuration
├── scripts/                         # Utility scripts
│   └── seed-data.sh                # Test data population script
└── postman/                         # API testing
    ├── Order-Platform.postman_collection.json
    └── environments/
        └── local.postman_environment.json
```

## 🚀 Getting Started

### Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Docker Desktop** (or Docker Engine + Docker Compose)
- **At least 8GB RAM** allocated to Docker
- **Git** for version control

### Quick Start (5 minutes)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd event-sourced-order-platform
   ```

2. **Build the project**
   ```bash
   mvn clean install -DskipTests
   ```

3. **Start all services**
   ```bash
   make up
   ```
   
   This starts:
   - Infrastructure (Kafka, PostgreSQL, Elasticsearch, Keycloak)
   - Observability stack (Prometheus, Grafana, Jaeger)
   - Application services (API Gateway, Command Service, Query Service)
   - UI tools (Kafka UI, pgAdmin, Kibana)

4. **Wait for services to be healthy** (2-3 minutes)
   ```bash
   make status
   ```

5. **Seed test data**
   ```bash
   make seed
   ```

6. **Access the platform**
   - API Gateway: http://localhost:8080
   - Grafana: http://localhost:3000 (admin/admin)
   - Jaeger: http://localhost:16686
   - Kafka UI: http://localhost:8090

### Building the Project

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
cd order-command-service
mvn clean package

# Run tests
mvn test

# Run integration tests
mvn verify -P integration-tests
```

### Running Locally with Docker Compose

The project includes a complete local development environment with all infrastructure and observability services.

```bash
# Start all services
make up

# View logs
make logs

# Stop services
make down

# Clean everything (removes volumes)
make clean

# Rebuild Docker images
make build

# Restart services
make restart

# Show service status
make status
```

See [docker/README.md](docker/README.md) for detailed documentation.

## 🔑 Key Features

### Event Sourcing
- All state changes captured as immutable events in PostgreSQL
- Complete audit trail of all order lifecycle changes
- Event replay capability for debugging and new projections
- Snapshot optimization for aggregate reconstruction

### CQRS (Command Query Responsibility Segregation)
- Separate models optimized for writes (PostgreSQL) and reads (Elasticsearch)
- Independent scaling of command and query services
- Eventual consistency with configurable lag monitoring

### Exactly-Once Semantics
- Transactional outbox pattern for command service
- Kafka transactions for atomic database + Kafka writes
- Kafka Streams exactly-once-v2 processing guarantee
- Idempotency keys for duplicate command detection

### Domain-Driven Design
- Rich domain model with Order aggregate
- Business invariants enforced at aggregate boundaries
- Domain events capture business-meaningful state changes
- Value objects (Money, OrderItem) for type safety

### Security
- OAuth2/OIDC authentication via Keycloak
- Role-based access control (admin, ops, analyst)
- JWT token validation with Spring Security
- Rate limiting and request throttling

### Observability
- Distributed tracing with OpenTelemetry and Jaeger
- Metrics collection with Prometheus
- Grafana dashboards for visualization
- Structured JSON logging with correlation IDs
- Health checks and readiness probes

### Schema Evolution
- Avro schemas with backward/forward compatibility
- Schema Registry for centralized schema management
- Versioned event schemas

## 📡 API Usage

### Authentication

Get an access token from Keycloak:

```bash
curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=api-client" \
  -d "username=admin@test.com" \
  -d "password=test123"
```

### Create an Order

```bash
curl -X POST "http://localhost:8080/api/v1/orders" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174001",
    "items": [
      {
        "sku": "LAPTOP-001",
        "productName": "Dell XPS 15",
        "quantity": 1,
        "unitPrice": 1299.99
      }
    ],
    "currency": "USD"
  }'
```

### Query Orders

```bash
curl -X GET "http://localhost:8080/api/v1/orders?status=APPROVED&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Using Postman

1. Import `postman/Order-Platform.postman_collection.json`
2. Import `postman/environments/local.postman_environment.json`
3. Select "Order Platform - Local" environment
4. Run "Get Access Token (Admin)" to authenticate
5. Use other requests (token is automatically applied)

## 🔍 Monitoring & Debugging

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | Bearer token |
| Keycloak | http://localhost:8080 | admin/admin |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Jaeger | http://localhost:16686 | - |
| Kafka UI | http://localhost:8090 | - |
| pgAdmin | http://localhost:5050 | admin@orderplatform.com/admin |
| Kibana | http://localhost:5601 | - |
| Elasticsearch | http://localhost:9200 | - |

### Test Users

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| admin@test.com | test123 | admin | All operations |
| ops@test.com | test123 | ops | Queries + approve/ship |
| analyst@test.com | test123 | analyst | Queries only |

### View Distributed Traces

1. Open Jaeger: http://localhost:16686
2. Select service (api-gateway, order-command-service, order-query-service)
3. Click "Find Traces" to see request flows

### Query Event Store

```sql
-- View recent events
SELECT * FROM events 
ORDER BY occurred_at DESC 
LIMIT 10;

-- View events for specific order
SELECT * FROM events 
WHERE aggregate_id = 'your-order-id'
ORDER BY version;
```

### Query Read Models

```bash
# Search orders in Elasticsearch
curl -X GET "http://localhost:9200/orders_v1/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{"query": {"match_all": {}}}'
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests (requires Docker)
mvn verify -P integration-tests

# Run tests for specific module
cd order-command-service
mvn test

# Generate coverage report
mvn jacoco:report
```

## 📚 Documentation

- [Docker Environment Guide](docker/README.md) - Complete local development setup
- [Shared Domain README](shared-domain/README.md) - Domain models and events
- [Design Document](.kiro/specs/event-sourced-order-platform/design.md) - Architecture details
- [Requirements](.kiro/specs/event-sourced-order-platform/requirements.md) - Feature requirements
- [Tasks](.kiro/specs/event-sourced-order-platform/tasks.md) - Implementation plan

## 🏗️ Development Status

### ✅ Completed

- [x] Task 1: Project structure and shared domain
- [x] Task 6: Local development environment with Docker Compose

### 🚧 In Progress

- [ ] Task 2: Order Command Service implementation
- [ ] Task 3: Order Query Service implementation
- [ ] Task 4: API Gateway implementation
- [ ] Task 5: Keycloak integration and security

### 📋 Planned

- [ ] Task 7: Integration tests with Testcontainers
- [ ] Task 8: Grafana dashboards
- [ ] Task 9: Kubernetes deployment manifests
- [ ] Task 10: CI/CD pipeline

## 🤝 Contributing

This is a demonstration project showcasing enterprise architecture patterns. Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Inspired by enterprise event sourcing patterns
- Built with Spring Boot and Apache Kafka ecosystems
- Observability patterns from OpenTelemetry community

