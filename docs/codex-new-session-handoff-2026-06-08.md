# Codex Session Handoff - concurrency-lab

This file captures the current verified baseline so a later session can continue without resuming the oversized original conversation.

## Current Verdict

- Workspace: `D:\AI\myProject\concurrency-lab`
- Branch: `main`
- Base commit before this work: `19e3b7c first commit`
- The Spring Boot backend baseline is implemented and verified locally.
- Real MySQL concurrency evidence and real k6 pressure-test reports are still pending.

## Implemented In This Working Tree

Dynamic thread pool:

- Runtime config read/update APIs.
- Sleep, CPU, and mixed task submission APIs.
- Reset-scoped metrics generation.
- `CALLER_RUNS` counted separately as caller-runs work, not rejected work.
- `completedTaskCount`, wait samples, and execution samples are generation-scoped after `metrics/reset`.
- Stable `UNSUPPORTED_REJECTION_POLICY` error mapping for invalid enum payloads.

Leaf ID:

- Segment allocation repository and in-memory segment generator.
- Async next-segment preload.
- `preload-wait-timeout-ms` bound for waiting on async preload.
- H2 functional concurrent no-duplicate regression test.
- k6 script for ID batch endpoint exists, but no real k6 report has been produced.

Order lab:

- `POST /api/lab/orders` generates a Leaf-style order ID.
- The endpoint submits one async side task to the dynamic thread pool.
- Response includes accepted, rejected, and caller-runs async task counts.

Docs and scripts:

- README now documents current API, local profile, smoke commands, and evidence boundary.
- OpenSpec specs describe caller-runs metrics, reset generation, Leaf preload timeout, and report expectations.
- `scripts/smoke-thread-pool.ps1` now uses assertions.
- `scripts/run-thread-pool-matrix.ps1` writes k6 summaries under `reports/`.
- `scripts/codex-review-watch.ps1` is the live post-commit review watcher.
- `scripts/codex-idle-scan.ps1` is a read-only idle scan loop that writes under `target/`.
- `.gitignore` excludes watcher runtime pid/state/log files.

## Verified Commands

These commands passed in this session:

```powershell
mvn -Dtest=ThreadPoolLabControllerTest test
mvn -Dtest=OrderLabControllerTest test
mvn -Dtest=IdLabControllerTest test
mvn test
mvn -DskipTests package
```

HTTP smoke was also verified by starting the packaged jar with JDK 17 and running:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-thread-pool.ps1
```

The smoke path verified health, config update, ABORT rejection behavior, caller-runs count `0` for ABORT, metrics samples, and reset-scoped counters.

## Environment Notes

- `java.exe` on PATH reports Java 11.
- Maven uses Java 17.0.16 from `C:\Users\syxhl\.jdks\ms-17.0.16`.
- Direct jar smoke used `C:\Users\syxhl\.jdks\ms-17.0.16\bin\java.exe`.
- `k6` was not found on PATH, so no k6 report was generated.
- Packaging needed elevated access because Maven wrote plugin dependencies under `D:\tools\repository`.

## Monitoring

The live review watcher is running:

```text
docs/review/.codex-review-watch.pid
docs/review/watcher.log
```

Runtime files are ignored by git. `docs/review/README.md` is intended to be committed.

The idle scan process is also writing under `target/`; those files are ignored by git.

## Still Pending

- Let the watcher review the new commit and inspect the generated report.
- Install or locate k6, then run real thread-pool and Leaf ID pressure tests.
- Produce reproducible reports under `reports/YYYYMMDD-HHmmss-<experiment>/`.
- Add MySQL-profile Leaf concurrency evidence with concurrency 64 and total IDs at least `step * 3`.

## Do Not Overclaim

- H2 concurrent ID generation is a functional regression only.
- It is not MySQL/InnoDB segment allocation proof.
- k6 scripts existing in the repo are not pressure-test evidence until reports are actually generated.
