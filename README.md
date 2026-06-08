# Concurrency Lab

这是一个后端-only 的高并发下单实验项目，用来练动态线程池、Leaf 号段 ID、压测证据和面试表达。

它不是完整订单系统。订单场景只保留两件事：生成订单 ID，提交一段异步订单侧任务。这样业务压力足够具体，代码又不会被支付、库存、账号、权限拖偏。

## 当前状态

当前仓库已经不是纯文档阶段。已存在 Spring Boot 应用、线程池实验接口、Leaf 号段 ID 接口、最小订单包装接口、H2 功能测试、k6 脚本和 smoke 脚本。

证据边界先说清楚：

- `local-h2` 只能证明本地功能链路和回归测试，不证明 MySQL/InnoDB 并发分配号段的生产语义。
- k6 脚本已经有了，但只有真实跑出 `reports/YYYYMMDD-HHmmss-<experiment>/` 后，才算压测证据。
- `CALLER_RUNS` 不是失败拒绝。它会在调用线程执行任务，所以单独记为 `callerRunsTaskCount`。
- `metrics/reset` 会开启新的实验窗口。旧窗口中尚未结束的任务不会污染新窗口的完成数、等待样本和执行样本。

建议阅读顺序：

1. `tasks/prd-concurrency-lab.md`
2. `openspec/changes/bootstrap-concurrency-lab/`
3. `superpowers/workflow.md`
4. `docs/multi-agent-review.md`
5. `docs/architecture.md`

## 版本一范围

已纳入：

- Spring Boot 后端。
- JSON API 和 JSON 指标。
- 动态线程池配置、任务提交、指标、reset。
- Leaf 风格号段 ID，含本地 H2 功能验证。
- 最小订单实验接口。
- k6 脚本和本地 smoke 脚本。
- 可复现报告模板和面试笔记。

不纳入：

- 前端 dashboard。
- 登录、权限、租户。
- 完整订单、支付、库存。
- 服务端自压测 controller。
- 生产观测栈。
- Snowflake。

## 本地基线

```text
JDK: Java 17
Port: 8080
默认功能 profile: local-h2
Leaf 并发证据 profile: mysql-leaf
Health: GET /actuator/health
本地诊断 actuator: health,info,metrics,threaddump
第一次安全压测: 16 VUs / 30s
本地压测上限: 64 VUs / 2min，机器实测前不要越过
```

`application.yml` 已默认启用 `local-h2`。显式指定也可以：

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local-h2
```

## 运行

启动后端：

```powershell
mvn spring-boot:run
```

检查健康状态：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

运行测试：

```powershell
mvn test
```

另开一个 PowerShell 跑线程池 smoke：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-thread-pool.ps1
```

如果已安装 k6，跑安全线程池压测：

```powershell
k6 run .\scripts\k6\thread-pool-safe.js
```

常用 k6 参数：

```powershell
$env:VUS='16'
$env:DURATION='30s'
$env:TASK_TYPE='sleep'
$env:TASK_COUNT='20'
$env:TASK_DURATION_MS='100'
$env:CORE_POOL_SIZE='4'
$env:MAX_POOL_SIZE='8'
$env:QUEUE_CAPACITY='64'
$env:REJECTION_POLICY='ABORT'
k6 run .\scripts\k6\thread-pool-safe.js
```

线程池矩阵脚本会把 k6 summary 写到 `reports/`：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-thread-pool-matrix.ps1
```

Leaf ID 脚本：

```powershell
k6 run .\scripts\k6\leaf-id.js
```

报告目录：

```text
reports/YYYYMMDD-HHmmss-<experiment>/
```

## 当前 API

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

线程池任务响应会返回 `acceptedCount`、`rejectedCount`、`callerRunsCount`、`submittedCount` 和 `requestId`。订单包装响应会返回 `orderId`、`acceptedAsyncTasks`、`rejectedAsyncTasks`、`callerRunsAsyncTasks` 和 `requestId`。

## 验收口径

线程池本地回归至少跑：

```powershell
mvn -Dtest=ThreadPoolLabControllerTest test
```

Leaf ID 本地功能回归至少跑：

```powershell
mvn -Dtest=IdLabControllerTest test
```

全量回归：

```powershell
mvn test
```

MySQL Leaf 并发验收不能用 H2 代替。需要在 `mysql-leaf` profile 下跑并发 64、总 ID 数至少 `step * 3` 的用例或压测，并保存报告。
