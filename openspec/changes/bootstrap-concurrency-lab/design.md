# Design: Bootstrap Concurrency Lab

## Architecture

```text
client / k6 script / curl
  -> REST APIs
    -> threadpool module
    -> id module
    -> orderlab module
  -> explicit JSON metrics
  -> local reports
```

The service must not pressure-test itself in version 1. Pressure tests run from external scripts so the measured service resources are not polluted by service-side benchmark controllers.

## Package Layout

```text
com.example.concurrencylab
├── threadpool
│   ├── DynamicThreadPoolManager
│   ├── MonitoredThreadPoolExecutor
│   ├── ResizableCapacityBlockingQueue
│   ├── ThreadPoolMetrics
│   ├── ThreadPoolConfigRequest
│   ├── ThreadPoolConfigResponse
│   ├── TaskSubmissionRequest
│   ├── TaskSubmissionResponse
│   └── ThreadPoolLabController
├── id
│   ├── IdGenerator
│   ├── SegmentIdGenerator
│   ├── SegmentBuffer
│   ├── LeafAllocEntity
│   ├── LeafAllocRepository
│   ├── LeafIdMetrics
│   └── IdLabController
├── orderlab
│   ├── OrderLabController
│   ├── OrderLabService
│   ├── OrderRequest
│   └── OrderResponse
└── support
    ├── ApiErrorResponse
    ├── ErrorCode
    └── ClockProvider
```

## Runtime Baseline

- Java 17.
- Spring Boot.
- Default local port: `8080`.
- Local functional profile: `local-h2`.
- Leaf concurrency validation profile: `mysql-leaf`.
- Health check: `GET /actuator/health`.
- First safe pressure-test run: `16 VUs / 30s`.
- Local upper bound before machine-specific tuning: `64 VUs / 2min`.

## API Contract

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

Stable error codes:

- `INVALID_THREAD_POOL_CONFIG`
- `QUEUE_CAPACITY_TOO_SMALL`
- `UNSUPPORTED_REJECTION_POLICY`
- `INVALID_TASK_REQUEST`
- `ID_SEGMENT_ALLOC_FAILED`
- `ID_BATCH_SIZE_TOO_LARGE`

## Dynamic Thread Pool Design

`DynamicThreadPoolManager` owns named executors. Version 1 starts with one default executor named `lab`.

`MonitoredThreadPoolExecutor` records:

- submitted task count
- completed task count
- rejected task count
- caller-runs task count
- queue wait time in milliseconds
- execution time in milliseconds
- sample counts for wait and execution metrics
- metrics generation
- `metricsResetAt`

`ResizableCapacityBlockingQueue` provides bounded queue behavior with adjustable capacity. Capacity changes must not drop queued tasks.

First-version shrink rule:

```text
if newQueueCapacity < currentQueueSize:
    reject config update with QUEUE_CAPACITY_TOO_SMALL
```

Metrics reset starts a new metrics generation and clears reset-scoped counters. Tasks submitted before the reset do not update the new generation's completed count, wait samples, or execution samples when they finish later. Reset does not fake real-time values such as active thread count or queue size.

## Leaf ID Design

`LeafAllocEntity` stores segment allocation state:

```text
biz_tag
max_id
step
version
update_time
```

`version` is a project teaching extension for optimistic locking. If a row-lock strategy is selected for MySQL, `version` may not be required for correctness.

`SegmentIdGenerator` holds current and next `SegmentBuffer` in memory. It allocates IDs from memory and loads the next DB segment asynchronously when remaining IDs fall below a configurable threshold. The default threshold is 10% of `step`.

If request threads exhaust the current segment while async preload is still running, they wait up to `preload-wait-timeout-ms` for the next segment. Timeout returns `ID_SEGMENT_ALLOC_FAILED` instead of waiting forever behind a stalled database allocation.

Validation boundary:

- H2 validates functional flow only.
- MySQL validates real segment allocation concurrency.

MySQL segment allocation must use either:

- optimistic update with `WHERE version = ?` and retry; or
- transactional row lock such as `SELECT ... FOR UPDATE`.

## Order Lab Design

The order-lab endpoint is a business wrapper, not a real order system.

`POST /api/lab/orders` should:

1. Generate an order ID through the Leaf ID generator.
2. Submit one configurable asynchronous side task to the dynamic thread pool.
3. Return the order ID plus async accepted/rejected/caller-runs counts.

It must not add payment, user, inventory persistence, or real downstream integration in version 1.

## Pressure Testing Design

Pressure testing is external-first. Version 1 uses k6 scripts and local report directories.

Thread pool matrix:

- baseline
- small queue
- large queue
- AbortPolicy
- CallerRunsPolicy
- CPU task
- sleep task

Leaf matrix after ID module implementation:

- `step=100`
- `step=1000`
- `step=10000`
- preload threshold 10%
- preload threshold 30%
- preload threshold 60%

Report directory:

```text
reports/YYYYMMDD-HHmmss-<experiment>/
```

Each report must include:

- experiment name
- hypothesis
- command
- payload
- JDK version
- CPU cores
- heap
- git commit
- Spring profile
- DB profile
- thread pool config
- ID config
- raw summary
- observation
- root-cause explanation
- interview answer

## First Coding Slice

1. Bootstrap Spring Boot app.
2. Add health endpoint.
3. Add dynamic thread pool with fixed initial config.
4. Add config read/update API.
5. Add metrics and reset API.
6. Add sleep task submission API.
7. Add one safe k6 pressure-test script and one reproducible report.

Leaf ID starts after the dynamic thread pool loop is measurable.
