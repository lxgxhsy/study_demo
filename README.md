# Concurrency Lab

Backend-only high-concurrency order placement lab for learning and interview preparation.

The first version focuses on two backend topics:

- Dynamic thread pool configuration, metrics, rejection, and backpressure.
- Leaf-style order ID generation, segment allocation, double buffer, and load testing.

The lab is intentionally not a full order system. The order scenario exists only to make the technical tradeoffs concrete.

## Current Status

Documentation-first. No application code has been generated yet.

Read in this order:

1. `tasks/prd-concurrency-lab.md`
2. `openspec/changes/bootstrap-concurrency-lab/`
3. `superpowers/workflow.md`
4. `docs/architecture.md`

## First Version Scope

Included:

- Spring Boot backend.
- JSON APIs and JSON metrics.
- k6 pressure-test scripts.
- Reproducible reports under `reports/`.
- Interview notes.

Excluded:

- Frontend dashboard.
- Auth, RBAC, tenancy.
- Full order/payment/inventory workflow.
- Service-side benchmark controller.
- Production observability stack.
- Snowflake.

## Planned Local Baseline

```text
JDK: Java 17
Port: 8080
Functional profile: local-h2
Leaf concurrency profile: mysql-leaf
Health: GET /actuator/health
First safe pressure run: 16 VUs / 30s
Local baseline upper bound: 64 VUs / 2min
```

## Runbook

Start the backend:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local-h2
```

Verify health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Run the local smoke script in another terminal:

```powershell
.\scripts\smoke-thread-pool.ps1
```

Run tests:

```powershell
mvn test
```

Run the first safe k6 pressure test after k6 is installed:

```powershell
k6 run .\scripts\k6\thread-pool-safe.js
```

Optional k6 parameters:

```powershell
$env:VUS='16'
$env:DURATION='30s'
$env:TASK_COUNT='20'
$env:TASK_DURATION_MS='100'
$env:QUEUE_CAPACITY='64'
$env:REJECTION_POLICY='ABORT'
k6 run .\scripts\k6\thread-pool-safe.js
```

Reports go under:

```text
reports/YYYYMMDD-HHmmss-<experiment>/
```

## First Coding Slice

```text
Spring Boot skeleton
health endpoint
thread-pool config API
thread-pool metrics API
metrics reset API
sleep task API
one safe k6 script
one reproducible report
one interview note
```
