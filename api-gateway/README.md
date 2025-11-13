# API Gateway

The API Gateway is the single entry point for all external traffic to the Event-Sourced Order Platform. It provides authentication, authorization, routing, rate limiting, and other cross-cutting concerns.

## Features

### 1. OAuth2/OIDC Authentication
- JWT token validation against Keycloak
- Automatic token verification on all requests
- Returns 401 Unauthorized for invalid/expired tokens

### 2. Role-Based Access Control (RBAC)
- **admin** role: Full access to all commands and queries
- **ops** role: Access to queries and command operations
- **analyst** role: Read-only access to queries
- Returns 403 Forbidden for insufficient permissions

### 3. Intelligent Routing
- Routes POST/PUT/DELETE requests to Order Command Service
- Routes GET requests to Order Query Service
- Automatic retry on transient failures (503, 502)

### 4. Correlation ID Propagation
- Generates or propagates `X-Correlation-ID` header
- Injects user context headers (`X-User-ID`, `X-User-Roles`)
- Enables distributed tracing across services

### 5. Rate Limiting
- 100 requests per minute per client (configurable)
- Returns 429 Too Many Requests when limit exceeded
- Includes rate limit headers in responses

### 6. Security Headers
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security` (HSTS)
- `X-XSS-Protection`
- Content Security Policy

### 7. CORS Support
- Configurable allowed origins
- Supports credentials (cookies, auth headers)
- Preflight request caching

## Configuration

### Environment Variables

```bash
# Server Configuration
SERVER_PORT=8080

# Backend Services
COMMAND_SERVICE_URL=http://localhost:8081
QUERY_SERVICE_URL=http://localhost:8082

# Keycloak Configuration
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/order-platform

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# Rate Limiting
RATE_LIMIT_RPM=100
```

### application.yml

See `src/main/resources/application.yml` for full configuration options.

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- Keycloak running on port 8180
- Order Command Service running on port 8081
- Order Query Service running on port 8082

### Start the Gateway

```bash
mvn spring-boot:run
```

The gateway will start on port 8080.

## API Endpoints

### Health Checks
- `GET /actuator/health` - Health status (public)
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe

### Metrics
- `GET /actuator/prometheus` - Prometheus metrics

### Order Commands (requires admin or ops role)
- `POST /api/v1/orders` - Create order
- `POST /api/v1/orders/{id}/approve` - Approve order
- `POST /api/v1/orders/{id}/ship` - Ship order
- `DELETE /api/v1/orders/{id}` - Cancel order

### Order Queries (requires any authenticated role)
- `GET /api/v1/orders` - List orders with filters
- `GET /api/v1/orders/{id}` - Get order by ID
- `GET /api/v1/orders/search` - Full-text search

## Testing

### Get JWT Token from Keycloak

```bash
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/order-platform/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "username=admin@test.com" \
  -d "password=test123" \
  -d "client_id=api-client" | jq -r '.access_token')
```

### Make Authenticated Request

```bash
curl -X GET "http://localhost:8080/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN"
```

### Test Rate Limiting

```bash
for i in {1..105}; do
  curl -X GET "http://localhost:8080/api/v1/orders" \
    -H "Authorization: Bearer $TOKEN" \
    -w "\nStatus: %{http_code}\n"
done
```

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTPS + JWT
       ▼
┌─────────────────────────────────┐
│       API Gateway               │
│  ┌──────────────────────────┐  │
│  │  Security Headers Filter │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │  Correlation ID Filter   │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │  Rate Limiting Filter    │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │  JWT Authentication      │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │  RBAC Authorization      │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │  Route to Backend        │  │
│  └──────────────────────────┘  │
└─────────┬───────────┬───────────┘
          │           │
          ▼           ▼
    ┌─────────┐ ┌─────────┐
    │ Command │ │  Query  │
    │ Service │ │ Service │
    └─────────┘ └─────────┘
```

## Monitoring

### Prometheus Metrics

The gateway exposes metrics at `/actuator/prometheus`:

- `http_server_requests_seconds` - Request duration
- `jvm_memory_used_bytes` - JVM memory usage
- `process_cpu_usage` - CPU usage

### Distributed Tracing

All requests include correlation IDs for tracing:
- Check logs for `X-Correlation-ID`
- Trace requests across services using the correlation ID

## Security Considerations

1. **JWT Validation**: All tokens are validated against Keycloak
2. **HTTPS Only**: Use HTTPS in production (configure TLS termination)
3. **Rate Limiting**: Prevents abuse and ensures fair usage
4. **Security Headers**: Protects against common web vulnerabilities
5. **CORS**: Restricts cross-origin requests to trusted origins

## Troubleshooting

### 401 Unauthorized
- Check if JWT token is valid and not expired
- Verify Keycloak is running and accessible
- Check `KEYCLOAK_ISSUER_URI` configuration

### 403 Forbidden
- Verify user has required role (admin, ops, or analyst)
- Check JWT token contains `realm_access.roles` claim

### 429 Too Many Requests
- Rate limit exceeded (100 requests/minute by default)
- Wait for rate limit window to reset
- Adjust `RATE_LIMIT_RPM` if needed

### 502 Bad Gateway
- Backend service (Command or Query) is not running
- Check `COMMAND_SERVICE_URL` and `QUERY_SERVICE_URL` configuration
- Verify backend services are healthy

## Development

### Project Structure

```
api-gateway/
├── src/main/java/com/orderplatform/gateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── KeycloakJwtAuthenticationConverter.java
│   │   └── CorsConfig.java
│   └── filter/
│       ├── CorrelationIdFilter.java
│       ├── RateLimitingFilter.java
│       └── SecurityHeadersFilter.java
└── src/main/resources/
    └── application.yml
```

### Adding New Routes

Edit `application.yml` and add a new route:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my-new-route
          uri: http://my-service:8080
          predicates:
            - Path=/api/v1/my-resource/**
          filters:
            - StripPrefix=0
```

### Customizing Rate Limits

Modify `RateLimitingFilter.java` to implement per-endpoint or per-role rate limits.
