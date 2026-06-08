param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url
    }

    return Invoke-RestMethod -Method $Method -Uri $Url -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 10)
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$health = Invoke-Json -Method Get -Url "$BaseUrl/actuator/health"
if ($health.status -ne "UP") {
    throw "Health check failed: $($health | ConvertTo-Json -Depth 10)"
}

$config = Invoke-Json -Method Get -Url "$BaseUrl/api/lab/thread-pool/config"
Write-Host "Config:" ($config | ConvertTo-Json -Compress)

$updated = Invoke-Json -Method Put -Url "$BaseUrl/api/lab/thread-pool/config" -Body @{
    corePoolSize = 2
    maximumPoolSize = 4
    queueCapacity = 8
    keepAliveSeconds = 60
    allowCoreThreadTimeOut = $false
    rejectionPolicy = "ABORT"
}
Write-Host "Updated:" ($updated | ConvertTo-Json -Compress)
Assert-True ($updated.corePoolSize -eq 2) "Expected corePoolSize=2 after update."
Assert-True ($updated.maximumPoolSize -eq 4) "Expected maximumPoolSize=4 after update."
Assert-True ($updated.queueCapacity -eq 8) "Expected queueCapacity=8 after update."

[void](Invoke-Json -Method Post -Url "$BaseUrl/api/lab/thread-pool/metrics/reset")

$submitted = Invoke-Json -Method Post -Url "$BaseUrl/api/lab/thread-pool/tasks/sleep" -Body @{
    count = 20
    durationMs = 1000
}
Write-Host "Submitted:" ($submitted | ConvertTo-Json -Compress)
Assert-True ($submitted.submittedCount -eq 20) "Expected submittedCount=20."
Assert-True (($submitted.acceptedCount + $submitted.rejectedCount) -eq 20) "Expected acceptedCount + rejectedCount = 20."
Assert-True ($submitted.rejectedCount -gt 0) "Expected at least one rejected task in small-queue ABORT smoke."
Assert-True ($submitted.callerRunsCount -eq 0) "Expected callerRunsCount=0 for ABORT policy."

Start-Sleep -Milliseconds 3500

$metrics = Invoke-Json -Method Get -Url "$BaseUrl/api/lab/thread-pool/metrics"
Write-Host "Metrics:" ($metrics | ConvertTo-Json -Compress)
Assert-True ($metrics.submittedTaskCount -eq 20) "Expected metrics submittedTaskCount=20."
Assert-True ($metrics.rejectedTaskCount -eq $submitted.rejectedCount) "Expected metrics rejectedTaskCount to match submission response."
Assert-True ($metrics.executionSampleCount -ge $submitted.acceptedCount) "Expected execution samples for accepted tasks."

[void](Invoke-Json -Method Post -Url "$BaseUrl/api/lab/thread-pool/metrics/reset")
$resetMetrics = Invoke-Json -Method Get -Url "$BaseUrl/api/lab/thread-pool/metrics"
Write-Host "ResetMetrics:" ($resetMetrics | ConvertTo-Json -Compress)
Assert-True ($resetMetrics.submittedTaskCount -eq 0) "Expected submittedTaskCount=0 after reset."
Assert-True ($resetMetrics.rejectedTaskCount -eq 0) "Expected rejectedTaskCount=0 after reset."
Assert-True ($resetMetrics.callerRunsTaskCount -eq 0) "Expected callerRunsTaskCount=0 after reset."
Assert-True ($resetMetrics.executionSampleCount -eq 0) "Expected executionSampleCount=0 after reset."

Write-Host "Smoke test completed."
