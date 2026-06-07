# Multi-Agent Review Notes

## Review Goal

Use role-specific review to reduce generic AI-generated project drift. Each role argued from its own taste and responsibility, with evidence from the local PRD/OpenSpec/Superpowers documents.

## Roles

- PM: demanded a business-shaped story instead of a pure technology checklist.
- Requirement reviewer: rejected vague acceptance language and asked for measurable criteria.
- Technical director: pushed for conservative architecture boundaries and explicit tradeoffs.
- Backend developer: asked for API contracts, DTOs, errors, and a realistic first-week slice.
- QA: asked for assertions, failure paths, and concurrency test boundaries.
- Frontend/experience: supported no frontend in version 1, but demanded self-explanatory README, JSON, and reports.
- SRE: asked for runbook, environment baseline, pressure-test safety bounds, and reproducible reports.

## Main Disputes and Outcomes

### Generic Demo vs Business Domain

Dispute:

- PM argued a standalone dynamic-threadpool/Leaf demo would feel like an interview checklist.
- Backend wanted to keep the first implementation small.

Outcome:

- Documentation now frames the project as a high-concurrency order placement lab.
- Implementation remains small: first week is still the dynamic thread pool loop.

### Service-Side Benchmark vs External Pressure Test

Dispute:

- Initial architecture included a benchmark module and benchmark endpoints.
- PM, architecture, and SRE argued this would pollute the measured service.

Outcome:

- Version 1 removes service-side benchmark controllers.
- Pressure testing is external-first with k6.

### H2 vs MySQL Evidence

Dispute:

- Initial docs allowed H2 first and MySQL later.
- QA, SRE, and architecture argued H2 cannot prove MySQL/InnoDB segment allocation safety.

Outcome:

- H2 is documented as functional only.
- MySQL is required for Leaf concurrency evidence.

### Queue Capacity Shrink

Dispute:

- Initial docs said capacity changes must not drop tasks but did not define shrink behavior.

Outcome:

- Shrinking below current queue size returns HTTP 400 with `QUEUE_CAPACITY_TOO_SMALL`.

### Metrics and Reports

Dispute:

- Initial docs listed metrics but did not define units, windows, or report schema.

Outcome:

- Thread-pool wait and execution averages are milliseconds since reset.
- Reports must include full reproducibility context and an interview answer.

## Current Coding Entry

Start with:

1. Spring Boot Java 17 skeleton.
2. `GET /actuator/health`.
3. Thread pool config read/update.
4. Thread pool metrics/reset.
5. Sleep task endpoint.
6. k6 safe local script.
7. First reproducible report.
