# Tasks: Bootstrap Concurrency Lab

## 1. Project Bootstrap

- [ ] Create Spring Boot Java 17 project.
- [ ] Add default local port `8080`.
- [ ] Add `local-h2` profile for local functional flow.
- [ ] Add health endpoint at `GET /actuator/health`.
- [ ] Add package skeleton for `threadpool`, `id`, `orderlab`, and `support`.
- [ ] Add smoke test for context startup and health response.
- [ ] Add README runbook with startup command and health check.

## 2. Dynamic Thread Pool First Slice

- [ ] Implement `ResizableCapacityBlockingQueue` with bounded capacity.
- [ ] Reject queue shrink when `newQueueCapacity < currentQueueSize`.
- [ ] Implement `MonitoredThreadPoolExecutor`.
- [ ] Implement runtime config manager for the default `lab` executor.
- [ ] Add `GET /api/lab/thread-pool/config`.
- [ ] Add `PUT /api/lab/thread-pool/config`.
- [ ] Add stable thread-pool error codes.
- [ ] Add tests for `corePoolSize > maximumPoolSize`, unsupported rejection policy, and queue shrink rejection.

## 3. Thread Pool Metrics and Tasks

- [ ] Add `GET /api/lab/thread-pool/metrics`.
- [ ] Add `POST /api/lab/thread-pool/metrics/reset`.
- [ ] Track wait and execution averages in milliseconds since reset.
- [ ] Expose wait and execution sample counts.
- [ ] Add `POST /api/lab/thread-pool/tasks/sleep`.
- [ ] Add `POST /api/lab/thread-pool/tasks/cpu`.
- [ ] Add `POST /api/lab/thread-pool/tasks/mixed`.
- [ ] Add tests for metrics reset, rejection count, and accepted/rejected response counts.

## 4. First Pressure-Test Loop

- [ ] Add k6 script for safe local thread-pool pressure test.
- [ ] Set first safe run to at most `16 VUs / 30s`.
- [ ] Add report template with required schema fields.
- [ ] Save the first report under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- [ ] Add docs for reading QPS, P95, P99, queue backlog, rejection count, wait time, and execution time.
- [ ] Add interview note for the first thread-pool experiment.

## 5. Leaf-Segment ID

- [ ] Add `mysql-leaf` profile for real segment allocation concurrency validation.
- [ ] Add segment allocation table/entity with `biz_tag`, `max_id`, `step`, optional `version`, and update time.
- [ ] Document whether the first implementation uses optimistic locking or row locks.
- [ ] Implement segment allocation repository.
- [ ] Implement in-memory segment generator.
- [ ] Implement asynchronous next-segment preload.
- [ ] Add `GET /api/lab/id/{bizTag}`.
- [ ] Add `POST /api/lab/id/{bizTag}/batch`.
- [ ] Add `GET /api/lab/id/{bizTag}/metrics`.
- [ ] Add concurrent MySQL-profile duplicate-ID test with concurrency 64 and total IDs at least `step * 3`.

## 6. Order Lab Wrapper

- [ ] Add `POST /api/lab/orders` after thread pool and ID modules are independently verified.
- [ ] Generate order ID through Leaf ID generator.
- [ ] Submit one configurable async side task to the dynamic thread pool.
- [ ] Return order ID plus async accepted/rejected counts.
- [ ] Keep payment, user, inventory persistence, and real downstream calls out of scope.

## 7. Extended Pressure Testing

- [ ] Add thread pool matrix: baseline, small queue, large queue, AbortPolicy, CallerRunsPolicy, CPU task, sleep task.
- [ ] Add ID generation matrix: `step=100`, `step=1000`, `step=10000`.
- [ ] Add double-buffer matrix: preload threshold 10%, 30%, 60%.
- [ ] Save reports with complete schema.
- [ ] Add interview notes that explain at least one measured tradeoff per matrix.
