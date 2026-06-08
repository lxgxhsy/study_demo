# Tasks: Bootstrap Concurrency Lab

## 1. Project Bootstrap

- [x] Create Spring Boot Java 17 project.
- [x] Add default local port `8080`.
- [x] Add `local-h2` profile for local functional flow.
- [x] Add health endpoint at `GET /actuator/health`.
- [x] Add package skeleton for `threadpool`, `id`, `orderlab`, and `support`.
- [x] Add smoke test for context startup and health response.
- [x] Add README runbook with startup command and health check.

## 2. Dynamic Thread Pool First Slice

- [x] Implement `ResizableCapacityBlockingQueue` with bounded capacity.
- [x] Reject queue shrink when `newQueueCapacity < currentQueueSize`.
- [x] Implement `MonitoredThreadPoolExecutor`.
- [x] Implement runtime config manager for the default `lab` executor.
- [x] Add `GET /api/lab/thread-pool/config`.
- [x] Add `PUT /api/lab/thread-pool/config`.
- [x] Add stable thread-pool error codes.
- [x] Add tests for `corePoolSize > maximumPoolSize`, unsupported rejection policy, and queue shrink rejection.

## 3. Thread Pool Metrics and Tasks

- [x] Add `GET /api/lab/thread-pool/metrics`.
- [x] Add `POST /api/lab/thread-pool/metrics/reset`.
- [x] Track wait and execution averages in milliseconds since reset.
- [x] Expose wait and execution sample counts.
- [x] Add `POST /api/lab/thread-pool/tasks/sleep`.
- [x] Add `POST /api/lab/thread-pool/tasks/cpu`.
- [x] Add `POST /api/lab/thread-pool/tasks/mixed`.
- [x] Add tests for metrics reset, rejection count, and accepted/rejected response counts.

## 4. First Pressure-Test Loop

- [x] Add k6 script for safe local thread-pool pressure test.
- [x] Set first safe run to at most `16 VUs / 30s`.
- [x] Add report template with required schema fields.
- [ ] Save the first report under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- [x] Add docs for reading QPS, P95, P99, queue backlog, rejection count, wait time, and execution time.
- [x] Add interview note for the first thread-pool experiment.

## 5. Leaf-Segment ID

- [x] Add `mysql-leaf` profile for real segment allocation concurrency validation.
- [x] Add segment allocation table/entity with `biz_tag`, `max_id`, `step`, optional `version`, and update time.
- [x] Document whether the first implementation uses optimistic locking or row locks.
- [x] Implement segment allocation repository.
- [x] Implement in-memory segment generator.
- [x] Implement asynchronous next-segment preload.
- [x] Add `GET /api/lab/id/{bizTag}`.
- [x] Add `POST /api/lab/id/{bizTag}/batch`.
- [x] Add `GET /api/lab/id/{bizTag}/metrics`.
- [x] Add concurrent MySQL-profile duplicate-ID test with concurrency 64 and total IDs at least `step * 3`.

## 6. Order Lab Wrapper

- [x] Add `POST /api/lab/orders` after thread pool and ID modules are independently verified.
- [x] Generate order ID through Leaf ID generator.
- [x] Submit one configurable async side task to the dynamic thread pool.
- [x] Return order ID plus async accepted/rejected/caller-runs counts.
- [x] Keep payment, user, inventory persistence, and real downstream calls out of scope.

## 7. Extended Pressure Testing

- [x] Add thread pool matrix: baseline, small queue, large queue, AbortPolicy, CallerRunsPolicy, CPU task, sleep task.
- [ ] Add ID generation matrix: `step=100`, `step=1000`, `step=10000`.
- [ ] Add double-buffer matrix: preload threshold 10%, 30%, 60%.
- [ ] Save reports with complete schema.
- [ ] Add interview notes that explain at least one measured tradeoff per matrix.
