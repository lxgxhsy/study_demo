# 决策日志

本文记录项目方向为什么发生变化。每条记录应短、具体，并说明对实现的影响。

## 2026-06-08：重塑为高并发下单实验项目

决策：

- 将项目从通用并发 demo 重塑为后端-only 的高并发下单实验项目。

原因：

- PM 评审认为，单纯的“动态线程池 + Leaf ID”demo 看起来更像技术清单。
- 下单场景能给项目一个业务压力来源：高负载下的订单 ID 生成和订单侧异步任务。

实现后果：

- 新增 `orderlab` 模块。
- 订单范围保持克制：生成订单 ID、提交一个异步 side task、暴露指标。
- 不加入支付、库存持久化、用户体系或真实下游集成。

## 2026-06-08：版本一移除服务端 benchmark controller

决策：

- 版本一不实现 `BenchmarkController`。

原因：

- 架构、PM 和 SRE 评审一致认为，服务端 benchmark endpoint 会污染被测服务资源。
- 外部 k6 脚本更干净，因为服务端只暴露被测系统本身。

实现后果：

- 使用 `scripts/k6/` 存放压测脚本。
- 报告保存到 `reports/YYYYMMDD-HHmmss-<experiment>/`。
- 后端 API 聚焦在线程池、ID、指标和 order lab。

## 2026-06-08：拒绝把队列容量缩小到当前队列大小以下

决策：

- 如果 `newQueueCapacity < currentQueueSize`，返回 HTTP 400 和 `QUEUE_CAPACITY_TOO_SMALL`。

原因：

- 需求、后端、QA 和架构评审都指出队列缩容语义不明确。
- 拒绝更新更简单、可测试，并且避免丢弃已排队任务。

实现后果：

- `ResizableCapacityBlockingQueue#setCapacity` 必须拒绝非法缩容。
- 从用户视角看，配置更新必须是原子的：更新失败后现有配置保持不变。

## 2026-06-08：指标使用 reset-scoped 累计平均值

决策：

- `waitTimeMsAvg` 和 `executionTimeMsAvg` 表示自上次 metrics reset 以来的累计平均值。

原因：

- 评审拒绝了含糊的“平均等待时间”表述。
- reset-scoped 累计平均值比 rolling window 更容易实现，也更容易解释。

实现后果：

- 暴露 `waitSampleCount`、`executionSampleCount` 和 `metricsResetAt`。
- metrics reset 会清空累计计数器，但不能伪造 active threads、queue size 这类实时值。

## 2026-06-08：H2 只用于功能验证，MySQL 用于证明 Leaf 并发语义

决策：

- 使用 `local-h2` 做启动和功能链路验证。
- 使用 `mysql-leaf` 做真实 Leaf 号段分配并发验证。

原因：

- H2 不能被当作 MySQL/InnoDB 行锁或乐观更新行为的证据。
- Leaf ID 安全性关注的是数据库并发下的号段分配，不只是内存里的 `AtomicLong`。

实现后果：

- MySQL profile 测试必须覆盖并发号段分配。
- Leaf 无重复 ID 验收需要并发 64，并且总 ID 数至少为 `step * 3`。

## 2026-06-08：k6 是版本一首选压测工具

决策：

- 版本一压测使用 k6。

原因：

- k6 适合 JSON POST API、P95/P99 报告、自定义指标、阈值和脚本化 payload。
- JMeter 和 wrk 可以后续评估，但第一阶段不需要。

实现后果：

- 在宣称并发切片完成前，需要加入 k6 脚本。
- 如果本地没有安装 k6，脚本仍然应存在，Maven 测试仍作为最低验证路径。

## 2026-06-08：报告必须可复现

决策：

- 缺少必要上下文的报告不能被接受为证据。

原因：

- QA 和 SRE 评审拒绝只有 QPS/P99 数字、没有环境、命令、payload 和配置快照的报告。

实现后果：

- 使用 `docs/report-template.md`。
- 记录命令、payload、JDK、CPU、heap、profile、配置、原始 summary、观察、根因解释和面试回答。
