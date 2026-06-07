# Superpowers Workflow

This project uses "superpowers" as practical working modes rather than hidden magic.

The working principle is:

```text
business pressure -> technical tradeoff -> measurable experiment -> interview answer
```

If a feature does not produce that chain, it is not done.

## 1. Spec-First Coding

Before implementation:

- Read `tasks/prd-concurrency-lab.md`.
- Read the active OpenSpec change under `openspec/changes/bootstrap-concurrency-lab/`.
- Pick one small task from `openspec/changes/bootstrap-concurrency-lab/tasks.md`.
- Define the smallest verifiable endpoint, test, or pressure-test report for that task.

Done means:

- Code compiles.
- The relevant unit, integration, smoke, or pressure-test check passes.
- The behavior maps back to one OpenSpec scenario.
- The API response or report contains enough context to understand the result without a frontend.
- `docs/interview-notes.md` is updated with business problem, technical choice, measured evidence, tradeoff, and interview answer.

## 2. Pressure-Test First-Class Workflow

For any concurrency feature:

- Add or update a k6 pressure-test script in the same change.
- Capture metrics before and after tuning.
- Save reports under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- Write one note explaining what changed and why.

Every accepted report must include:

- `experimentName`
- `hypothesis`
- `command`
- `payload`
- `jdkVersion`
- `cpuCores`
- `heap`
- `gitCommit`
- `springProfile`
- `dbProfile`
- `threadPoolConfig`
- `idConfig`
- `rawSummary`
- `observation`
- `rootCauseExplanation`
- `interviewAnswer`

Do not call a concurrency feature done if it only passes a unit test and has no pressure-test observation.

## 3. Anti-Homogeneity Definition of Done

To avoid a generic AI-generated Spring Boot demo, every completed feature must have:

- one reproducible experiment
- one failure or bottleneck scenario
- one metric explanation
- one interview conflict conclusion

Examples of acceptable conflict conclusions:

- A larger queue reduced rejection but increased wait time and P99 latency.
- `AbortPolicy` failed fast while `CallerRunsPolicy` slowed the caller and created backpressure.
- Increasing CPU-task thread count stopped improving throughput after CPU saturation.
- Larger Leaf `step` reduced DB allocation count but increased possible segment waste.
- Too-low double-buffer preload threshold caused a segment-switch latency spike.

## 4. Interview Explanation Capture

After each working slice:

- Write the implementation principle in plain language.
- Write the pressure-test observation.
- Write the tradeoff.
- Write the interview answer version in 3 to 5 sentences.

Suggested file:

```text
docs/interview-notes.md
```

## 5. Runbook Discipline

Before a pressure-test result is accepted:

- The startup command is recorded.
- The profile is recorded.
- The health check result is recorded.
- The machine baseline is recorded at least as CPU cores and heap.
- The pressure-test command is recorded.
- The report path is recorded.

First safe local pressure run:

```text
16 VUs / 30s
```

Local baseline upper bound before machine-specific tuning:

```text
64 VUs / 2min
```

## 6. Guardrails

- Keep version 1 backend-only.
- Do not add auth, frontend, or deployment before core experiments work.
- Do not add a service-side benchmark controller in version 1.
- Prefer explicit JSON metrics over logs-only evidence.
- Prefer small APIs that are easy to call from curl, PowerShell, and k6.
- Do not mix Snowflake into the first Leaf-segment implementation.
- H2 is for functional flow only; MySQL is required for Leaf segment concurrency evidence.
