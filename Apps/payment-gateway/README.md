# payment-gateway

HTTP service that accepts incoming payment requests and forwards them to the
`payment-processor` service. Built with Spring Boot 3 / Java 17.

## Endpoints

| Method | Path       | Description                                              |
|--------|------------|-----------------------------------------------------------|
| POST   | `/pay`     | Accepts a JSON payment request and forwards it downstream |
| GET    | `/healthz` | Liveness/readiness health check (Spring Boot Actuator)    |
| GET    | `/metrics` | Prometheus-compatible metrics                             |

### `POST /pay` request body

```json
{
  "source": "acc-1001",
  "destination": "acc-2002",
  "amount": 250.00,
  "currency": "INR"
}
```

### Response

```json
{
  "transactionId": "b6b8...e1",
  "status": "APPROVED",
  "message": "Payment processed successfully",
  "processedBy": "payment-processor",
  "amount": 250.00,
  "currency": "INR",
  "processedAt": "2025-01-01T10:00:00Z"
}
```

## Configuration (environment variables)

| Variable                      | Default                        | Description                                   |
|--------------------------------|---------------------------------|------------------------------------------------|
| `PROCESSOR_URL`                | `http://localhost:8081`        | Base URL of payment-processor                  |
| `PROCESSOR_API_KEY`            | `changeme-shared-secret`       | Shared secret sent as `X-API-Key` header        |
| `PROCESSOR_CONNECT_TIMEOUT_MS` | `1000`                          | TCP connect timeout to processor               |
| `PROCESSOR_READ_TIMEOUT_MS`    | `3000`                          | Read timeout to processor                      |

## Resilience

Calls to `payment-processor` are wrapped with Resilience4j:
- **Retry** (3 attempts, exponential backoff) for transient I/O failures.
- **Circuit breaker** that opens after a 50% failure rate over the last 20 calls,
  so a degraded processor doesn't cascade into an outage of the gateway.
- **Time limiter** (4s) to bound how long a caller waits.
- On failure/open circuit, the gateway returns `503` with a clear message
  instead of hanging or crashing.

## Run locally

```bash
mvn spring-boot:run
docker build -t devops-challenge/payment-gateway:1.0.0 .
docker run -p 8080:8080 -e PROCESSOR_URL=http://host.docker.internal:8081 devops-challenge/payment-gateway:1.0.0
```
