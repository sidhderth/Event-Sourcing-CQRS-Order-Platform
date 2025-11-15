.PHONY: help up down build logs clean seed test restart status

# Default target
help:
	@echo "Order Platform - Local Development Environment"
	@echo ""
	@echo "Available targets:"
	@echo "  make up          - Start all services with docker-compose"
	@echo "  make down        - Stop and remove all containers"
	@echo "  make build       - Build Maven project and Docker images"
	@echo "  make logs        - Tail logs from all services"
	@echo "  make clean       - Remove volumes and clean state"
	@echo "  make seed        - Populate test data"
	@echo "  make test        - Run integration tests"
	@echo "  make restart     - Restart all services"
	@echo "  make status      - Show status of all services"
	@echo ""

# Start all services
up:
	@echo "Starting Order Platform services..."
	docker-compose up -d
	@echo ""
	@echo "Services are starting. Use 'make logs' to view logs."
	@echo "Access points:"
	@echo "  - API Gateway:        http://localhost:8080"
	@echo "  - Keycloak:           http://localhost:8080 (admin/admin)"
	@echo "  - Grafana:            http://localhost:3000 (admin/admin)"
	@echo "  - Prometheus:         http://localhost:9090"
	@echo "  - Jaeger:             http://localhost:16686"
	@echo "  - Kafka UI:           http://localhost:8090"
	@echo "  - pgAdmin:            http://localhost:5050 (admin@orderplatform.com/admin)"
	@echo "  - Kibana:             http://localhost:5601"
	@echo "  - Elasticsearch:      http://localhost:9200"

# Stop and remove containers
down:
	@echo "Stopping Order Platform services..."
	docker-compose down
	@echo "Services stopped."

# Build Maven project and Docker images
build:
	@echo "Building Maven project..."
	./mvnw clean install -DskipTests
	@echo ""
	@echo "Building Docker images..."
	docker-compose build --no-cache
	@echo "Build complete."

# Tail logs from all services
logs:
	docker-compose logs -f

# Remove volumes and clean state
clean:
	@echo "Cleaning up Order Platform..."
	docker-compose down -v
	@echo "Removing Docker images..."
	docker rmi -f order-platform/api-gateway:latest || true
	docker rmi -f order-platform/order-command-service:latest || true
	docker rmi -f order-platform/order-query-service:latest || true
	@echo "Cleaning Maven build artifacts..."
	./mvnw clean
	@echo "Clean complete."

# Populate test data
seed:
	@echo "Populating test data..."
	@if [ -f scripts/seed-data.sh ]; then \
		bash scripts/seed-data.sh; \
	else \
		echo "Error: scripts/seed-data.sh not found"; \
		exit 1; \
	fi

# Run integration tests
test:
	@echo "Running integration tests..."
	./mvnw verify -P integration-tests

# Restart all services
restart: down up

# Show status of all services
status:
	@echo "Order Platform Service Status:"
	@echo ""
	docker-compose ps
