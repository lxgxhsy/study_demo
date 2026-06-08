# 报告模板

每一份被接受的压测报告都必须使用这个模板。

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

## 解读检查清单

- 和对照组相比，发生了什么变化？
- 变化最大的指标是哪一个：QPS、P95、P99、rejected count、wait time、execution time、duplicate count、DB allocation count，还是 segment switch count？
- 这个结果是否符合底层技术机制的预期？
- 哪个取舍可以在面试中讲清楚？
- 根据记录下来的命令和环境，报告是否可以复现？
