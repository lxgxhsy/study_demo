# Architecture Skeleton

## Domain

Concurrency Lab is framed as a high-concurrency order placement lab.

The order domain is intentionally narrow:

- Generate an order ID.
- Submit one asynchronous order-side task.
- Expose metrics.
- Pressure-test the behavior.

It is not a payment, inventory, user, or production order system.

## Modules

### Dynamic Thread Pool

Purpose:

- Show how core size, max size, queue capacity, and rejection policy affect backlog, rejection, wait time, execution time, and P99 latency.

Main objects:

- `DynamicThreadPoolManager`
- `MonitoredThreadPoolExecutor`
- `ResizableCapacityBlockingQueue`
- `ThreadPoolMetrics`
- `ThreadPoolConfigRequest`
- `ThreadPoolConfigResponse`
- `TaskSubmissionRequest`
- `TaskSubmissionResponse`
- `ThreadPoolLabController`

Key first-version decision:

- Reject queue capacity shrink when `newQueueCapacity < currentQueueSize`.

### Leaf ID

Purpose:

- Show how segment allocation reduces database pressure while preserving uniqueness for order IDs.

Main objects:

- `IdGenerator`
- `SegmentIdGenerator`
- `SegmentBuffer`
- `LeafAllocEntity`
- `LeafAllocRepository`
- `LeafIdMetrics`
- `IdLabController`

Validation boundary:

- H2 validates local function.
- MySQL validates real segment allocation concurrency.

### Order Lab

Purpose:

- Give the concurrency experiments a business-shaped story without building a full order system.

Main objects:

- `OrderLabController`
- `OrderLabService`
- `OrderRequest`
- `OrderResponse`

### Support

Purpose:

- Keep cross-cutting API error and time utilities out of the core modules.

Main objects:

- `ApiErrorResponse`
- `ErrorCode`
- `ClockProvider`

## API Draft

```text
GET  /api/lab/thread-pool/config
PUT  /api/lab/thread-pool/config
GET  /api/lab/thread-pool/metrics
POST /api/lab/thread-pool/metrics/reset
POST /api/lab/thread-pool/tasks/sleep
POST /api/lab/thread-pool/tasks/cpu
POST /api/lab/thread-pool/tasks/mixed

GET  /api/lab/id/{bizTag}
POST /api/lab/id/{bizTag}/batch
GET  /api/lab/id/{bizTag}/metrics

POST /api/lab/orders
```

Version 1 does not include:

```text
POST /api/lab/benchmark/thread-pool
POST /api/lab/benchmark/id
```

Pressure tests are external k6 scripts.

## First Coding Slice

1. Bootstrap Spring Boot app.
2. Add `GET /actuator/health`.
3. Add dynamic thread pool with fixed initial config.
4. Add config read/update endpoint.
5. Add metrics endpoint.
6. Add metrics reset endpoint.
7. Add sleep task submission endpoint.
8. Add one safe k6 pressure-test script.
9. Save one reproducible report.
10. Add one interview note.

Leaf ID starts after the dynamic thread pool loop is measurable.
