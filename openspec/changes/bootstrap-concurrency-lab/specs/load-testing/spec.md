# Spec Delta: Load Testing

## ADDED Requirements

### Requirement: Repeatable Thread Pool Pressure Test

The system shall provide a repeatable k6-based way to pressure-test thread pool behavior.

#### Scenario: Run safe local thread pool benchmark

- **WHEN** the first local thread pool benchmark is run
- **THEN** it uses at most `16 VUs / 30s`
- **AND** the report is saved under `reports/YYYYMMDD-HHmmss-<experiment>/`

#### Scenario: Run thread pool experiment matrix

- **WHEN** the thread pool experiment matrix is run
- **THEN** it includes baseline, small queue, large queue, AbortPolicy, CallerRunsPolicy, CPU task, and sleep task cases
- **AND** each case reports QPS, average latency, P95 latency, P99 latency, error count, rejected count, average wait time, and average execution time

#### Scenario: Capture backpressure behavior

- **WHEN** CallerRunsPolicy is tested against AbortPolicy under comparable payload and concurrency
- **THEN** the report explains how rejection, caller-side latency, queue wait time, and throughput changed

### Requirement: Repeatable ID Generation Pressure Test

The system shall provide a repeatable k6-based way to pressure-test ID generation after the ID module is implemented.

#### Scenario: Run ID benchmark

- **WHEN** a benchmark is run against the ID batch endpoint
- **THEN** the report includes QPS, average latency, P95 latency, P99 latency, error count, duplicate count, DB allocation count, and segment switch count

#### Scenario: Run Leaf step matrix

- **WHEN** the Leaf ID matrix is run
- **THEN** it includes `step=100`, `step=1000`, and `step=10000`
- **AND** the report explains the tradeoff between DB allocation count, latency, and segment waste

#### Scenario: Run double-buffer threshold matrix

- **WHEN** the double-buffer matrix is run
- **THEN** it includes preload thresholds of 10%, 30%, and 60%
- **AND** the report explains segment-switch latency behavior

### Requirement: Reproducible Report Schema

The system shall save enough report context to reproduce pressure-test conclusions.

#### Scenario: Save benchmark report

- **WHEN** a benchmark completes
- **THEN** the report is saved under `reports/YYYYMMDD-HHmmss-<experiment>/`
- **AND** the report includes `experimentName`, `hypothesis`, `command`, `payload`, `jdkVersion`, `cpuCores`, `heap`, `gitCommit`, `springProfile`, `dbProfile`, `threadPoolConfig`, `idConfig`, `rawSummary`, `observation`, `rootCauseExplanation`, and `interviewAnswer`

#### Scenario: Reject incomplete report as evidence

- **WHEN** a report is missing any required schema field
- **THEN** the report is not accepted as completed experiment evidence
