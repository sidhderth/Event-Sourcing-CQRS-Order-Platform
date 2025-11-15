# OpenTelemetry Java Agent

This directory contains the OpenTelemetry Java Agent configuration and the agent JAR file.

## Download OpenTelemetry Java Agent

The OpenTelemetry Java Agent JAR needs to be downloaded before building the Docker images.

### Automatic Download (Recommended)

Run the download script from the project root:

```bash
# On Linux/Mac
./scripts/download-otel-agent.sh

# On Windows
scripts\download-otel-agent.bat
```

### Manual Download

Download the latest OpenTelemetry Java Agent from:
https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases

```bash
# Using curl
curl -L -o docker/observability/otel/opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Using wget
wget -O docker/observability/otel/opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

## Configuration

The `otel-config.yaml` file configures the OpenTelemetry Collector with:

- **Receivers**: OTLP (gRPC on 4317, HTTP on 4318)
- **Processors**: Batch processing and memory limiting
- **Exporters**: Jaeger for traces, Prometheus for metrics, and logging

## Usage in Docker

The Java Agent is automatically attached to all Spring Boot services via the Dockerfile:

```dockerfile
COPY --from=builder /app/docker/observability/otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
```

## Environment Variables

Each service configures the agent via environment variables in docker-compose.yml:

- `OTEL_SERVICE_NAME`: Service identifier (e.g., "api-gateway")
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Collector endpoint (http://otel-collector:4317)
- `OTEL_TRACES_EXPORTER`: Trace exporter type (otlp)
- `OTEL_METRICS_EXPORTER`: Metrics exporter type (otlp)
- `OTEL_LOGS_EXPORTER`: Logs exporter type (none for now)

## Instrumentation

The agent automatically instruments:

- **HTTP**: Spring Web, Spring WebFlux
- **Database**: JDBC, Hibernate, Spring Data JPA
- **Messaging**: Kafka Producer and Consumer
- **Async**: CompletableFuture, ExecutorService

No code changes are required for basic instrumentation.
