param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [int]$IntervalSeconds = 60,
    [int]$ReviewTimeoutSeconds = 600,
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

function ConvertTo-ProcessArgumentString {
    param([string[]]$Arguments)

    $quoted = foreach ($argument in $Arguments) {
        if ($null -eq $argument) {
            '""'
        } elseif ($argument -notmatch '[\s"]') {
            $argument
        } else {
            '"' + ($argument -replace '\\', '\\' -replace '"', '\"') + '"'
        }
    }

    return ($quoted -join " ")
}

function Invoke-CodexProcess {
    param(
        [string[]]$Arguments,
        [int]$TimeoutSeconds
    )

    $codexCommand = Get-Command codex -ErrorAction Stop
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $codexCommand.Source
    $startInfo.Arguments = ConvertTo-ProcessArgumentString -Arguments $Arguments
    $startInfo.WorkingDirectory = $Workspace
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo

    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $timeoutMs = [Math]::Max(1, $TimeoutSeconds) * 1000
    $completed = $process.WaitForExit($timeoutMs)

    if (-not $completed) {
        try {
            & taskkill.exe /PID $process.Id /T /F *> $null
        } catch {
            try {
                $process.Kill()
            } catch {
                # Best effort only; the timeout is still reported below.
            }
        }

        return [pscustomobject]@{
            ExitCode = 124
            TimedOut = $true
            StdOut = ""
            StdErr = "codex review timed out after $TimeoutSeconds seconds"
        }
    }

    [void]$stdoutTask.Wait(30000)
    [void]$stderrTask.Wait(30000)

    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        TimedOut = $false
        StdOut = $stdoutTask.Result
        StdErr = $stderrTask.Result
    }
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

function Invoke-CodexCommitReview {
    param([string]$Sha)

    $meta = Get-CommitMetadata -Sha $Sha
    $changedFiles = Get-ChangedFiles -Sha $Sha
    $reviewPath = Get-ReviewPath -Meta $meta

    $header = @"
# Code Review: $($meta.ShortSha) $($meta.Subject)

- Commit: $($meta.Sha)
- Author: $($meta.AuthorName) <$($meta.AuthorEmail)>
- Commit date: $($meta.AuthorDate)
- Reviewed at: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz")
- Mode: codex --search exec review --commit
- Timeout seconds: $ReviewTimeoutSeconds

## Changed Files

~~~text
$changedFiles
~~~

## Review

"@

    Set-Content -Path $reviewPath -Value $header -Encoding UTF8
    Write-WatchLog "review start sha=$Sha path=$reviewPath"

    $codexArgs = @("--search", "exec", "-C", $Workspace, "-s", "read-only", "--ephemeral", "review", "--commit", $Sha)
    $result = $null

    try {
        $result = Invoke-CodexProcess -Arguments $codexArgs -TimeoutSeconds $ReviewTimeoutSeconds
    } catch {
        $result = [pscustomobject]@{
            ExitCode = 1
            TimedOut = $false
            StdOut = ""
            StdErr = "codex review invocation failed: $($_.Exception.Message)"
        }
    }

    $reviewOutput = @()
    if (-not [string]::IsNullOrWhiteSpace($result.StdOut)) {
        $reviewOutput += "### stdout"
        $reviewOutput += ""
        $reviewOutput += "~~~text"
        $reviewOutput += $result.StdOut.TrimEnd()
        $reviewOutput += "~~~"
    }

    if (-not [string]::IsNullOrWhiteSpace($result.StdErr)) {
        $reviewOutput += "### stderr"
        $reviewOutput += ""
        $reviewOutput += "~~~text"
        $reviewOutput += $result.StdErr.TrimEnd()
        $reviewOutput += "~~~"
    }

    if ($reviewOutput.Count -eq 0) {
        $reviewOutput += "(codex review produced no output)"
    }

    Add-Content -Path $reviewPath -Value ($reviewOutput -join "`n") -Encoding UTF8
    Add-Content -Path $reviewPath -Value "`n`n---`nReview command exit code: $($result.ExitCode)`nTimed out: $($result.TimedOut)`n" -Encoding UTF8

    if ($result.ExitCode -ne 0) {
        Write-WatchLog "review failed sha=$Sha exitCode=$($result.ExitCode) timedOut=$($result.TimedOut)"
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
Write-WatchLog "started pid=$PID workspace=$Workspace intervalSeconds=$IntervalSeconds reviewTimeoutSeconds=$ReviewTimeoutSeconds once=$Once reviewCurrentHead=$ReviewCurrentHead"

$script:HadCycleError = $false
try {
    while ($true) {
        try {
            Invoke-ReviewCycle
        } catch {
            $script:HadCycleError = $true
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

if ($Once -and $script:HadCycleError) {
    exit 1
}
