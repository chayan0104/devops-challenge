# Spring Payment Microservices

This repository is a small microservices project built to show DevOps-ready delivery around a simple payment flow. The application layer is intentionally straightforward: `payment-gateway` receives payment requests and forwards them to `payment-processor`, which validates and returns the processing result. The stronger focus is on how the services are packaged, secured, deployed, observed, and operated.

## DevOps Brief

The project is structured like a lightweight production handoff:

- Two independent Spring Boot services with separate Dockerfiles and runtime configuration.
- Multi-stage container builds to keep images small and runtime-only.
- Kubernetes manifests split by concern for easier review and change tracking.
- Health probes, rolling updates, autoscaling, and disruption budgets for safer deployments.
- Shared-secret based service-to-service authentication with configuration separated into `ConfigMap` and `Secret`.
- Network policies and hardened container security settings to reduce blast radius.
- Built-in metrics and correlation IDs to support monitoring and troubleshooting.

## Services

| Service | Image | Description |
| --- | --- | --- |
| payment-gateway Service A | `devops-challenge/payment-gateway:1.0.0` | HTTP entry service that accepts payment requests and forwards them to the processor. Exposes port `8080`. |
| payment-processor Service B | `devops-challenge/payment-processor:1.0.0` | Internal backend service that processes requests from the gateway and returns the response. Exposes port `8080`. |

## What Is Included

- `docker-compose.yml` for local multi-container runs.
- `k8s/` manifests for namespace, config, secret template, deployments, services, network policy, PDB, HPA, and kustomization.
- Spring Boot Actuator endpoints exposed as `/healthz` and `/metrics`.
- Resilience handling in the gateway for downstream processor failures.

## Local Run

```bash
docker compose up --build
```

- Gateway: `http://localhost:8080`
- Processor: `http://localhost:8081`

## Kubernetes Deploy

```bash
kubectl apply -k k8s
```

Before applying, set a real value for `PROCESSOR_API_KEY` instead of using the sample in `k8s/02-secret.example.yaml`.

## DevOps Notes

- Containers run as non-root users with restricted privileges.
- Deployments use readiness, liveness, and startup probes.
- Both services run with two replicas by default.
- `payment-gateway` can keep serving predictable `503` responses during processor failures instead of hanging.
- Metrics are Prometheus-friendly, and logs include a request correlation ID across both services.

## Scope

This repo demonstrates deployment and operations fundamentals well, but it does not yet include a full CI/CD pipeline, centralized observability stack, ingress/TLS setup, or external secret management.
