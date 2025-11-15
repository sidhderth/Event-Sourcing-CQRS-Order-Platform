# Observability Stack

This directory contains the complete observability stack configuration for the Event-Sourced Order Platform.

## Overview

The observability stack provides comprehensive monitoring, tracing, and logging capabilities:

- **OpenTelemetry Collector**: Receives traces and metrics from all services
- **Prometheus**: Stores and queries metrics
- **Grafana**: Visualizes metrics with pre-built dashboards
- **Jaeger**: Distributed tracing UI
- **Structured JSON Logging**: All services emit JSON logs with trace context

## Components

### OpenTelemetry (otel/)

The OpenTelemetry Java Agent automatically instruments all Spring Boot services for:
- HTTP requests/responses
- Database queries (JDBC)
- Kafka producer/consumer operations
- Custom business metrics

**Setup:**
1. Download the OpenTelemetry Java Agent:
   ```bash
   # Linux/Mac
   ./scripts/download-otel-agent.sh
   
   # Windows
   scripts\download-otel-agent.bat
   ```

2. The agent is automatically attached to services via Dockerfile

**Configuration:**
- `otel-collector-config.yaml`: Collector pipeline configuration
- `otel-config.yaml`: Java agent configuration (deprecated, use env vars)

### Prometheus (prometheus/)

Scrapes metrics from:
- All application services (`/actuator/prometheus`)
- OpenTelemetry Collector
- Self-monitoring

**Configuration:**
- `prometheus.yml`: Scrape targets and retention settings
- Retention: 15 days
- Scrape interval: 10-15 seconds

**Access:**
- URL: http://localhost:9090
- Query examples:
  ```promql
  # Request rate by service
  rate(http_server_requests_seconds_count[5m])
  
  # P95 latency
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))
  
  # Error rate
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))
  ```

### Grafana (grafana/)

Pre-configured with 5 dashboards:

1. **Service Health Overview** (`service-health-overview.json`)
   - Request rate by service
   - Error rate percentage
   - Latency percentiles (P50, P95, P99)
   - JVM heap usage
   - Active instances

2. **Order Platform Business Metrics** (`order-platform-business-metrics.json`)
   - Orders created (last hour)
   - Orders by status (pie chart)
   - Order processing funnel
   - Average order value
   - Top customers by order count
   - Command processing time

3. **Kafka Streams Health** (`kafka-streams-health.json`)
   - Consumer lag by partition
   - Messages consumed rate
   - Task processing latency
   - State store size
   - Rebalance events
   - Active tasks per thread

4. **Database Performance** (`database-performance.json`)
   - PostgreSQL connection pool usage
   - Connection acquire latency
   - Query execution time
   - Transaction rate
   - Elasticsearch indexing rate
   - Elasticsearch query latency

5. **API Gateway Security** (`api-gateway-security.json`)
   - Requests by authentication status
   - Failed authentication attempts
   - JWT validation latency
   - Rate limit exceeded events
   - Requests by role
   - Top forbidden endpoints

**Access:**
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

**Provisioning:**
- Datasources: `provisioning/datasources/datasources.yml`
- Dashboards: `provisioning/dashboards/dashboards.yml`

### Jaeger

Distributed tracing UI for visualizing request flows across services.

**Access:**
- URL: http://localhost:16686

**Features:**
- Trace search by service, operation, tags
- Service dependency graph
- Latency analysis
- Error tracking

## Structured Logging

All services emit JSON logs with:

**Standard Fields:**
- `timestamp`: ISO 8601 format (UTC)
- `level`: Log level (ERROR, WARN, INFO, DEBUG, TRACE)
- `service`: Service name
- `component`: Service component type
- `layer`: Architecture layer
- `logger`: Logger class name
- `message`: Log message
- `thread`: Thread name

**Trace Context:**
- `traceId`: OpenTelemetry trace ID
- `spanId`: OpenTelemetry span ID

**Business Context (MDC):**
- `orderId`: Order aggregate ID
- `customerId`: Customer ID
- `version`: Aggregate version
- `commandType`: Command type (command service)
- `eventType`: Event type
- `correlationId`: Request correlation ID
- `userId`: Authenticated user ID
- `userRoles`: User roles

**Example Log Entry:**
```json
{
  "timestamp": "2025-11-14T10:00:00.123Z",
  "level": "INFO",
  "service": "order-command-service",
  "component": "command",
  "layer": "application",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "logger": "com.orderplatform.command.OrderCommandService",
  "message": "Order created successfully",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "660e8400-e29b-41d4-a716-446655440001",
  "version": 1,
  "commandType": "CreateOrder",
  "thread": "http-nio-8081-exec-1"
}
```

## Environment Variables

Each service is configured with OpenTelemetry environment variables:

```yaml
OTEL_SERVICE_NAME: <service-name>
OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
OTEL_TRACES_EXPORTER: otlp
OTEL_METRICS_EXPORTER: otlp
OTEL_LOGS_EXPORTER: none
OTEL_INSTRUMENTATION_JDBC_ENABLED: "true"
OTEL_INSTRUMENTATION_KAFKA_ENABLED: "true"
OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED: "true"
```

## Monitoring Best Practices

### Key Metrics to Watch

**Golden Signals:**
1. **Latency**: P95 and P99 response times
2. **Traffic**: Request rate per service
3. **Errors**: Error rate percentage
4. **Saturation**: JVM heap usage, connection pool usage

**Business Metrics:**
- Orders created/approved/shipped rates
- Order processing funnel conversion
- Average order value trends

**Infrastructure Metrics:**
- Kafka consumer lag
- Database connection pool exhaustion
- Elasticsearch indexing backlog

### Alerting Recommendations

Set up alerts for:
- Error rate > 1% for 5 minutes
- P95 latency > 1 second for 5 minutes
- Kafka consumer lag > 10,000 messages
- JVM heap usage > 85%
- Connection pool active > 90%

### Troubleshooting

**High Latency:**
1. Check Grafana "Service Health Overview" for P95/P99 latency
2. Find slow traces in Jaeger
3. Check database query performance in "Database Performance" dashboard
4. Review structured logs for the trace ID

**High Error Rate:**
1. Check Grafana "Service Health Overview" for error rate
2. Filter Jaeger traces by error tag
3. Search logs for ERROR level with trace ID
4. Check "API Gateway Security" for authentication failures

**Kafka Consumer Lag:**
1. Check "Kafka Streams Health" dashboard
2. Review consumer lag by partition
3. Check processing latency
4. Look for rebalance events
5. Review logs for processing errors

## Development vs Production

**Development (docker-compose):**
- All observability services run locally
- Debug logging enabled
- No authentication required
- Data retention: 15 days

**Production (Kubernetes):**
- Use managed services where possible (Prometheus, Grafana Cloud)
- INFO logging level
- Enable authentication and RBAC
- Longer retention periods
- Set up alerting rules
- Configure log aggregation (ELK, Loki)

## Next Steps

1. **Download OpenTelemetry Agent**: Run `./scripts/download-otel-agent.sh`
2. **Start Services**: Run `make up` from project root
3. **Access Grafana**: Open http://localhost:3000 (admin/admin)
4. **Generate Traffic**: Use Postman collection or seed data script
5. **Explore Dashboards**: View metrics in Grafana
6. **Trace Requests**: Search traces in Jaeger (http://localhost:16686)
7. **Query Metrics**: Use Prometheus (http://localhost:9090)

## References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Jaeger Tracing](https://www.jaegertracing.io/docs/)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
