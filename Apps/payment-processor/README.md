# payment-processor

Backend service that processes payment requests received from the
`payment-gateway` and returns a response. Built with Spring Boot 3 / Java 17.

## Endpoints

| Method | Path       | Description                                              |
|--------|------------|-----------------------------------------------------------|
| POST   | `/process` | Processes a payment request. Requires `X-API-Key` header |
| GET    | `/healthz` | Liveness/readiness health check (Spring Boot Actuator)    |
| GET    | `/metrics` | Prometheus-compatible metrics                             |

### `POST /process` request body

```json
{
  "source": "acc-1001",
  "destination": "acc-2002",
  "amount": 250.00,
  "currency": "INR"
}
```

Required header: `X-API-Key: <shared secret>` (also matching `PROCESSOR_API_KEY`).
Requests without a valid key receive `401 Unauthorized`.

## Configuration (environment variables)

| Variable                          | Default                   | Description                                              |
|------------------------------------|----------------------------|-----------------------------------------------------------|
| `PROCESSOR_API_KEY`                | `changeme-shared-secret`  | Shared secret expected on `X-API-Key`                      |
| `SIMULATED_FAILURE_RATE_PERCENT`   | `0`                        | % of requests to intentionally fail, useful to test the gateway's resilience (retry/circuit breaker) |

## Security note

Authentication here is a simple shared-secret header, sufficient for the
scope of this take-home exercise. For a real production payment platform this
would be hardened with **mutual TLS** between services (e.g. via a service
mesh such as Istio/Linkerd) and/or short-lived, per-service tokens issued by
an identity provider, plus network-level isolation (see the root README and
`k8s/networkpolicy.yaml`).

## Run locally

```bash
mvn spring-boot:run
docker build -t devops-challenge/payment-processor:1.0.0 .
docker run -p 8081:8080 -e PROCESSOR_API_KEY=changeme-shared-secret devops-challenge/payment-processor:1.0.0
```
