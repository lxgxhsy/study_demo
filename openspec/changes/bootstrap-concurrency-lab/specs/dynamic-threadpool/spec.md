# Spec Delta: Dynamic Thread Pool

## ADDED Requirements

### Requirement: Runtime Thread Pool Configuration

The system shall allow runtime inspection and update of the default `lab` thread pool configuration through stable REST APIs.

#### Scenario: Read current config

- **WHEN** a client requests `GET /api/lab/thread-pool/config`
- **THEN** the system returns HTTP 200
- **AND** the response includes `corePoolSize`, `maximumPoolSize`, `queueCapacity`, `currentQueueSize`, `keepAliveSeconds`, `allowCoreThreadTimeOut`, and `rejectionPolicy`

#### Scenario: Update valid config

- **WHEN** a client submits `PUT /api/lab/thread-pool/config` with valid `corePoolSize`, `maximumPoolSize`, `queueCapacity`, `keepAliveSeconds`, `allowCoreThreadTimeOut`, and `rejectionPolicy`
- **THEN** the system returns HTTP 200
- **AND** the next config response reflects the submitted values
- **AND** the next metrics sample reflects the updated executor behavior

#### Scenario: Reject core greater than max

- **WHEN** a client submits config where `corePoolSize > maximumPoolSize`
- **THEN** the system returns HTTP 400
- **AND** the error code is `INVALID_THREAD_POOL_CONFIG`
- **AND** the current thread pool config is not changed

#### Scenario: Reject queue shrink below current size

- **WHEN** the current queue size is greater than the submitted `queueCapacity`
- **THEN** the system returns HTTP 400
- **AND** the error code is `QUEUE_CAPACITY_TOO_SMALL`
- **AND** queued tasks are not dropped
- **AND** the current thread pool config is not changed

#### Scenario: Reject unsupported rejection policy

- **WHEN** a client submits an unsupported `rejectionPolicy`
- **THEN** the system returns HTTP 400
- **AND** the error code is `UNSUPPORTED_REJECTION_POLICY`
- **AND** the current thread pool config is not changed

### Requirement: Thread Pool Metrics

The system shall expose measurable executor state for pressure-test interpretation.

#### Scenario: Metrics during load

- **WHEN** tasks are submitted under load
- **THEN** `GET /api/lab/thread-pool/metrics` returns active thread count, pool size, largest pool size, queue size, queue capacity, task count, completed task count, submitted task count, rejected task count, caller-runs task count, rejection policy, keep-alive seconds, and allow-core-timeout flag
- **AND** the response includes `waitTimeMsAvg`, `executionTimeMsAvg`, `waitSampleCount`, `executionSampleCount`, `metricsGeneration`, and `metricsResetAt`

#### Scenario: Metrics units and window

- **WHEN** a client reads metrics
- **THEN** wait and execution averages are expressed in milliseconds
- **AND** the averages are cumulative since the last metrics reset

#### Scenario: Reset metrics

- **WHEN** a client requests `POST /api/lab/thread-pool/metrics/reset`
- **THEN** cumulative submitted, completed, rejected, caller-runs, wait, and execution counters are reset
- **AND** `metricsResetAt` is updated
- **AND** `metricsGeneration` is incremented
- **AND** real-time values such as active thread count and queue size are not falsified
- **AND** tasks submitted before the reset do not update the new generation's completed count, wait sample count, or execution sample count when they finish later

#### Scenario: Caller-runs is tracked separately

- **WHEN** the configured rejection policy is `CALLER_RUNS`
- **AND** the executor is saturated
- **THEN** tasks run in the submitting thread are counted in `callerRunsCount` and `callerRunsTaskCount`
- **AND** those tasks are not counted as rejected tasks

### Requirement: Simulated Task Submission

The system shall provide configurable simulated tasks for load testing.

#### Scenario: Submit sleep tasks

- **WHEN** a client submits `POST /api/lab/thread-pool/tasks/sleep` with `count` and `durationMs`
- **THEN** the system schedules accepted tasks
- **AND** the response includes `acceptedCount`, `rejectedCount`, `callerRunsCount`, `submittedCount`, and `requestId`

#### Scenario: Submit CPU tasks

- **WHEN** a client submits `POST /api/lab/thread-pool/tasks/cpu` with `count` and `iterations`
- **THEN** the system schedules accepted tasks
- **AND** the response includes `acceptedCount`, `rejectedCount`, `callerRunsCount`, `submittedCount`, and `requestId`

#### Scenario: Submit mixed tasks

- **WHEN** a client submits `POST /api/lab/thread-pool/tasks/mixed` with `count`, `durationMs`, and `iterations`
- **THEN** the system schedules accepted tasks
- **AND** the response includes `acceptedCount`, `rejectedCount`, `callerRunsCount`, `submittedCount`, and `requestId`

#### Scenario: Reject invalid task request

- **WHEN** a task request is missing required fields or exceeds the documented batch limit
- **THEN** the system returns HTTP 400
- **AND** the error code is `INVALID_TASK_REQUEST`
