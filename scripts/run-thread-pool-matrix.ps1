param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ReportsRoot = "reports",
    [string[]]$Cases = @("baseline-abort", "small-queue-abort", "small-queue-caller-runs", "cpu-abort", "mixed-abort")
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    throw "k6 is not installed or not on PATH."
}

function Invoke-K6Case {
    param(
        [string]$CaseName,
        [string]$Script,
        [hashtable]$Env
    )

    $ts = Get-Date -Format "yyyyMMdd-HHmmss"
    $reportDir = Join-Path $ReportsRoot "$ts-$CaseName"
    New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

    $summaryPath = Join-Path $reportDir "summary.json"
    $outputPath = Join-Path $reportDir "k6-output.txt"
    $commandPath = Join-Path $reportDir "command.txt"

    $previous = @{}
    foreach ($key in $Env.Keys) {
        $previous[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        [Environment]::SetEnvironmentVariable($key, [string]$Env[$key], "Process")
    }

    [Environment]::SetEnvironmentVariable("BASE_URL", $BaseUrl, "Process")
    "k6 run --summary-export `"$summaryPath`" `"$Script`"" | Set-Content -Path $commandPath

    try {
        & k6 run --summary-export $summaryPath $Script *>&1 | Tee-Object -FilePath $outputPath
    } finally {
        foreach ($key in $Env.Keys) {
            [Environment]::SetEnvironmentVariable($key, $previous[$key], "Process")
        }
        [Environment]::SetEnvironmentVariable("BASE_URL", $null, "Process")
    }
}

$threadPoolScript = ".\scripts\k6\thread-pool-safe.js"

foreach ($case in $Cases) {
    switch ($case) {
        "baseline-abort" {
            Invoke-K6Case -CaseName $case -Script $threadPoolScript -Env @{
                VUS = "16"; DURATION = "30s"; TASK_TYPE = "sleep"; TASK_COUNT = "20"; TASK_DURATION_MS = "100";
                CORE_POOL_SIZE = "4"; MAX_POOL_SIZE = "8"; QUEUE_CAPACITY = "64"; REJECTION_POLICY = "ABORT"
            }
        }
        "small-queue-abort" {
            Invoke-K6Case -CaseName $case -Script $threadPoolScript -Env @{
                VUS = "16"; DURATION = "30s"; TASK_TYPE = "sleep"; TASK_COUNT = "20"; TASK_DURATION_MS = "100";
                CORE_POOL_SIZE = "2"; MAX_POOL_SIZE = "4"; QUEUE_CAPACITY = "8"; REJECTION_POLICY = "ABORT"
            }
        }
        "small-queue-caller-runs" {
            Invoke-K6Case -CaseName $case -Script $threadPoolScript -Env @{
                VUS = "16"; DURATION = "30s"; TASK_TYPE = "sleep"; TASK_COUNT = "20"; TASK_DURATION_MS = "100";
                CORE_POOL_SIZE = "2"; MAX_POOL_SIZE = "4"; QUEUE_CAPACITY = "8"; REJECTION_POLICY = "CALLER_RUNS"
            }
        }
        "cpu-abort" {
            Invoke-K6Case -CaseName $case -Script $threadPoolScript -Env @{
                VUS = "16"; DURATION = "30s"; TASK_TYPE = "cpu"; TASK_COUNT = "10"; TASK_ITERATIONS = "100000";
                CORE_POOL_SIZE = "4"; MAX_POOL_SIZE = "8"; QUEUE_CAPACITY = "64"; REJECTION_POLICY = "ABORT"
            }
        }
        "mixed-abort" {
            Invoke-K6Case -CaseName $case -Script $threadPoolScript -Env @{
                VUS = "16"; DURATION = "30s"; TASK_TYPE = "mixed"; TASK_COUNT = "10"; TASK_DURATION_MS = "50"; TASK_ITERATIONS = "50000";
                CORE_POOL_SIZE = "4"; MAX_POOL_SIZE = "8"; QUEUE_CAPACITY = "64"; REJECTION_POLICY = "ABORT"
            }
        }
        default {
            throw "Unknown matrix case: $case"
        }
    }
}
