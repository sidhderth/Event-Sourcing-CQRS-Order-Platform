# Order Platform - Local Development Environment

This directory contains the Docker Compose configuration and supporting files for running the complete Order Platform locally.

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- At least 8GB of RAM allocated to Docker
- Ports available: 2181, 3000, 4317, 4318, 5050, 5432, 5601, 5778, 6831, 6832, 8080, 8081, 8082, 8090, 9090, 9092, 9093, 9200, 9300, 14250, 14268, 14269, 16686

## Quick Start

### 1. Start All Services

From the project root directory:

```bash
make up
```

Or using docker-compose directly:

```bash
docker-compose up -d
```

### 2. Wait for Services to Start

All services have health checks configured. You can monitor the startup:

```bash
make logs
```

Or check service status:

```bash
make status
```

### 3. Access the Services

Once all services are healthy, you can access:

- **API Gateway**: http://localhost:8080
- **Keycloak Admin Console**: http://localhost:8080 (admin/admin)
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Jaeger UI**: http://localhost:16686
- **Kafka UI**: http://localhost:8090
- **pgAdmin**: http://localhost:5050 (admin@orderplatform.com/admin)
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

### 4. Seed Test Data

```bash
make seed
```

This will create sample orders with various statuses (CREATED, APPROVED, SHIPPED).

## Service Architecture

### Infrastructure Services

- **Zookeeper** (port 2181): Kafka coordination
- **Kafka** (ports 9092, 9093): Event streaming backbone
- **Schema Registry** (port 8081): Avro schema management
- **PostgreSQL** (port 5432): Event store database
- **Elasticsearch** (ports 9200, 9300): Read model storage
- **Keycloak** (port 8080): Identity and access management

### Observability Services

- **Prometheus** (port 9090): Metrics collection
- **Grafana** (port 3000): Metrics visualization
- **Jaeger** (port 16686): Distributed tracing
- **OpenTelemetry Collector** (ports 4317, 4318): Telemetry aggregation

### UI Tools

- **Kafka UI** (port 8090): Browse Kafka topics and messages
- **pgAdmin** (port 5050): PostgreSQL database management
- **Kibana** (port 5601): Elasticsearch query interface

### Application Services

- **API Gateway** (port 8080): Entry point for all API requests
- **Order Command Service** (port 8081): Handles write operations
- **Order Query Service** (port 8082): Handles read operations

## Using the API

### Authentication

First, get an access token from Keycloak:

```bash
curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=api-client" \
  -d "username=admin@test.com" \
  -d "password=test123"
```

Save the `access_token` from the response.

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
curl -X GET "http://localhost:8080/api/v1/orders?page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Using Postman

Import the Postman collection and environment:

1. Open Postman
2. Import `postman/Order-Platform.postman_collection.json`
3. Import `postman/environments/local.postman_environment.json`
4. Select "Order Platform - Local" environment
5. Run "Get Access Token (Admin)" request first
6. The token will be automatically saved and used for subsequent requests

## Test Users

The Keycloak realm comes pre-configured with test users:

| Username | Password | Roles | Permissions |
|----------|----------|-------|-------------|
| admin@test.com | test123 | admin | All commands and queries |
| ops@test.com | test123 | ops | Queries + approve/ship commands |
| analyst@test.com | test123 | analyst | Queries only |

## Monitoring and Debugging

### View Application Logs

```bash
# All services
make logs

# Specific service
docker-compose logs -f order-command-service
```

### Check Kafka Topics

1. Open Kafka UI: http://localhost:8090
2. Navigate to Topics
3. View `order-events` topic messages

### Query PostgreSQL Event Store

1. Open pgAdmin: http://localhost:5050
2. Connect to "Order Platform PostgreSQL" server (password: orderpass)
3. Query the `events` table:

```sql
SELECT * FROM events ORDER BY occurred_at DESC LIMIT 10;
```

### Query Elasticsearch Read Models

1. Open Kibana: http://localhost:5601
2. Go to Dev Tools
3. Query the orders index:

```json
GET /orders_v1/_search
{
  "query": {
    "match_all": {}
  }
}
```

### View Distributed Traces

1. Open Jaeger: http://localhost:16686
2. Select service: api-gateway, order-command-service, or order-query-service
3. Click "Find Traces"

### View Metrics

1. Open Grafana: http://localhost:3000
2. Navigate to Dashboards (once dashboards are created)
3. Or query Prometheus directly: http://localhost:9090

## Stopping Services

```bash
make down
```

## Cleaning Up

To remove all containers, volumes, and images:

```bash
make clean
```

**Warning**: This will delete all data including orders, events, and configurations.

## Troubleshooting

### Services Won't Start

Check Docker resource allocation:
- Ensure at least 8GB RAM is allocated to Docker
- Check available disk space

### Port Conflicts

If ports are already in use, you can modify the port mappings in `docker-compose.yml`.

### Keycloak Not Importing Realm

Ensure the realm export file exists at `keycloak/realm-export.json` before starting Keycloak.

### Application Services Can't Connect to Infrastructure

Wait for all infrastructure services to be healthy before starting application services. The `depends_on` with health checks should handle this automatically.

### Kafka Consumer Lag

Check Kafka UI or Prometheus metrics to monitor consumer lag. The query service may take a few seconds to process events after they're published.

## Directory Structure

```
docker/
├── observability/
│   ├── grafana/
│   │   ├── dashboards/          # Grafana dashboard JSON files
│   │   └── provisioning/        # Datasource and dashboard configs
│   ├── otel/
│   │   └── otel-collector-config.yaml
│   └── prometheus/
│       └── prometheus.yml
├── pgadmin/
│   └── servers.json             # pgAdmin server configuration
└── postgres/
    └── init.sql                 # PostgreSQL initialization script
```

## Next Steps

- Explore the API using the Postman collection
- Create custom Grafana dashboards for business metrics
- Run integration tests: `make test`
- Review distributed traces in Jaeger
- Experiment with event replay scenarios
