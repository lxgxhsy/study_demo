# Live Code Reviews

This directory is written by `scripts/codex-review-watch.ps1`.

The watcher checks Git `HEAD` every 60 seconds. When it sees new commits, it runs:

```powershell
codex --search review --commit <sha>
```

Each report is saved as a Markdown file in this directory. The prompt requires live web search, official or mainstream GitHub evidence, and a direct check for scaffold-like "vibe coding" artifacts.

State files:

- `.last-reviewed-head`: last commit successfully reviewed.
- `.codex-review-watch.pid`: running watcher process id.
- `.codex-review-watch.stop`: create this file to stop the watcher.
- `watcher.log`: watcher activity log.

Manual start:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex-review-watch.ps1
```

Stop:

```powershell
New-Item -ItemType File -Force .\docs\review\.codex-review-watch.stop
```
