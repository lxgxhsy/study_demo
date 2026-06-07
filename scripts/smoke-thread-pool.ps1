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

$submitted = Invoke-Json -Method Post -Url "$BaseUrl/api/lab/thread-pool/tasks/sleep" -Body @{
    count = 20
    durationMs = 100
}
Write-Host "Submitted:" ($submitted | ConvertTo-Json -Compress)

$metrics = Invoke-Json -Method Get -Url "$BaseUrl/api/lab/thread-pool/metrics"
Write-Host "Metrics:" ($metrics | ConvertTo-Json -Compress)

Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/lab/thread-pool/metrics/reset" | Out-Null
$resetMetrics = Invoke-Json -Method Get -Url "$BaseUrl/api/lab/thread-pool/metrics"
Write-Host "ResetMetrics:" ($resetMetrics | ConvertTo-Json -Compress)

Write-Host "Smoke test completed."
