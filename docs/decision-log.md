# Decision Log

This file records why project direction changed. Keep entries short, concrete, and tied to implementation consequences.

## 2026-06-08: Reframe as High-Concurrency Order Lab

Decision:

- Reframe the project from a generic concurrency demo to a backend-only high-concurrency order placement lab.

Why:

- PM review argued that a pure "dynamic thread pool + Leaf ID" demo would look like technical checklist work.
- The order scenario gives the project a business pressure: order ID generation and asynchronous order-side work under load.

Implementation consequence:

- Add `orderlab` as a module.
- Keep the order scope narrow: generate order ID, submit one async side task, expose metrics.
- Do not add payment, inventory persistence, users, or real downstream integration.

## 2026-06-08: Remove Service-Side Benchmark Controller from Version 1

Decision:

- Do not implement `BenchmarkController` in version 1.

Why:

- Architecture, PM, and SRE review agreed that service-side benchmark endpoints pollute the measured service resources.
- External k6 scripts are cleaner because the service only exposes the system under test.

Implementation consequence:

- Use `scripts/k6/` for load scripts.
- Save reports under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- Keep backend APIs focused on thread pool, ID, metrics, and order lab.

## 2026-06-08: Queue Shrink Below Current Size Is Rejected

Decision:

- If `newQueueCapacity < currentQueueSize`, return HTTP 400 with `QUEUE_CAPACITY_TOO_SMALL`.

Why:

- Requirement, backend, QA, and architecture review all flagged queue shrink semantics as ambiguous.
- Rejecting the update is simpler, testable, and avoids dropping queued tasks.

Implementation consequence:

- `ResizableCapacityBlockingQueue#setCapacity` must reject invalid shrink.
- Config update must be atomic from the user's perspective: failed update leaves existing config unchanged.

## 2026-06-08: Metrics Use Reset-Scoped Cumulative Averages

Decision:

- `waitTimeMsAvg` and `executionTimeMsAvg` are cumulative averages since the last metrics reset.

Why:

- Reviewers rejected vague "average wait time" wording.
- Reset-scoped cumulative averages are easier to implement and explain than rolling windows.

Implementation consequence:

- Expose `waitSampleCount`, `executionSampleCount`, and `metricsResetAt`.
- Metrics reset clears cumulative counters but must not falsify real-time values like active threads and queue size.

## 2026-06-08: H2 Is Functional Only, MySQL Proves Leaf Concurrency

Decision:

- Use `local-h2` for startup and functional flow.
- Use `mysql-leaf` for real Leaf segment allocation concurrency validation.

Why:

- H2 cannot be treated as evidence for MySQL/InnoDB row-lock or optimistic-update behavior.
- Leaf ID safety is about segment allocation under database concurrency, not only in-memory `AtomicLong`.

Implementation consequence:

- MySQL-profile tests must cover concurrent segment allocation.
- Leaf duplicate-ID acceptance requires concurrency 64 and total IDs at least `step * 3`.

## 2026-06-08: k6 Is the First Pressure-Test Tool

Decision:

- Use k6 for version-1 pressure tests.

Why:

- k6 fits JSON POST APIs, P95/P99 reporting, custom metrics, thresholds, and script-readable payloads.
- JMeter and wrk can be evaluated later but are not needed for the first slice.

Implementation consequence:

- Add k6 scripts before calling a concurrency slice complete.
- If k6 is not installed locally, scripts still exist and Maven tests remain the minimum verification path.

## 2026-06-08: Reports Must Be Reproducible

Decision:

- A report missing required context is not accepted as evidence.

Why:

- QA and SRE review rejected bare QPS/P99 numbers without environment, command, payload, and config snapshots.

Implementation consequence:

- Use `docs/report-template.md`.
- Record command, payload, JDK, CPU, heap, profile, config, raw summary, observation, root-cause explanation, and interview answer.
