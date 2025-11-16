# Kubernetes Deployment Guide

## Quick Start

### 1. Prerequisites

Ensure you have the following infrastructure services running in your Kubernetes cluster:

- **Kafka Cluster** (with Zookeeper)
- **Schema Registry**
- **PostgreSQL** (for event store)
- **Elasticsearch** (for read models)
- **Keycloak** (for authentication)
- **Prometheus** (for metrics)
- **Grafana** (for dashboards)
- **Jaeger** (for distributed tracing)
- **OpenTelemetry Collector** (for observability)

### 2. Create Namespace

```bash
kubectl create namespace order-platform
```

### 3. Create Secrets

Create a `secrets.yaml` file with your sensitive data:

```yaml
# secrets.yaml - DO NOT COMMIT TO GIT
apiGateway:
  keycloakClientSecret: "your-keycloak-client-secret"

orderCommandService:
  postgresPassword: "your-postgres-password"
  keycloakClientSecret: "your-keycloak-client-secret"

orderQueryService:
  elasticsearchPassword: "your-elasticsearch-password"
  keycloakClientSecret: "your-keycloak-client-secret"
```

### 4. Deploy Services

#### Option A: Deploy All Services (Production)

```bash
# Deploy API Gateway
helm install api-gateway ./api-gateway \
  -f ./api-gateway/values-prod.yaml \
  --set secrets.keycloakClientSecret="${KEYCLOAK_CLIENT_SECRET}" \
  -n order-platform

# Deploy Order Command Service
helm install order-command-service ./order-command-service \
  -f ./order-command-service/values-prod.yaml \
  --set secrets.postgresPassword="${POSTGRES_PASSWORD}" \
  --set secrets.keycloakClientSecret="${KEYCLOAK_CLIENT_SECRET}" \
  -n order-platform

# Deploy Order Query Service
helm install order-query-service ./order-query-service \
  -f ./order-query-service/values-prod.yaml \
  --set secrets.elasticsearchPassword="${ELASTICSEARCH_PASSWORD}" \
  --set secrets.keycloakClientSecret="${KEYCLOAK_CLIENT_SECRET}" \
  -n order-platform
```

#### Option B: Deploy for Development

```bash
helm install api-gateway ./api-gateway -f ./api-gateway/values-dev.yaml -n order-platform
helm install order-command-service ./order-command-service -f ./order-command-service/values-dev.yaml -n order-platform
helm install order-query-service ./order-query-service -f ./order-query-service/values-dev.yaml -n order-platform
```

### 5. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n order-platform

# Check services
kubectl get svc -n order-platform

# Check HPA status
kubectl get hpa -n order-platform

# Check ingress (if enabled)
kubectl get ingress -n order-platform
```

### 6. Access the Application

#### Via Port-Forward (Development)

```bash
# Forward API Gateway port
kubectl port-forward svc/api-gateway 8080:80 -n order-platform

# Access at http://localhost:8080
```

#### Via Ingress (Production)

Access the application at the configured domain (e.g., https://api.orderplatform.com)

## Environment-Specific Deployments

### Development Environment

- 1 replica per service
- Minimal resource allocation
- DEBUG log level
- No persistence for Query Service
- NetworkPolicy disabled
- Ingress disabled

```bash
helm install <service> ./<service> -f ./<service>/values-dev.yaml -n order-platform
```

### Staging Environment

- 2 replicas per service
- Moderate resource allocation
- INFO log level
- Persistence enabled for Query Service
- NetworkPolicy optional
- Ingress enabled with staging domain

```bash
helm install <service> ./<service> -f ./<service>/values-staging.yaml -n order-platform
```

### Production Environment

- 3+ replicas per service
- Full resource allocation
- INFO log level
- Persistence enabled for Query Service
- NetworkPolicy enabled
- Ingress enabled with production domain
- PodDisruptionBudget enabled

```bash
helm install <service> ./<service> -f ./<service>/values-prod.yaml -n order-platform
```

## Configuration Management

### Update Configuration

```bash
# Upgrade with new configuration
helm upgrade api-gateway ./api-gateway \
  -f ./api-gateway/values-prod.yaml \
  --set config.logLevel=DEBUG \
  -n order-platform
```

### Rollback

```bash
# List release history
helm history api-gateway -n order-platform

# Rollback to previous version
helm rollback api-gateway -n order-platform

# Rollback to specific revision
helm rollback api-gateway 2 -n order-platform
```

## Scaling

### Manual Scaling

```bash
# Scale Order Command Service to 5 replicas
kubectl scale deployment order-command-service --replicas=5 -n order-platform
```

### Horizontal Pod Autoscaling

HPA is enabled by default in staging and production environments:

```bash
# Check HPA status
kubectl get hpa -n order-platform

# Describe HPA for details
kubectl describe hpa order-command-service -n order-platform
```

## Security

### Enable NetworkPolicy

NetworkPolicy is disabled by default. Enable it in production:

```bash
helm upgrade api-gateway ./api-gateway \
  -f ./api-gateway/values-prod.yaml \
  --set networkPolicy.enabled=true \
  -n order-platform
```

### Update Secrets

```bash
# Update secret
kubectl create secret generic order-command-service \
  --from-literal=postgres-password=new-password \
  --dry-run=client -o yaml | kubectl apply -n order-platform -f -

# Restart pods to pick up new secret
kubectl rollout restart deployment order-command-service -n order-platform
```

## Monitoring and Observability

### View Logs

```bash
# Stream logs from all pods of a service
kubectl logs -l app=order-command-service -n order-platform -f

# View logs from specific pod
kubectl logs <pod-name> -n order-platform --tail=100
```

### Access Metrics

```bash
# Port-forward to Prometheus endpoint
kubectl port-forward svc/order-command-service 8080:8080 -n order-platform

# Access metrics at http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/prometheus
```

### Health Checks

```bash
# Check liveness
kubectl exec <pod-name> -n order-platform -- curl http://localhost:8080/actuator/health/liveness

# Check readiness
kubectl exec <pod-name> -n order-platform -- curl http://localhost:8080/actuator/health/readiness
```

## Troubleshooting

### Pod Crashes or CrashLoopBackOff

```bash
# Check pod events
kubectl describe pod <pod-name> -n order-platform

# Check logs
kubectl logs <pod-name> -n order-platform --previous

# Check resource constraints
kubectl top pod <pod-name> -n order-platform
```

### Database Connection Issues

```bash
# Test PostgreSQL connectivity
kubectl run -it --rm psql-test --image=postgres:16 --restart=Never -n order-platform -- \
  psql -h postgresql -U orderuser -d orderdb_prod

# Check ConfigMap
kubectl get configmap order-command-service -o yaml -n order-platform
```

### Kafka Connection Issues

```bash
# Test Kafka connectivity
kubectl run -it --rm kafka-test --image=confluentinc/cp-kafka:7.5.0 --restart=Never -n order-platform -- \
  kafka-topics --bootstrap-server kafka:9092 --list

# Check if order-events topic exists
kubectl run -it --rm kafka-test --image=confluentinc/cp-kafka:7.5.0 --restart=Never -n order-platform -- \
  kafka-topics --bootstrap-server kafka:9092 --describe --topic order-events
```

### Elasticsearch Connection Issues

```bash
# Test Elasticsearch connectivity
kubectl run -it --rm es-test --image=curlimages/curl --restart=Never -n order-platform -- \
  curl -u elastic:password http://elasticsearch:9200/_cluster/health

# Check if index exists
kubectl run -it --rm es-test --image=curlimages/curl --restart=Never -n order-platform -- \
  curl -u elastic:password http://elasticsearch:9200/orders_v1
```

### NetworkPolicy Blocking Traffic

```bash
# Check NetworkPolicy rules
kubectl get networkpolicy -n order-platform
kubectl describe networkpolicy api-gateway -n order-platform

# Temporarily disable for debugging
helm upgrade api-gateway ./api-gateway \
  -f ./api-gateway/values-prod.yaml \
  --set networkPolicy.enabled=false \
  -n order-platform
```

## Maintenance

### Update Application Version

```bash
# Update image tag in values file or use --set
helm upgrade order-command-service ./order-command-service \
  -f ./order-command-service/values-prod.yaml \
  --set image.tag=1.1.0 \
  -n order-platform

# Watch rollout status
kubectl rollout status deployment order-command-service -n order-platform
```

### Backup and Restore

#### PostgreSQL Backup

```bash
# Backup event store
kubectl exec -it postgresql-0 -n order-platform -- \
  pg_dump -U orderuser orderdb_prod > backup.sql

# Restore
kubectl exec -i postgresql-0 -n order-platform -- \
  psql -U orderuser orderdb_prod < backup.sql
```

#### Elasticsearch Snapshot

```bash
# Create snapshot repository
curl -X PUT "elasticsearch:9200/_snapshot/backup_repo" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/backup"
  }
}'

# Create snapshot
curl -X PUT "elasticsearch:9200/_snapshot/backup_repo/snapshot_1?wait_for_completion=true"
```

### Event Replay

If you need to rebuild read models:

```bash
# Enable maintenance mode
helm upgrade order-query-service ./order-query-service \
  -f ./order-query-service/values-prod.yaml \
  --set config.maintenanceMode=true \
  -n order-platform

# Run replay tool (see replay-tool documentation)
kubectl run -it --rm replay-tool --image=order-platform/replay-tool:1.0.0 --restart=Never -n order-platform -- \
  java -jar replay-tool.jar rebuild-elasticsearch orders_v1

# Disable maintenance mode
helm upgrade order-query-service ./order-query-service \
  -f ./order-query-service/values-prod.yaml \
  --set config.maintenanceMode=false \
  -n order-platform
```

## Cleanup

### Uninstall Services

```bash
# Uninstall all services
helm uninstall api-gateway -n order-platform
helm uninstall order-command-service -n order-platform
helm uninstall order-query-service -n order-platform

# Delete namespace (WARNING: This deletes all resources)
kubectl delete namespace order-platform
```

### Delete PVCs

```bash
# List PVCs
kubectl get pvc -n order-platform

# Delete specific PVC
kubectl delete pvc order-query-service -n order-platform
```

## GitOps with Argo CD

For automated deployments using GitOps, see the Argo CD configuration in `../argocd/` directory.

```bash
# Apply Argo CD applications
kubectl apply -f ../argocd/api-gateway-app.yaml
kubectl apply -f ../argocd/order-command-service-app.yaml
kubectl apply -f ../argocd/order-query-service-app.yaml
```

## Additional Resources

- [Helm Documentation](https://helm.sh/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Argo CD Documentation](https://argo-cd.readthedocs.io/)
- [Order Platform Architecture](../README.md)
- [API Documentation](../docs/API.md)
- [Operations Runbook](../docs/OPERATIONS.md)
