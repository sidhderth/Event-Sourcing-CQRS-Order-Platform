# Event-Sourced Order Platform

A production-grade, cloud-native microservices system implementing CQRS and Event Sourcing patterns for order management.

## Architecture Overview

This platform separates write operations (commands) from read operations (queries), using Kafka as the event backbone and maintaining separate data stores optimized for each concern.

### Core Components

- **API Gateway**: Entry point with OAuth2 authentication, rate limiting, and routing
- **Order Command Service**: Handles write operations and publishes domain events
- **Order Query Service**: Materializes read models from events using Kafka Streams
- **Shared Domain**: Common domain models, commands, events, and Avro schemas

### Technology Stack

- **Runtime**: Java 21, Spring Boot 3.x
- **Messaging**: Apache Kafka, Kafka Streams, Schema Registry (Avro)
- **Databases**: PostgreSQL 16 (write-side), Elasticsearch 8 (read-side)
- **Security**: Keycloak, Spring Security OAuth2
- **Observability**: OpenTelemetry, Prometheus, Grafana, Jaeger
- **Orchestration**: Kubernetes, Helm, Argo CD

## Project Structure

```
event-sourced-order-platform/
├── pom.xml                      # Parent POM with dependency management
├── shared-domain/               # Shared domain models and Avro schemas
│   ├── src/main/java/          # Domain models, commands, events
│   └── src/main/avro/          # Avro schema definitions
├── order-command-service/       # Write-side service
│   └── src/main/java/          # Command handlers, event store
├── order-query-service/         # Read-side service
│   └── src/main/java/          # Kafka Streams, Elasticsearch
└── api-gateway/                 # API Gateway
    └── src/main/java/          # Routing, authentication
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose (for local development)

### Building the Project

```bash
# Build all modules
mvn clean install

# Build specific module
cd order-command-service
mvn clean install
```

### Running Locally

Full local development environment with docker-compose will be available in future tasks.

## Key Features

- **Event Sourcing**: All state changes captured as immutable events
- **CQRS**: Separate models for writes and reads
- **Exactly-Once Semantics**: Transactional guarantees across Kafka and databases
- **Domain-Driven Design**: Aggregates enforce business invariants
- **Observability**: Built-in distributed tracing, metrics, and structured logging
- **Security**: OAuth2/OIDC authentication with role-based access control

## Development Status

✅ Task 1: Project structure and shared infrastructure
- Maven multi-module project with Java 21 and Spring Boot 3.x
- Shared domain models (Order aggregate, Money, OrderItem)
- Command DTOs with validation
- Domain events
- Avro schemas for event serialization

🔄 Next: Implement Order Command Service

## Documentation

- [Shared Domain README](shared-domain/README.md)
- [Requirements](.kiro/specs/event-sourced-order-platform/requirements.md)
- [Design](.kiro/specs/event-sourced-order-platform/design.md)
- [Tasks](.kiro/specs/event-sourced-order-platform/tasks.md)

## License

Copyright © 2025 Order Platform Team
