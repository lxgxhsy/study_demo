param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [int]$IntervalSeconds = 60,
    [switch]$Once,
    [switch]$ReviewCurrentHead
)

$ErrorActionPreference = "Stop"

$Workspace = (Resolve-Path $Workspace).Path
$reviewDir = Join-Path $Workspace "docs\review"
$statePath = Join-Path $reviewDir ".last-reviewed-head"
$pidPath = Join-Path $reviewDir ".codex-review-watch.pid"
$stopPath = Join-Path $reviewDir ".codex-review-watch.stop"
$logPath = Join-Path $reviewDir "watcher.log"

New-Item -ItemType Directory -Force -Path $reviewDir | Out-Null

function Write-WatchLog {
    param([string]$Message)

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $logPath -Value "[$timestamp] $Message" -Encoding UTF8
}

function Invoke-GitText {
    param([string[]]$GitArgs)

    Push-Location $Workspace
    try {
        $output = & git @GitArgs 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "git $($GitArgs -join ' ') failed: $($output -join "`n")"
        }
        return ($output -join "`n").Trim()
    } finally {
        Pop-Location
    }
}

function Test-GitSuccess {
    param([string[]]$GitArgs)

    Push-Location $Workspace
    try {
        & git @GitArgs *> $null
        return ($LASTEXITCODE -eq 0)
    } finally {
        Pop-Location
    }
}

function Get-CurrentHead {
    return Invoke-GitText -GitArgs @("rev-parse", "HEAD")
}

function Get-CommitMetadata {
    param([string]$Sha)

    $format = "%H%n%h%n%an%n%ae%n%ad%n%s"
    $text = Invoke-GitText -GitArgs @("show", "-s", "--date=iso-strict", "--format=$format", $Sha)
    $lines = @($text -split "`r?`n")

    return [pscustomobject]@{
        Sha = $lines[0]
        ShortSha = $lines[1]
        AuthorName = $lines[2]
        AuthorEmail = $lines[3]
        AuthorDate = $lines[4]
        Subject = $lines[5]
    }
}

function ConvertTo-AsciiSlug {
    param([string]$Text)

    $slug = $Text.ToLowerInvariant()
    $slug = [regex]::Replace($slug, "[^a-z0-9]+", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($slug)) {
        $slug = "commit"
    }
    if ($slug.Length -gt 48) {
        $slug = $slug.Substring(0, 48).Trim("-")
    }
    return $slug
}

function Get-ReviewPath {
    param([object]$Meta)

    try {
        $commitStamp = ([DateTimeOffset]::Parse($Meta.AuthorDate)).ToString("yyyyMMdd-HHmmss")
    } catch {
        $commitStamp = Get-Date -Format "yyyyMMdd-HHmmss"
    }

    $slug = ConvertTo-AsciiSlug -Text $Meta.Subject
    return Join-Path $reviewDir "$commitStamp-$($Meta.ShortSha)-$slug.md"
}

function Get-ChangedFiles {
    param([string]$Sha)

    $files = Invoke-GitText -GitArgs @("diff-tree", "--root", "--no-commit-id", "--name-status", "-r", $Sha)
    if ([string]::IsNullOrWhiteSpace($files)) {
        return "(no file changes reported by git diff-tree)"
    }
    return $files
}

function Get-PendingCommits {
    $head = Get-CurrentHead

    if (-not (Test-Path $statePath)) {
        if ($ReviewCurrentHead) {
            return @($head)
        }

        Set-Content -Path $statePath -Value $head -Encoding ASCII
        Write-WatchLog "initialized baseline head=$head; no review generated"
        return @()
    }

    $lastReviewed = (Get-Content -Path $statePath -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($lastReviewed)) {
        Set-Content -Path $statePath -Value $head -Encoding ASCII
        Write-WatchLog "state was empty; reset baseline head=$head"
        return @()
    }

    if ($lastReviewed -eq $head) {
        return @()
    }

    if (-not (Test-GitSuccess -GitArgs @("cat-file", "-e", "$lastReviewed^{commit}"))) {
        Write-WatchLog "last reviewed commit $lastReviewed no longer exists; reviewing current head=$head"
        return @($head)
    }

    if (-not (Test-GitSuccess -GitArgs @("merge-base", "--is-ancestor", $lastReviewed, $head))) {
        Write-WatchLog "last reviewed commit $lastReviewed is not an ancestor of head=$head; reviewing current head only"
        return @($head)
    }

    $text = Invoke-GitText -GitArgs @("rev-list", "--reverse", "$lastReviewed..$head")
    if ([string]::IsNullOrWhiteSpace($text)) {
        return @()
    }

    return @($text -split "`r?`n")
}

function New-ReviewPrompt {
    param(
        [object]$Meta,
        [string]$ChangedFiles
    )

@"
你是这个仓库的实时代码评审员。请审查 commit $($Meta.Sha)，只输出审查报告，不要修改仓库文件。

硬性流程：
1. 必须先使用 web search 检索最新资料，再评审。优先官方文档、JDK/Spring Boot 文档、成熟开源项目、GitHub 上多数派实现和近期工程实践。报告中列出参考 URL；如果搜索证据弱，也要写清楚搜索方向和不确定性。
2. 你要有自己的工程品味，不做顺从式总结。GitHub 多数派是证据，不是盲从；如果本项目的取舍更好，可以明确维护当前做法。
3. 重点去掉代码骨架里的 vibe coding 味道：空泛脚手架、为了像项目而堆层、缺少业务/并发不变量、命名泛化、魔法数、DTO 贫血但契约不清、异常边界不实、测试只覆盖 happy path、README 与代码事实脱节、看起来像生成但没有工程约束的代码。
4. 这个项目是 Java 17 + Spring Boot 3.x 的 concurrency lab。评审时特别关注动态线程池、拒绝策略、队列容量调整、指标统计、Leaf-style segment ID、并发安全、压测脚本和测试可信度。
5. Findings 必须按严重程度排序。只有真实会导致错误、回归、维护性明显下降或背离主流工程规则的问题才列为需要修改；不要输出泛泛建议。

输出格式：
- 结论：阻塞 / 需要修改 / 可接受。
- Findings：每条包含 文件:行号、问题、为什么与官方/多数派/本项目边界冲突、建议改法。
- Vibe Coding 检查：指出是否有脚手架味，哪些可以删、收紧或改成真实工程契约。
- Web Evidence：列出 URL 和对应判断依据。
- 测试与验证：说明现有测试缺口和最小验证命令。

Commit metadata:
- SHA: $($Meta.Sha)
- Author: $($Meta.AuthorName) <$($Meta.AuthorEmail)>
- Date: $($Meta.AuthorDate)
- Subject: $($Meta.Subject)

Changed files:
```text
$ChangedFiles
```
"@
}

function Invoke-CodexCommitReview {
    param([string]$Sha)

    $meta = Get-CommitMetadata -Sha $Sha
    $changedFiles = Get-ChangedFiles -Sha $Sha
    $reviewPath = Get-ReviewPath -Meta $meta
    $prompt = New-ReviewPrompt -Meta $meta -ChangedFiles $changedFiles

    $header = @"
# Code Review: $($meta.ShortSha) $($meta.Subject)

- Commit: $($meta.Sha)
- Author: $($meta.AuthorName) <$($meta.AuthorEmail)>
- Commit date: $($meta.AuthorDate)
- Reviewed at: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz")
- Mode: codex review --commit with live web search

## Changed Files

```text
$changedFiles
```

## Review

"@

    Set-Content -Path $reviewPath -Value $header -Encoding UTF8
    Write-WatchLog "review start sha=$Sha path=$reviewPath"

    $codexArgs = @("--search", "-C", $Workspace, "-s", "read-only", "-a", "never", "review", "--commit", $Sha, "-")
    $reviewOutput = $null
    $exitCode = 0

    try {
        $reviewOutput = $prompt | & codex @codexArgs 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        $exitCode = 1
        $reviewOutput = @("codex review invocation failed: $($_.Exception.Message)")
    }

    if ($null -eq $reviewOutput -or $reviewOutput.Count -eq 0) {
        $reviewOutput = @("(codex review produced no output)")
    }

    Add-Content -Path $reviewPath -Value ($reviewOutput -join "`n") -Encoding UTF8
    Add-Content -Path $reviewPath -Value "`n`n---`nReview command exit code: $exitCode`n" -Encoding UTF8

    if ($exitCode -ne 0) {
        Write-WatchLog "review failed sha=$Sha exitCode=$exitCode"
        throw "codex review failed for $Sha; see $reviewPath"
    }

    Set-Content -Path $statePath -Value $Sha -Encoding ASCII
    Write-WatchLog "review complete sha=$Sha path=$reviewPath"
}

function Invoke-ReviewCycle {
    $commits = @(Get-PendingCommits)
    if ($commits.Count -eq 0) {
        Write-WatchLog "no new commits"
        return
    }

    foreach ($commit in $commits) {
        if ([string]::IsNullOrWhiteSpace($commit)) {
            continue
        }
        Invoke-CodexCommitReview -Sha $commit.Trim()
    }
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git is not installed or not on PATH."
}

if (-not (Get-Command codex -ErrorAction SilentlyContinue)) {
    throw "codex is not installed or not on PATH."
}

if (Test-Path $pidPath) {
    $existingPid = (Get-Content -Path $pidPath -Raw).Trim()
    if ($existingPid -match "^\d+$") {
        $existingProcess = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
        if ($null -ne $existingProcess) {
            throw "codex review watcher is already running with PID $existingPid. Stop it by creating $stopPath."
        }
    }
}

if (Test-Path $stopPath) {
    Remove-Item -Path $stopPath -Force
}

Set-Content -Path $pidPath -Value $PID -Encoding ASCII
Write-WatchLog "started pid=$PID workspace=$Workspace intervalSeconds=$IntervalSeconds once=$Once reviewCurrentHead=$ReviewCurrentHead"

try {
    while ($true) {
        try {
            Invoke-ReviewCycle
        } catch {
            Write-WatchLog "cycle error: $($_.Exception.Message)"
        }

        if ($Once) {
            break
        }

        $remaining = $IntervalSeconds
        while ($remaining -gt 0) {
            if (Test-Path $stopPath) {
                break
            }

            $sleepSeconds = [Math]::Min(5, $remaining)
            Start-Sleep -Seconds $sleepSeconds
            $remaining -= $sleepSeconds
        }

        if (Test-Path $stopPath) {
            Write-WatchLog "stopped by stop file"
            break
        }
    }
} finally {
    if (Test-Path $pidPath) {
        Remove-Item -Path $pidPath -Force
    }
    Write-WatchLog "stopped pid=$PID"
}
