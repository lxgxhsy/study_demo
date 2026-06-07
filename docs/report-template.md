# Report Template

Use this template for every accepted pressure-test report.

```yaml
experimentName:
hypothesis:
command:
payload:
jdkVersion:
cpuCores:
heap:
gitCommit:
springProfile:
dbProfile:
threadPoolConfig:
idConfig:
rawSummary:
observation:
rootCauseExplanation:
interviewAnswer:
```

## Interpretation Checklist

- What changed compared with the control group?
- Which metric moved most: QPS, P95, P99, rejected count, wait time, execution time, duplicate count, DB allocation count, or segment switch count?
- Is the result expected from the technical mechanism?
- What tradeoff can be explained in an interview?
- Is the report reproducible from the recorded command and environment?
