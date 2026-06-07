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

## 2026-06-08: Dynamic Thread Pool First Slice Planning

### Business Pressure

The order lab simulates high-concurrency requests that generate orders and enqueue asynchronous side work.

### Technical Choice

The first implementation slice focuses on a dynamic thread pool before Leaf ID, because queue backlog, rejection, metrics reset, and task submission form the shortest measurable loop.

### Measured Evidence

Current evidence: `mvn test` passes for application startup, health, config read/update validation, queue shrink rejection, sleep task submission, and metrics reset. A real k6 report is still pending because k6 is not installed locally.

### Tradeoff

Starting with the thread pool delays the full order-ID story, but it prevents Leaf and pressure-test code from being built on vague API and metrics contracts.

### Interview Answer

I did not start by building a large order system. I first isolated the async workload path, made the thread pool configurable at runtime, and exposed metrics that show queue size, rejection count, wait time, and execution time. This lets me pressure-test how queue size and rejection policy change latency and backpressure before mixing in distributed ID generation.
