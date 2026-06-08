# Live Code Reviews

This directory is written by `scripts/codex-review-watch.ps1`.

The watcher checks Git `HEAD` every 60 seconds. When it sees new commits, it runs:

```powershell
codex --search exec -C <workspace> -s read-only --ephemeral review --commit <sha>
```

Each report is saved as a Markdown file in this directory. `codex review --commit` in Codex CLI 0.137.0 does not allow a custom stdin prompt together with `--commit`, so the watcher uses the CLI's built-in commit review flow and records stdout/stderr plus the command exit code.

State files:

- `.last-reviewed-head`: last commit successfully reviewed.
- `.codex-review-watch.pid`: running watcher process id.
- `.codex-review-watch.stop`: create this file to stop the watcher.
- `watcher.log`: watcher activity log.

Manual start:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex-review-watch.ps1
```

One-shot check with a shorter review timeout:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex-review-watch.ps1 -Once -ReviewTimeoutSeconds 120
```

Stop:

```powershell
New-Item -ItemType File -Force .\docs\review\.codex-review-watch.stop
```
