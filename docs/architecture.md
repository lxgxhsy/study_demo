# 架构骨架

## 领域定位

Concurrency Lab 被定义为一个高并发下单实验项目。

订单领域被刻意收窄，只保留和并发压力有关的最小链路：

- 生成订单 ID。
- 提交一个订单侧异步任务。
- 暴露可观测指标。
- 对行为进行压测。

它不是支付系统、库存系统、用户系统，也不是生产级订单系统。

## 模块

### 动态线程池

目的：

- 展示 `corePoolSize`、`maxPoolSize`、队列容量和拒绝策略如何影响积压、拒绝、等待时间、执行时间和 P99 延迟。

主要对象：

- `DynamicThreadPoolManager`
- `MonitoredThreadPoolExecutor`
- `ResizableCapacityBlockingQueue`
- `ThreadPoolMetrics`
- `ThreadPoolConfigRequest`
- `ThreadPoolConfigResponse`
- `TaskSubmissionRequest`
- `TaskSubmissionResponse`
- `ThreadPoolLabController`

版本一关键决策：

- 当 `newQueueCapacity < currentQueueSize` 时，拒绝缩小队列容量。

### Leaf ID

目的：

- 展示号段分配如何在保持订单 ID 唯一性的同时减少数据库压力。

主要对象：

- `IdGenerator`
- `SegmentIdGenerator`
- `SegmentBuffer`
- `LeafAllocEntity`
- `LeafAllocRepository`
- `LeafIdMetrics`
- `IdLabController`

验证边界：

- H2 只验证本地功能链路。
- MySQL 用来验证真实号段分配并发语义。

### Order Lab

目的：

- 给并发实验一个业务化场景，而不是构建完整订单系统。

主要对象：

- `OrderLabController`
- `OrderLabService`
- `OrderRequest`
- `OrderResponse`

### Support

目的：

- 将跨模块的 API 错误处理和时间工具留在核心模块之外。

主要对象：

- `ApiErrorResponse`
- `ErrorCode`
- `ClockProvider`

## API 草案

```text
GET  /api/lab/thread-pool/config
PUT  /api/lab/thread-pool/config
GET  /api/lab/thread-pool/metrics
POST /api/lab/thread-pool/metrics/reset
POST /api/lab/thread-pool/tasks/sleep
POST /api/lab/thread-pool/tasks/cpu
POST /api/lab/thread-pool/tasks/mixed

GET  /api/lab/id/{bizTag}
POST /api/lab/id/{bizTag}/batch
GET  /api/lab/id/{bizTag}/metrics

POST /api/lab/orders
```

版本一不包含：

```text
POST /api/lab/benchmark/thread-pool
POST /api/lab/benchmark/id
```

压测由外部 k6 脚本完成。

## 第一段编码切片

1. 搭建 Spring Boot 应用骨架。
2. 添加 `GET /actuator/health`。
3. 添加固定初始配置的动态线程池。
4. 添加配置读取/更新接口。
5. 添加指标接口。
6. 添加指标 reset 接口。
7. 添加 sleep task 提交接口。
8. 添加一个安全的 k6 压测脚本。
9. 保存一份可复现实验报告。
10. 添加一份面试笔记。

Leaf ID 在动态线程池闭环可测量之后再开始。
