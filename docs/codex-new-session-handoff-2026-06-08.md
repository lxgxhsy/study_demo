# Codex 会话交接 - concurrency-lab

本文记录当前已经验证过的基线，方便后续新会话继续工作，不需要恢复原本过大的对话上下文。

## 当前结论

- 工作区：`D:\AI\myProject\concurrency-lab`
- 分支：`main`
- 本轮工作前的基准 commit：`19e3b7c first commit`
- Spring Boot 后端基线已经实现，并已在本地验证。
- 已存在默认禁用的 MySQL Leaf 集成测试，但真实 MySQL 运行证据和真实 k6 压测报告仍未完成。

## 当前工作树已实现内容

动态线程池：

- 运行时配置读取/更新 API。
- Sleep、CPU、mixed 三类任务提交 API。
- 基于 reset 窗口的指标生成。
- `CALLER_RUNS` 单独计为 caller-runs work，不计为 rejected work。
- `completedTaskCount`、等待样本和执行样本都在 `metrics/reset` 后按 generation 隔离。
- 对非法枚举 payload 提供稳定的 `UNSUPPORTED_REJECTION_POLICY` 错误映射。

Leaf ID：

- 号段分配 repository 和内存号段生成器。
- 异步预加载下一号段。
- `preload-wait-timeout-ms` 用来限制等待异步预加载的时间。
- H2 下的功能性并发无重复回归测试。
- MySQL profile 集成测试 `MySqlLeafConcurrencyIT` 已存在，由 `MYSQL_LEAF_IT_ENABLED=true` 控制，覆盖 64 并发号段分配和至少 `step * 3` 个生成 ID。
- ID batch endpoint 的 k6 脚本已存在，但还没有生成真实 k6 报告。

Order lab：

- `POST /api/lab/orders` 会生成 Leaf 风格订单 ID。
- 该接口会向动态线程池提交一个异步 side task。
- 响应包含 accepted、rejected 和 caller-runs 三类异步任务计数。

文档和脚本：

- README 已记录当前 API、本地 profile、smoke 命令和证据边界。
- OpenSpec specs 描述了 caller-runs 指标、reset generation、Leaf preload timeout 和报告要求。
- `scripts/smoke-thread-pool.ps1` 已加入断言。
- `scripts/run-thread-pool-matrix.ps1` 会把 k6 summary 写入 `reports/`。
- `scripts/codex-review-watch.ps1` 是提交后实时审查 watcher。
- `scripts/codex-idle-scan.ps1` 是只读 idle scan 循环，会写入 `target/`。
- `.gitignore` 已排除 watcher 的运行时 pid/state/log 文件。

## 已验证命令

以下命令已在本会话通过：

```powershell
mvn -Dtest=ThreadPoolLabControllerTest test
mvn -Dtest=OrderLabControllerTest test
mvn -Dtest=IdLabControllerTest test
mvn -Dtest=MySqlLeafConcurrencyIT test
mvn test
mvn -DskipTests package
```

`mvn -Dtest=MySqlLeafConcurrencyIT test` 是在未设置 `MYSQL_LEAF_IT_ENABLED` 的情况下运行，结果为 2 个测试 skipped，用来确认默认不会连接 MySQL；它不是 MySQL 并发证据。

HTTP smoke 也已经验证：使用 JDK 17 启动打包后的 jar，然后运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-thread-pool.ps1
```

该 smoke 链路验证了 health、配置更新、ABORT 拒绝行为、ABORT 下 caller-runs 计数为 `0`、指标样本和 reset-scoped 计数器。

## 环境说明

- PATH 上的 `java.exe` 显示为 Java 11。
- Maven 使用 `C:\Users\syxhl\.jdks\ms-17.0.16` 下的 Java 17.0.16。
- 直接 jar smoke 使用 `C:\Users\syxhl\.jdks\ms-17.0.16\bin\java.exe`。
- PATH 上未找到 `k6`，所以还没有生成 k6 报告。
- 打包需要提升权限，因为 Maven 会向 `D:\tools\repository` 写入插件依赖。
- MySQL 集成测试默认禁用，只有设置 `MYSQL_LEAF_IT_ENABLED=true`，并提供 `MYSQL_LEAF_JDBC_URL`、`MYSQL_LEAF_USERNAME`、`MYSQL_LEAF_PASSWORD` 后才会运行。
- 本机可见 MySQL 8.0 客户端和 `MySQL80` 服务，classic MySQL 端口实际监听在 `3300`；`root` 空密码被拒绝，且没有发现 `MYSQL_LEAF_*` 凭据，因此没有运行真实 MySQL IT。

## 监控

本轮不处理监控进程。以下运行时文件可能存在：

```text
docs/review/.codex-review-watch.pid
docs/review/watcher.log
```

运行时文件已被 git 忽略。`docs/review/README.md` 预期需要提交。

idle scan 进程也在 `target/` 下写入文件；这些文件同样被 git 忽略。

## 仍待完成

- 等 watcher 审查新 commit，并检查生成的报告。
- 安装或定位 k6，然后运行真实线程池和 Leaf ID 压测。
- 在 `reports/YYYYMMDD-HHmmss-<experiment>/` 下生成可复现实验报告。
- 在真实 MySQL 下设置 `MYSQL_LEAF_IT_ENABLED=true` 并运行 `mvn -Dtest=MySqlLeafConcurrencyIT test`，然后保存输出作为证据。

## 不要过度宣称

- H2 并发 ID 生成只是一项功能回归测试。
- 它不是 MySQL/InnoDB 号段分配证明。
- 仓库里存在 k6 脚本不等于已经有压测证据；只有真实报告生成后才算证据。
