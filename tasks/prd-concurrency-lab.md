# PRD: Concurrency Lab

## 1. Introduction/Overview

Concurrency Lab is a backend-only learning and interview-demo project built around a minimal business domain: a high-concurrency order placement lab.

The lab is not a full order system. It uses a narrow order scenario to make two backend topics concrete:

- Leaf-style order ID generation under concurrent requests.
- Dynamic thread pool tuning for asynchronous order-side work such as notification, inventory simulation, or downstream callbacks.

The goal is to create a runnable lab where each concurrency concept can be changed, pressured, measured, explained, and converted into interview language. The first version focuses on backend APIs, explicit JSON metrics, k6 pressure-test scripts, reproducible reports, and interview notes. UI, login, permissions, production deployment, Snowflake, and a full order workflow are intentionally out of scope.

## 2. Goals

- Build a minimal Spring Boot service for high-concurrency order-lab experiments.
- Build a dynamic thread pool module with runtime configuration, metrics, reset, and simulated task submission.
- Build a Leaf-segment ID generator with verifiable no-duplicate behavior under concurrency.
- Provide k6 pressure-test scripts that expose throughput, latency, queue backlog, rejection count, ID duplicate count, DB allocation count, and segment-switch behavior.
- Keep each implementation slice small enough to complete and verify in one focused coding session.
- Produce reports and notes that explain business pressure, technical tradeoffs, measured evidence, and interview-ready conclusions.

## 3. User Stories

### US-001: Bootstrap Backend Project

**Description:** As a learner, I want a clean backend project skeleton so that dynamic thread pool, Leaf ID, and order-lab modules can be implemented independently.

**Acceptance Criteria:**

- [ ] The project uses Java 17 and Spring Boot.
- [ ] Running `./mvnw spring-boot:run -Dspring-boot.run.profiles=local-h2` starts the service locally. If Maven Wrapper is not present, the README must provide the equivalent `mvn` command.
- [ ] The default local port is `8080`.
- [ ] `GET /actuator/health` returns HTTP 200 within 30 seconds and contains `status=UP`.
- [ ] Package structure separates `threadpool`, `id`, `orderlab`, and `support` modules.
- [ ] A smoke test verifies application context startup and health response.

### US-002: Dynamic Thread Pool Runtime Configuration

**Description:** As a learner, I want to change thread pool parameters at runtime so that I can observe how concurrency settings affect backlog, rejection, latency, and backpressure.

**Acceptance Criteria:**

- [ ] `GET /api/lab/thread-pool/config` returns the current config for the default `lab` executor.
- [ ] `PUT /api/lab/thread-pool/config` updates `corePoolSize`, `maximumPoolSize`, `queueCapacity`, `keepAliveSeconds`, `allowCoreThreadTimeOut`, and `rejectionPolicy`.
- [ ] If `corePoolSize > maximumPoolSize`, the API returns HTTP 400 with code `INVALID_THREAD_POOL_CONFIG` and does not modify the current config.
- [ ] If `newQueueCapacity < currentQueueSize`, the API returns HTTP 400 with code `QUEUE_CAPACITY_TOO_SMALL`, does not modify the current config, and does not drop queued tasks.
- [ ] Unsupported rejection policies return HTTP 400 with code `UNSUPPORTED_REJECTION_POLICY`.
- [ ] A valid update is visible in the next `GET /api/lab/thread-pool/config` response and the next metrics sample.

### US-003: Dynamic Thread Pool Metrics

**Description:** As a learner, I want visible metrics for thread pool behavior so that pressure-test results can be explained with evidence.

**Acceptance Criteria:**

- [ ] `GET /api/lab/thread-pool/metrics` returns active thread count, pool size, largest pool size, queue size, queue capacity, task count, completed task count, submitted task count, rejected task count, rejection policy, keep-alive seconds, and allow-core-timeout flag.
- [ ] The API returns `waitTimeMsAvg` and `executionTimeMsAvg` in milliseconds.
- [ ] `waitTimeMsAvg` and `executionTimeMsAvg` are cumulative averages since the last metrics reset.
- [ ] Metrics include `waitSampleCount`, `executionSampleCount`, and `metricsResetAt`.
- [ ] `POST /api/lab/thread-pool/metrics/reset` clears cumulative submitted, completed, rejected, wait, and execution counters.
- [ ] Metrics reset does not falsify real-time values such as active thread count and queue size.

### US-004: Simulated Load Tasks

**Description:** As a learner, I want to submit configurable task types so that I can compare IO-wait, CPU-heavy, and mixed workloads.

**Acceptance Criteria:**

- [ ] `POST /api/lab/thread-pool/tasks/sleep` accepts `count` and `durationMs`.
- [ ] `POST /api/lab/thread-pool/tasks/cpu` accepts `count` and `iterations`.
- [ ] `POST /api/lab/thread-pool/tasks/mixed` accepts `count`, `durationMs`, and `iterations`.
- [ ] Task submission APIs return `acceptedCount`, `rejectedCount`, `submittedCount`, and `requestId`.
- [ ] Invalid task requests return HTTP 400 with code `INVALID_TASK_REQUEST`.
- [ ] Batch task submission has a documented maximum count to prevent accidental local-machine overload.

### US-005: Order Lab Scenario

**Description:** As a learner, I want a minimal order-lab endpoint so that dynamic thread pools and Leaf ID generation can be explained through a business scenario instead of as disconnected utilities.

**Acceptance Criteria:**

- [ ] `POST /api/lab/orders` returns an order ID generated by the Leaf-segment generator.
- [ ] The endpoint submits one configurable asynchronous side task to the dynamic thread pool.
- [ ] The response includes `orderId`, `acceptedAsyncTasks`, `rejectedAsyncTasks`, and `requestId`.
- [ ] The endpoint does not implement payment, inventory persistence, user accounts, or real downstream integration.
- [ ] The order-lab endpoint is not required in the first coding week; it becomes active after dynamic thread pool and Leaf ID modules have independent tests.

### US-006: Leaf-Segment ID Generator

**Description:** As a learner, I want a Leaf-segment ID generator so that I can understand how DB segment allocation reduces database pressure for high-concurrency order IDs.

**Acceptance Criteria:**

- [ ] Database table stores `biz_tag`, `max_id`, `step`, optional `version`, and update time.
- [ ] The documentation states that `version` is a project teaching extension for optimistic locking; the original Leaf table may not require it when row locks are used.
- [ ] H2 profile verifies the functional ID-generation flow only.
- [ ] MySQL profile is required for real segment allocation concurrency validation.
- [ ] ID generator allocates ID segments by business tag.
- [ ] Segment update is concurrency-safe in MySQL using either optimistic update `WHERE version = ?` with retry or a transactional row-lock strategy.
- [ ] In a single service instance, returned IDs for the same `biz_tag` are monotonically increasing.
- [ ] In future multi-instance mode, the guarantee is global uniqueness and trend increasing, not strict per-client observation order.

### US-007: Segment Double Buffer

**Description:** As a learner, I want the ID generator to preload the next segment before the current segment is exhausted so that ID generation remains stable under load.

**Acceptance Criteria:**

- [ ] Preload threshold is configurable and defaults to remaining IDs below 10% of `step`.
- [ ] Generator starts asynchronous preload when remaining IDs fall below the threshold.
- [ ] Generator switches to the next segment after the current segment is exhausted.
- [ ] Metrics expose `currentStart`, `currentEnd`, `currentValue`, `remaining`, `nextReady`, `preloadRunning`, `dbAllocationCount`, and `segmentSwitchCount`.
- [ ] Concurrent test for one `biz_tag` uses concurrency 64 and total generated IDs of at least `step * 3`.
- [ ] The concurrent test crosses the current segment, the preloaded segment, and a newly allocated DB segment.
- [ ] The concurrent test reports `duplicateCount=0`.

### US-008: Load Testing Scripts and Experiment Matrix

**Description:** As a learner, I want repeatable pressure-test scripts and fixed experiment matrices so that I can learn how parameter changes affect measurable behavior.

**Acceptance Criteria:**

- [ ] First version uses k6 as the primary pressure-test tool.
- [ ] Script parameters include endpoint, concurrency, duration, payload, and output report directory.
- [ ] First local safety run uses at most `16 VUs / 30s`.
- [ ] Local baseline upper bound is `64 VUs / 2min` until machine capacity is measured and documented.
- [ ] Thread pool matrix includes baseline, small queue, large queue, AbortPolicy, CallerRunsPolicy, CPU task, and sleep task.
- [ ] Leaf ID matrix includes `step=100`, `step=1000`, and `step=10000` once the ID module is implemented.
- [ ] Leaf double-buffer matrix includes preload thresholds of 10%, 30%, and 60% once the double buffer is implemented.
- [ ] Reports include QPS, average latency, P95, P99, error count, rejected count when available, duplicate count when available, average wait time, average execution time, and DB allocation count when available.

### US-009: Reproducible Reports

**Description:** As a learner, I want every pressure-test result to be reproducible so that measured conclusions can be reviewed later.

**Acceptance Criteria:**

- [ ] Reports are saved under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- [ ] Each report includes `experimentName`, `hypothesis`, `command`, `payload`, `jdkVersion`, `cpuCores`, `heap`, `gitCommit`, `springProfile`, `threadPoolConfig`, `idConfig`, `dbProfile`, `rawSummary`, `observation`, `rootCauseExplanation`, and `interviewAnswer`.
- [ ] Missing any required report field means the experiment is not accepted as complete.
- [ ] Reports explain at least one tradeoff, such as queue size versus P99 latency, rejection policy versus backpressure, step size versus DB allocation count, or preload threshold versus segment-switch latency.

### US-010: Interview Notes

**Description:** As a learner, I want concise explanation notes so that implementation details can be converted into interview answers.

**Acceptance Criteria:**

- [ ] Notes explain the business problem, technical choice, pressure-test evidence, and interview answer for each completed slice.
- [ ] Notes explain dynamic thread pool parameter effects.
- [ ] Notes explain bounded queue and rejection policy tradeoffs.
- [ ] Notes explain Leaf segment mode, segment waste, DB pressure, and double buffer.
- [ ] Notes include at least one conflict-based conclusion, such as "larger queue lowers rejection but increases wait time/P99".

## 4. Functional Requirements

- FR-1: The system must provide `GET /api/lab/thread-pool/config`.
- FR-2: The system must provide `PUT /api/lab/thread-pool/config`.
- FR-3: The system must reject invalid thread pool configuration with stable error codes.
- FR-4: The system must provide task submission APIs for sleep, CPU, and mixed workloads.
- FR-5: The system must expose dynamic thread pool metrics through `GET /api/lab/thread-pool/metrics`.
- FR-6: The system must support resetting cumulative dynamic thread pool metrics.
- FR-7: The system must provide an ID generation API by business tag.
- FR-8: The system must implement Leaf-segment allocation with database persistence.
- FR-9: The system must protect segment allocation from duplicate ranges under MySQL concurrent access.
- FR-10: The system must expose ID generator metrics.
- FR-11: The system must provide k6 pressure-test scripts for thread pool and ID generation endpoints.
- FR-12: The system must save reproducible pressure-test reports locally.
- FR-13: The system must provide a minimal order-lab endpoint after thread pool and ID modules are independently verified.

## 5. Non-Goals

- No frontend dashboard in the first version.
- No login, RBAC, tenant model, or production permission system.
- No distributed cluster deployment in the first version.
- No full order system, payment workflow, inventory persistence, or real downstream integration.
- No service-side benchmark controller in the first version.
- No production-grade observability stack such as Prometheus and Grafana in the first version.
- No Snowflake implementation until Leaf-segment is complete and tested.

## 6. Design Considerations

- First version is backend-only.
- README, API JSON responses, and reports must be self-explanatory because there is no frontend dashboard.
- APIs should be simple enough for curl, PowerShell, and k6.
- Metrics should be explicit JSON responses rather than hidden only in logs.
- The project should favor readable implementation over abstraction-heavy framework code.
- Each implementation slice must produce a business-pressure story, a technical tradeoff, measured evidence, and an interview-ready conclusion.

## 7. Technical Considerations

- Java 17 and Spring Boot are the preferred defaults.
- Default local port is `8080`.
- `local-h2` profile is for local startup and functional flow.
- `mysql-leaf` profile is required for Leaf segment concurrency validation.
- k6 is the first pressure-test tool.
- Dynamic queue resizing requires a custom bounded queue wrapper because standard fixed-capacity queues do not expose safe capacity mutation.
- First version rejects queue shrink requests where `newQueueCapacity < currentQueueSize`.
- Thread pool metrics track task wait time and execution time in milliseconds.
- Segment allocation uses optimistic locking or transactional row-lock update to avoid duplicate segment ranges.

## 8. Success Metrics

- A developer can run the backend and first safe k6 pressure test locally in under 5 minutes after dependencies are installed.
- Thread pool pressure tests show measured differences between baseline, small queue, large queue, AbortPolicy, CallerRunsPolicy, CPU task, and sleep task.
- ID generation pressure tests produce `duplicateCount=0` in repeated concurrent MySQL-profile runs.
- Pressure-test reports contain the required schema and can explain bottlenecks without guessing.
- Each completed slice has an interview note that ties business pressure, technical choice, metric evidence, and tradeoff together.

## 9. Open Questions

- Should the project later include a minimal dashboard after backend experiments are stable?
- Should Snowflake be version 2 or a separate OpenSpec change?
