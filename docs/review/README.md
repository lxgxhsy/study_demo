# 实时代码审查

## 背景

这个仓库是一个后端-only 的高并发下单实验项目，核心内容包括动态线程池、Leaf 风格号段 ID、最小订单包装接口、k6 压测脚本和可复现实验报告。

`docs/review/` 不属于业务模块，也不属于压测报告目录。它用于保存提交后的 Codex 只读审查结果，帮助在实现动态线程池、Leaf ID、订单实验接口或脚本变更后，及时发现回归风险、遗漏测试和文档不一致。

审查结果只能作为辅助证据。真正的功能验收仍以 `mvn test`、指定测试类、smoke 脚本和实际 k6/MySQL 报告为准。

## 工作方式

本目录由 `scripts/codex-review-watch.ps1` 写入。

watcher 默认每 60 秒检查一次 Git `HEAD`。当它发现新的 commit 后，会运行：

```powershell
codex --search exec -C <workspace> -s read-only --ephemeral review --commit <sha>
```

每次审查都会在本目录生成一个 Markdown 报告文件。文件名包含 commit 时间、短 SHA 和 commit subject 的 slug。

Codex CLI 0.137.0 中，`codex review --commit` 不能同时接收自定义 stdin prompt，所以 watcher 使用 CLI 内置的 commit review 流程，并把 stdout、stderr 和命令退出码一起写入报告。

## 状态文件

- `.last-reviewed-head`：最后一次成功审查的 commit。
- `.codex-review-watch.pid`：当前 watcher 进程 ID。
- `.codex-review-watch.stop`：创建这个文件即可要求 watcher 停止。
- `watcher.log`：watcher 活动日志。

这些运行时文件不应该作为审查结论使用。需要看审查内容时，打开本目录下按 commit 生成的 Markdown 报告。

## 执行

手动启动 watcher：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex-review-watch.ps1
```

只检查一次，并把单次审查超时缩短到 120 秒：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex-review-watch.ps1 -Once -ReviewTimeoutSeconds 120
```

停止 watcher：

```powershell
New-Item -ItemType File -Force .\docs\review\.codex-review-watch.stop
```

如果已有 watcher 在运行，脚本会读取 `.codex-review-watch.pid` 并拒绝重复启动。需要先停止旧进程，再重新启动。

## 使用建议

提交业务代码、压测脚本或文档后，可以让 watcher 自动审查新 commit。审查报告重点看三类内容：

- 是否存在行为回归或异常路径遗漏。
- 是否缺少对应测试、smoke 或压测证据。
- README、OpenSpec、报告模板和实际实现是否不一致。

对于本项目，尤其不要把 H2 下的 Leaf ID 功能测试误写成 MySQL/InnoDB 并发证明，也不要把 k6 脚本存在误写成已经产生压测证据。真实证据需要落到 `reports/YYYYMMDD-HHmmss-<experiment>/`。
