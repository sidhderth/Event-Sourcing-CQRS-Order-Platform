# Helm Charts for Event-Sourced Order Platform

This directory contains Helm charts for deploying the Event-Sourced Order Platform to Kubernetes.

## Charts

### 1. api-gateway
API Gateway service that handles authentication, routing, and cross-cutting concerns.

### 2. order-command-service
Order Command Service that processes write operations and publishes domain events.

### 3. order-query-service
Order Query Service that materializes read models from events and serves queries.

## Directory Structure

```
helm/
├── api-gateway/
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── values-dev.yaml
│   ├── values-staging.yaml
│   ├── values-prod.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── serviceaccount.yaml
│       ├── hpa.yaml
│       ├── pdb.yaml
│       ├── ingress.yaml
│       └── networkpolicy.yaml
├── order-command-service/
│   └── (same structure)
├── order-query-service/
│   └── (same structure)
└── README.md
```

## Installation

### Prerequisites

- Kubernetes cluster (1.24+)
- Helm 3.x installed
- kubectl configured to access your cluster
- Infrastructure services deployed:
  - Kafka cluster
  - PostgreSQL database
  - Elasticsearch cluster
  - Keycloak identity provider

### Install a Chart

#### Development Environment

```bash
# Install API Gateway
helm install api-gateway ./api-gateway -f ./api-gateway/values-dev.yaml -n order-platform --create-namespace

# Install Order Command Service
helm install order-command-service ./order-command-service -f ./order-command-service/values-dev.yaml -n order-platform

# Install Order Query Service
helm install order-query-service ./order-query-service -f ./order-query-service/values-dev.yaml -n order-platform
```

#### Staging Environment

```bash
helm install api-gateway ./api-gateway -f ./api-gateway/values-staging.yaml -n order-platform --create-namespace
helm install order-command-service ./order-command-service -f ./order-command-service/values-staging.yaml -n order-platform
helm install order-query-service ./order-query-service -f ./order-query-service/values-staging.yaml -n order-platform
```

#### Production Environment

```bash
helm install api-gateway ./api-gateway -f ./api-gateway/values-prod.yaml -n order-platform --create-namespace
helm install order-command-service ./order-command-service -f ./order-command-service/values-prod.yaml -n order-platform
helm install order-query-service ./order-query-service -f ./order-query-service/values-prod.yaml -n order-platform
```

### Upgrade a Chart

```bash
helm upgrade api-gateway ./api-gateway -f ./api-gateway/values-prod.yaml -n order-platform
helm upgrade order-command-service ./order-command-service -f ./order-command-service/values-prod.yaml -n order-platform
helm upgrade order-query-service ./order-query-service -f ./order-query-service/values-prod.yaml -n order-platform
```

### Uninstall a Chart

```bash
helm uninstall api-gateway -n order-platform
helm uninstall order-command-service -n order-platform
helm uninstall order-query-service -n order-platform
```

## Configuration

### Secrets Management

Before deploying, you need to set the following secrets in the values files:

#### API Gateway
- `secrets.keycloakClientSecret`: Keycloak client secret for OAuth2

#### Order Command Service
- `secrets.postgresPassword`: PostgreSQL database password
- `secrets.keycloakClientSecret`: Keycloak client secret for JWT validation

#### Order Query Service
- `secrets.elasticsearchPassword`: Elasticsearch password
- `secrets.keycloakClientSecret`: Keycloak client secret for JWT validation

**Example:**

```bash
# Using --set flag
helm install api-gateway ./api-gateway \
  -f ./api-gateway/values-prod.yaml \
  --set secrets.keycloakClientSecret="your-secret-here" \
  -n order-platform

# Or create a secrets.yaml file (DO NOT commit to Git)
cat > secrets.yaml <<EOF
secrets:
  keycloakClientSecret: "your-secret-here"
  postgresPassword: "your-db-password"
  elasticsearchPassword: "your-es-password"
EOF

helm install order-command-service ./order-command-service \
  -f ./order-command-service/values-prod.yaml \
  -f secrets.yaml \
  -n order-platform
```

### Environment-Specific Configuration

Each chart has three environment-specific values files:

- **values-dev.yaml**: Development environment (1 replica, minimal resources, DEBUG logging)
- **values-staging.yaml**: Staging environment (2 replicas, moderate resources, INFO logging)
- **values-prod.yaml**: Production environment (3+ replicas, full resources, INFO logging)

### Key Configuration Options

#### API Gateway

```yaml
replicaCount: 3
resources:
  limits:
    cpu: 1000m
    memory: 2Gi
config:
  logLevel: INFO
  keycloakUrl: http://keycloak:8080
  rateLimitRequestsPerMinute: 100
ingress:
  enabled: true
  hosts:
  - host: api.orderplatform.com
networkPolicy:
  enabled: true
```

#### Order Command Service

```yaml
replicaCount: 3
resources:
  limits:
    cpu: 1000m
    memory: 2Gi
config:
  postgresHost: postgresql
  postgresDatabase: orderdb_prod
  kafkaBootstrapServers: kafka:9092
  snapshotInterval: 50
networkPolicy:
  enabled: true
```

#### Order Query Service

```yaml
replicaCount: 3
resources:
  limits:
    cpu: 2000m
    memory: 4Gi
config:
  kafkaApplicationId: order-query-service-prod
  kafkaProcessingGuarantee: exactly_once_v2
  elasticsearchHosts: http://elasticsearch:9200
  elasticsearchIndexName: orders_v1
persistence:
  enabled: true
  size: 20Gi
networkPolicy:
  enabled: true
```

## Features

### High Availability

- **HorizontalPodAutoscaler**: Automatically scales pods based on CPU utilization (70% target)
- **PodDisruptionBudget**: Ensures minimum availability during voluntary disruptions
- **Pod Anti-Affinity**: Spreads pods across different nodes

### Security

- **NetworkPolicy**: Restricts network traffic between pods (disabled by default, enable in production)
- **SecurityContext**: Runs containers as non-root user with dropped capabilities
- **ReadOnlyRootFilesystem**: Prevents container filesystem modifications (except for necessary paths)
- **Secrets**: Sensitive data stored in Kubernetes Secrets

### Observability

- **Prometheus Metrics**: Exposed at `/actuator/prometheus` endpoint
- **OpenTelemetry**: Distributed tracing with automatic instrumentation
- **Health Probes**: Liveness and readiness probes for container health monitoring
- **Structured Logging**: JSON logs with trace IDs for correlation

### Ingress

- **TLS/SSL**: Automatic certificate management with cert-manager
- **Rate Limiting**: Nginx ingress rate limiting (configurable)
- **Path-based Routing**: Routes traffic to API Gateway

## Monitoring

### Check Deployment Status

```bash
# List all releases
helm list -n order-platform

# Check pod status
kubectl get pods -n order-platform

# Check HPA status
kubectl get hpa -n order-platform

# Check PDB status
kubectl get pdb -n order-platform
```

### View Logs

```bash
# API Gateway logs
kubectl logs -l app=api-gateway -n order-platform --tail=100 -f

# Order Command Service logs
kubectl logs -l app=order-command-service -n order-platform --tail=100 -f

# Order Query Service logs
kubectl logs -l app=order-query-service -n order-platform --tail=100 -f
```

### Access Metrics

```bash
# Port-forward to access Prometheus metrics
kubectl port-forward svc/api-gateway 8080:80 -n order-platform
curl http://localhost:8080/actuator/prometheus
```

## Troubleshooting

### Pods Not Starting

```bash
# Describe pod to see events
kubectl describe pod <pod-name> -n order-platform

# Check logs
kubectl logs <pod-name> -n order-platform

# Check if secrets are properly set
kubectl get secret -n order-platform
```

### Database Connection Issues

```bash
# Check if PostgreSQL is accessible
kubectl run -it --rm debug --image=postgres:16 --restart=Never -n order-platform -- psql -h postgresql -U orderuser -d orderdb_prod

# Check ConfigMap values
kubectl get configmap order-command-service -o yaml -n order-platform
```

### Kafka Connection Issues

```bash
# Check if Kafka is accessible
kubectl run -it --rm debug --image=confluentinc/cp-kafka:7.5.0 --restart=Never -n order-platform -- kafka-topics --bootstrap-server kafka:9092 --list
```

### NetworkPolicy Issues

If pods cannot communicate after enabling NetworkPolicy:

```bash
# Check NetworkPolicy rules
kubectl get networkpolicy -n order-platform
kubectl describe networkpolicy <policy-name> -n order-platform

# Temporarily disable NetworkPolicy for debugging
helm upgrade <release-name> ./<chart-name> -f values-prod.yaml --set networkPolicy.enabled=false -n order-platform
```

## Best Practices

1. **Always use environment-specific values files** instead of modifying the base values.yaml
2. **Store secrets in a secure vault** (e.g., HashiCorp Vault, AWS Secrets Manager) and inject them at deployment time
3. **Enable NetworkPolicy in production** for enhanced security
4. **Monitor resource usage** and adjust requests/limits accordingly
5. **Test upgrades in staging** before applying to production
6. **Use GitOps** (Argo CD) for automated deployments
7. **Enable persistence for Query Service** in production to preserve RocksDB state stores
8. **Configure proper backup strategies** for PostgreSQL and Elasticsearch

## Next Steps

- Set up Argo CD for GitOps deployments (see `../argocd/` directory)
- Configure monitoring and alerting with Prometheus and Grafana
- Set up log aggregation with ELK stack or similar
- Implement backup and disaster recovery procedures
- Configure horizontal pod autoscaling based on custom metrics
