# Load Testing

This folder contains dedicated load tests for Chrono Queue using k6.

## Scenarios

- `k6/scenarios/smoke.js`: Basic API validation at low concurrency.
- `k6/scenarios/throughput.js`: Ramp request rate to find stable throughput limits.
- `k6/scenarios/retry-storm.js`: High sustained ingestion pressure to stress retry/DLQ flows.
- `k6/scenarios/soak.js`: Long-running stability test.

## Prerequisites

- Chrono Queue stack running (typically `docker-compose up -d`)
- Either:
  - local `k6` binary installed, or
  - Docker available (runner falls back to `grafana/k6` image)

## Run

From repo root:

```bash
make load-test-smoke
make load-test-throughput
make load-test-retry-storm
make load-test-soak
```

Or run directly:

```bash
./load-tests/run-k6.sh load-tests/k6/scenarios/smoke.js
API_BASE_URL=http://localhost:8081 ./load-tests/run-k6.sh load-tests/k6/scenarios/throughput.js
```

## Key env overrides

- `API_BASE_URL` (default `http://localhost:8081`)
- `DURATION`, `VUS` for smoke
- `START_RATE`, `PRE_ALLOCATED_VUS`, `MAX_VUS`, stage vars for throughput
- `RATE`, `DURATION` for retry-storm and soak

Example:

```bash
API_BASE_URL=http://localhost:8081 RATE=200 DURATION=10m make load-test-retry-storm
```

## Metrics to watch

- k6: `http_req_failed`, `http_req_duration`
- service health: `/actuator/health`
- producer/worker/retry/dlq container logs
- Grafana dashboards already present in this repo
