# Payment Microservices Platform

This repository contains a small payment system built around two Spring Boot services:

- `payment-gateway` accepts payment requests
- `payment-processor` processes them and returns the result

The application itself is simple by design. The main goal of this repository is to show a practical DevOps-oriented setup: containerization, local reproducibility, Kubernetes deployment, service-to-service communication, health checks, metrics, and production-minded defaults.

## What The System Does

`payment-gateway` receives a `POST /pay` request and forwards it to `payment-processor` using the `PROCESSOR_URL` environment variable.

Both services expose:

- `GET /healthz`
- `GET /metrics`

## Services

| Service | Image | Description |
| --- | --- | --- |
| payment-gateway Service A | `devops-challenge/payment-gateway:1.0.0` | HTTP service that accepts incoming payment requests and forwards them to the payment-processor. Exposes port `8080`. |
| payment-processor Service B | `devops-challenge/payment-processor:1.0.0` | Backend service that processes payment requests received from the payment-gateway and returns a response. Exposes port `8080`. |

## Repo Structure

```text
.
|-- apps/
|   |-- payment-gateway/
|   |-- payment-processor/
|-- k8s/
|-- apps/docker-compose.yml
|-- README.md
```

## Infrastructure Included

The repository includes:

- Dockerfiles for both services
- `apps/docker-compose.yml` for local multi-container execution
- Kubernetes manifests under `k8s/`
- `kustomization.yaml` for applying the full stack
- `ConfigMap` for shared non-secret configuration
- `Secret` template for the processor API key
- Deployments and Services for both applications
- `NetworkPolicy`, `PodDisruptionBudget`, and `HorizontalPodAutoscaler`

## Why It Is Set Up This Way

This implementation tries to balance simplicity with production awareness.

- The services are packaged separately so they can be built and deployed independently.
- Configuration is externalized so the same images can run locally or in Kubernetes.
- The gateway uses retry, timeout, and circuit-breaker protection for downstream failures.
- The Kubernetes manifests are split into small files so they are easier to review and maintain.
- The containers run with hardened security settings instead of default permissive runtime behavior.

## Quick Start

### 1. Run with Docker Compose

```bash
docker compose -f apps/docker-compose.yml up --build
```

Once the stack is up:

- Gateway: `http://localhost:8080`
- Processor: `http://localhost:8081`

### 2. Try the app

Check health:

```bash
curl http://localhost:8080/healthz
curl http://localhost:8081/healthz
```

Send a payment:

```bash
curl -X POST http://localhost:8080/pay \
  -H "Content-Type: application/json" \
  -d '{
    "source": "acc-1001",
    "destination": "acc-2002",
    "amount": 250.00,
    "currency": "INR"
  }'
```

Check metrics:

```bash
curl http://localhost:8080/metrics
curl http://localhost:8081/metrics
```

## Kubernetes Setup

### Prerequisites

- Docker
- `kubectl`
- A local Kubernetes cluster such as `kind` or `minikube`

### Build the images

```bash
docker build -t devops-challenge/payment-gateway:1.0.0 apps/payment-gateway
docker build -t devops-challenge/payment-processor:1.0.0 apps/payment-processor
```

If you are using `kind`, load the images:

```bash
kind load docker-image devops-challenge/payment-gateway:1.0.0
kind load docker-image devops-challenge/payment-processor:1.0.0
```

### Create the shared secret

```bash
kubectl create namespace payments --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic payments-secrets \
  --namespace payments \
  --from-literal=PROCESSOR_API_KEY=changeme-shared-secret \
  --dry-run=client -o yaml | kubectl apply -f -
```

You can also use `k8s/02-secret.example.yaml` as a template, but for real usage the secret should be created outside source control.

The example secret is intentionally not included in `kustomization.yaml`, so `kubectl apply -k k8s` will not create a placeholder secret by mistake.

### Deploy everything

```bash
kubectl apply -k k8s
```

### Access the gateway

```bash
kubectl -n payments port-forward svc/payment-gateway 8080:8080
```

Then test:

```bash
curl http://localhost:8080/healthz
curl http://localhost:8080/metrics
```

## Kubernetes Features

- `Namespace` isolation with the `payments` namespace
- `ConfigMap` driven runtime configuration
- `Secret` based API key injection
- `startupProbe`, `readinessProbe`, and `livenessProbe`
- `RollingUpdate` deployment strategy
- Two replicas for each service by default
- `HorizontalPodAutoscaler` for CPU-based scaling
- `PodDisruptionBudget` to reduce voluntary downtime
- `NetworkPolicy` restricting processor access to the gateway
- Prometheus scrape annotations on both Deployments
- HPA behavior tuning to reduce scale flapping

## Security And Reliability Notes

- `payment-processor` requires an `X-API-Key` header for application traffic.
- Containers run as non-root users.
- Privilege escalation is disabled.
- The root filesystem is read-only.
- Linux capabilities are dropped.
- The gateway degrades more gracefully when the processor is slow or unavailable.

## Operational Considerations

The platform is designed to simplify troubleshooting during production incidents.

- Health endpoints (`/healthz`) allow Kubernetes and operators to quickly determine service availability.
- Metrics (`/metrics`) expose application telemetry for Prometheus and Grafana dashboards.
- Startup, readiness, and liveness probes automatically detect and replace unhealthy containers.
- Retry, timeout, and circuit breaker mechanisms reduce the impact of downstream failures.
- Container logs can be integrated with a centralized logging solution for faster incident investigation.
- Future enhancements include Alertmanager, distributed tracing, and correlation IDs to enable rapid root-cause analysis during on-call incidents.

## What I Would Improve Next

If this project were extended further, the next practical improvements would include:

- Implement a complete CI/CD pipeline (Jenkins or GitHub Actions) for automated build, testing, security scanning, image publishing, and Kubernetes deployment.
- Integrate SonarQube for static code analysis and Trivy for container image vulnerability scanning as part of the deployment pipeline.
- Replace Kubernetes Secrets with HashiCorp Vault or the External Secrets Operator for centralized and secure secret management.
- Deploy a full observability stack using Prometheus and Grafana for monitoring, along with centralized logging for application and cluster visibility.
- Introduce NGINX Ingress with TLS termination to securely expose services in a production environment.
- Organize Kubernetes manifests using Kustomize overlays for environment-specific configurations (development, staging, and production).
- Support advanced deployment strategies such as Canary or Blue-Green deployments to enable zero-downtime releases and safer rollouts.

## Trade-offs

This is intentionally a lightweight reference project.

- Plain Kubernetes manifests are used instead of Helm.
- Observability endpoints are exposed, but a full monitoring stack is not included in the repo.
- The setup is optimized for local reproducibility first.
