# Design Decisions & Trade-offs

This document explains the reasoning behind the setup in this repository,
written from a "what would I want in place before handing this to an
on-call team" perspective, and what was intentionally left out given the
~4 hour time-box.

## Architecture

See `docs/architecture.png`.

```
 Client
   │  HTTPS
   ▼
┌─────────────────────┐        HTTP + X-API-Key        ┌───────────────────────┐
│   payment-gateway    │ ───────────────────────────▶  │   payment-processor    │
│  (Spring Boot, 2 pods)│ ◀───────────────────────────  │  (Spring Boot, 2 pods) │
└─────────────────────┘        JSON response            └───────────────────────┘
        /healthz  /metrics                                  /healthz  /metrics
```

Both services are stateless HTTP APIs (Spring Boot 3 / Java 17), packaged as
independent Docker images and deployed as separate Kubernetes Deployments +
Services inside a dedicated `payments` namespace.

## Why Spring Boot

Spring Boot Actuator gives production-grade health/metrics endpoints
("`/healthz`" and "`/metrics`" via `management.endpoints.web.path-mapping`)
essentially for free, and Micrometer's Prometheus registry produces
Prometheus-compatible output out of the box, which matches the requirements
exactly without hand-rolling health/metrics plumbing.

## Communication & Security

- **Transport**: plain HTTP inside the cluster for simplicity of the
  exercise. `PROCESSOR_URL` is injected via a ConfigMap so it can be
  swapped per environment.
- **Authentication between services**: a shared-secret header (`X-API-Key`)
  is required by `payment-processor` for every request except health/metrics
  probes. The secret is stored in a Kubernetes `Secret` and injected as an
  env var — never hardcoded in the images.
- **Network segmentation**: a `NetworkPolicy` restricts `payment-processor`
  to only accept ingress traffic from pods labelled `app: payment-gateway`,
  so even a compromised pod elsewhere in the cluster cannot reach it
  directly.
- **Container hardening**: both images run as a non-root user, with
  `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, all
  Linux capabilities dropped, and the default seccomp profile applied.
- **What I'd add in real production** (not implemented here due to the
  time-box):
  - **mTLS** between services via a service mesh (Istio/Linkerd) or
    Spring's own TLS client/server certs, replacing the static shared
    secret with rotating certificates.
  - Short-lived, scoped tokens (e.g. SPIFFE/SPIRE identities or OAuth2
    client-credentials) instead of one static API key.
  - Secrets sourced from a vault (Sealed Secrets / External Secrets
    Operator + AWS/GCP/Vault) instead of a plain `Secret` manifest.
  - WAF/rate limiting at the ingress edge, and input sanitisation/fraud
    checks beyond basic bean validation.
  - Encryption at rest for anything persisted (no datastore was required
    for this exercise, so none was added).

## Reliability

- `payment-gateway` wraps calls to `payment-processor` with **Resilience4j**:
  retry with exponential backoff for transient I/O errors, a circuit
  breaker that opens on sustained failures (so it fails fast instead of
  piling up threads waiting on a dead dependency), and a time limiter to
  bound latency. On failure it returns a `503` with a clear message rather
  than crashing or hanging indefinitely.
- Both deployments run **2 replicas** behind a `ClusterIP` Service, with a
  **PodDisruptionBudget** (`minAvailable: 1`) so voluntary disruptions
  (node drains, upgrades) can't take a service fully offline, and a
  **HorizontalPodAutoscaler** to react to load.
- `RollingUpdate` strategy with `maxUnavailable: 0` guarantees zero-downtime
  deploys.
- `startupProbe` / `readinessProbe` / `livenessProbe` are all wired to
  `/healthz`, so Kubernetes won't route traffic to a pod that hasn't
  finished starting the JVM, and will restart pods that hang.
- `payment-processor` exposes `SIMULATED_FAILURE_RATE_PERCENT` purely to
  make it easy to demonstrate/verify the gateway's resilience behaviour
  under degraded conditions without extra tooling.
- **What I'd add given more time**: chaos testing (e.g. scaling the
  processor to 0 and observing gateway behaviour is possible today, but
  automated chaos experiments were out of scope), multi-AZ topology
  spread (a `topologySpreadConstraints` stub is included), and a proper
  outbox/retry-queue pattern if payments needed to be durable across a
  processor outage (currently a failed call is simply reported back to the
  caller — no built-in redelivery).

## Operability

- Structured console logs include a **correlation id** (`X-Request-Id`)
  propagated from the gateway to the processor and echoed back in the
  response, added to every log line via MDC. An on-call engineer can grep
  a single id across both services' logs to reconstruct what happened to
  one specific payment.
- `/metrics` exposes HTTP latency/error histograms per route out of the box
  via Micrometer + Spring MVC instrumentation, ready to be scraped by
  Prometheus (pod annotations `prometheus.io/scrape` etc. are set for
  clusters without a full Prometheus Operator/ServiceMonitor setup).
- **What I'd add given more time**: a docker-compose based Prometheus +
  Grafana stack (or `ServiceMonitor` CRDs for the Prometheus Operator) with
  a couple of starter dashboards/alerts (error rate, p99 latency, circuit
  breaker state), and shipping logs to a central store (Loki/ELK) instead
  of container stdout only.

## Engineering quality / repo layout

- Each service is a self-contained Maven project with its own Dockerfile
  and README so it can be built/run/tested independently.
- Multi-stage Dockerfiles keep runtime images small (JRE-only, Alpine base)
  and avoid shipping the Maven build toolchain.
- Kubernetes manifests are split into small, single-purpose files (and
  aggregated with `kustomization.yaml`) instead of one large monolithic
  YAML, which makes diffs and reviews easier.
- No Helm chart was introduced — for two simple services, plain manifests
  are easier to read for a reviewer and don't need a templating layer yet.
  If a third service were added, moving to Helm/Kustomize overlays
  (per-environment) would be the natural next step.

## Explicitly out of scope (time-box)

- CI/CD pipeline (build/scan/push images, GitOps deploy) — mentioned as a
  next step; not implemented.
- Image vulnerability scanning (Trivy/Grype) in CI.
- Ingress/TLS termination at the edge (only `ClusterIP` Services + a
  `kubectl port-forward` instruction are provided for local testing).
- Persistent storage / database — neither service currently needs one.
- Centralized log/metrics stack (Prometheus/Grafana/Loki) — the endpoints
  are exposed and ready to be scraped, but the observability stack itself
  isn't deployed as part of this challenge.
