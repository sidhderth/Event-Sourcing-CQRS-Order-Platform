# Multi-stage build for Order Command Service
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY shared-domain/pom.xml ./shared-domain/
COPY order-command-service/pom.xml ./order-command-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl shared-domain,order-command-service

# Copy source code
COPY shared-domain/src ./shared-domain/src
COPY order-command-service/src ./order-command-service/src

# Build the application
RUN ./mvnw clean package -pl shared-domain,order-command-service -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Download OpenTelemetry Java Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

# Copy the built artifact from builder stage
COPY --from=builder /app/order-command-service/target/*.jar /app/order-command-service.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Set OpenTelemetry options
ENV OTEL_SERVICE_NAME=order-command-service
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=prometheus
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_INSTRUMENTATION_JDBC_ENABLED=true
ENV OTEL_INSTRUMENTATION_KAFKA_ENABLED=true

# Run the application with OpenTelemetry agent
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar /app/order-command-service.jar"]
