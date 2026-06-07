# Change Proposal: Bootstrap Concurrency Lab

## Why

The project needs a focused learning environment for high-concurrency interview topics. Dynamic thread pools and Leaf-style IDs are easy to describe abstractly, but the useful learning happens when a business-shaped scenario is changed, pressured, measured, and explained.

The first business domain is a minimal high-concurrency order placement lab:

- Order IDs come from a Leaf-style segment generator.
- Asynchronous order-side work is submitted to a dynamic thread pool.
- Pressure tests prove queue backlog, rejection, latency, segment allocation, duplicate checks, and backpressure behavior with reproducible evidence.

## What Changes

- Add a Spring Boot backend skeleton.
- Add a dynamic thread pool lab module.
- Add a Leaf-segment ID generation lab module.
- Add a minimal order-lab API after the two core modules are independently verified.
- Add external k6 pressure-test scripts.
- Add reproducible report and interview-note documentation.

## Scope

Included:

- Backend APIs.
- Local metrics endpoints.
- Local k6 pressure-test scripts.
- Leaf-segment mode.
- Double-buffer design for ID segments.
- A minimal order-lab endpoint for business framing.
- Reproducible reports under `reports/`.

Excluded:

- Frontend dashboard.
- Authentication and authorization.
- Distributed deployment.
- Production observability stack.
- Service-side benchmark controller.
- Full order/payment/inventory system.
- Snowflake mode in the first change.

## Decisions

- H2 is only a functional local profile.
- MySQL is required for Leaf segment concurrency validation.
- Queue capacity shrink below current queue size is rejected.
- Metrics use milliseconds and reset-scoped cumulative averages.
- k6 is the first pressure-test tool.
- First coding week focuses on the dynamic thread pool loop before Leaf implementation begins.

## Impact

- Establishes the package structure and API boundary for future coding.
- Converts role-review findings into measurable acceptance criteria.
- Keeps future changes small enough to review, test, pressure, and explain.
