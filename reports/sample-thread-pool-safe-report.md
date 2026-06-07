# Sample Report: thread-pool-safe

```yaml
experimentName: thread-pool-safe-sample
hypothesis: A safe low-pressure sleep-task run should keep errors low and expose queue/accepted/rejected metrics.
command: k6 run scripts/k6/thread-pool-safe.js
payload:
  endpoint: POST /api/lab/thread-pool/tasks/sleep
  count: 20
  durationMs: 100
jdkVersion: Java 17 via Maven runtime
cpuCores: to-be-filled-by-runner
heap: to-be-filled-by-runner
gitCommit: not-a-git-repo
springProfile: local-h2
dbProfile: none
threadPoolConfig:
  corePoolSize: 4
  maximumPoolSize: 8
  queueCapacity: 64
  rejectionPolicy: ABORT
idConfig: not-applicable
rawSummary: Replace with k6 summary output.
observation: Replace with measured QPS, P95, P99, accepted count, rejected count, waitTimeMsAvg, and executionTimeMsAvg.
rootCauseExplanation: Explain why observed rejection/backlog/latency happened.
interviewAnswer: Replace with a 3-5 sentence answer that ties the metric result to thread-pool tradeoffs.
```

This sample is a template only. It is not accepted experiment evidence until the raw summary and environment fields are filled from a real run.
