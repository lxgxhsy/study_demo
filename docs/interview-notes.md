# Interview Notes

Add one entry after each completed implementation slice.

## Template

### Slice

Name:

### Business Pressure

What pressure does the high-concurrency order lab simulate?

### Technical Choice

What implementation choice was made?

### Measured Evidence

Which report proves the behavior?

### Tradeoff

What got better, and what got worse?

### Interview Answer

Three to five sentences that can be said directly in an interview.

## 2026-06-08: Dynamic Thread Pool First Slice

### Business Pressure

The order lab simulates high-concurrency requests that generate orders and enqueue asynchronous side work.

### Technical Choice

The first implementation slice focuses on a dynamic thread pool before Leaf ID, because queue backlog, rejection, metrics reset, and task submission form the shortest measurable loop.

### Measured Evidence

Current evidence: `mvn test` passes for application startup, health, config read/update validation, queue shrink rejection, unsupported rejection policy, sleep task submission, caller-runs accounting, and metrics reset generation isolation. The packaged jar also passed `scripts/smoke-thread-pool.ps1` over real HTTP. A real k6 report is still pending because k6 is not installed locally.

### Tradeoff

Starting with the thread pool delays the full order-ID story, but it prevents Leaf and pressure-test code from being built on vague API and metrics contracts.

### Interview Answer

I did not start by building a large order system. I first isolated the async workload path, made the thread pool configurable at runtime, and exposed metrics that show queue size, rejection count, caller-runs count, wait time, and execution time. Reset starts a new metrics generation, so old tasks cannot pollute a new experiment window. This lets me pressure-test how queue size and rejection policy change latency and backpressure before mixing in distributed ID generation.

## 2026-06-08: Leaf Segment Functional Slice

### Business Pressure

The order lab needs IDs without hitting the database for every request, while still avoiding duplicates when traffic crosses segment boundaries.

### Technical Choice

The implementation uses database-backed segment allocation with an in-memory current segment and async next-segment preload. Waiting for a running preload is bounded by `preload-wait-timeout-ms`, so request threads do not wait forever behind a stalled allocation.

### Measured Evidence

Current evidence: `IdLabControllerTest` passes under `local-h2`, including an 8-thread functional duplicate check across multiple segment switches. This is not MySQL/InnoDB concurrency proof.

### Tradeoff

Segment allocation reduces database pressure during ID generation, but it introduces segment waste risk and a database allocation boundary that must be validated under the real MySQL profile.

### Interview Answer

I used a Leaf-style segment generator so most ID requests are served from memory and the database is touched only when allocating a new range. The local H2 test proves the functional path and catches basic duplicate regressions, but I would not present it as InnoDB row-lock or optimistic-update proof. For production-grade evidence, I still need a MySQL-profile run with enough concurrent IDs to cross at least three segments.
