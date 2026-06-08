param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [int]$IntervalSeconds = 60,
    [int]$IdleThresholdSeconds = 60
)

$ErrorActionPreference = "Stop"

$targetDir = Join-Path $Workspace "target"
$logPath = Join-Path $targetDir "codex-idle-scan.log"
$stopPath = Join-Path $targetDir "codex-idle-scan.stop"
$heartbeatPath = Join-Path $targetDir "codex-active-work.heartbeat"

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

function Write-ScanLog {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $logPath -Value "[$timestamp] $Message"
}

function Get-IdleSeconds {
    if (-not (Test-Path $heartbeatPath)) {
        return [double]::PositiveInfinity
    }
    $heartbeat = Get-Item $heartbeatPath
    return ((Get-Date) - $heartbeat.LastWriteTime).TotalSeconds
}

function Invoke-ReadOnlyScan {
    Push-Location $Workspace
    try {
        $gitStatus = git status --short 2>&1
        $todoCount = (rg -n "TODO|FIXME|HACK" . --glob "!target/**" 2>$null | Measure-Object).Count
        $uncheckedTasks = (rg -n "^- \[ \]" openspec docs tasks --glob "!target/**" 2>$null | Measure-Object).Count
        $javaFiles = (rg --files src/main/java src/test/java 2>$null | Measure-Object).Count

        Write-ScanLog ("idle-scan status: dirtyLines={0}; todoMarkers={1}; uncheckedTasks={2}; javaFiles={3}" -f `
                (($gitStatus | Measure-Object).Count), $todoCount, $uncheckedTasks, $javaFiles)
    } catch {
        Write-ScanLog ("idle-scan error: {0}" -f $_.Exception.Message)
    } finally {
        Pop-Location
    }
}

Write-ScanLog ("started workspace={0}; intervalSeconds={1}; idleThresholdSeconds={2}" -f `
        $Workspace, $IntervalSeconds, $IdleThresholdSeconds)

while (-not (Test-Path $stopPath)) {
    $idleSeconds = Get-IdleSeconds
    if ($idleSeconds -ge $IdleThresholdSeconds) {
        Invoke-ReadOnlyScan
    } else {
        Write-ScanLog ("skip active-work idleSeconds={0:N1}" -f $idleSeconds)
    }

    Start-Sleep -Seconds $IntervalSeconds
}

Write-ScanLog "stopped by stop file"
