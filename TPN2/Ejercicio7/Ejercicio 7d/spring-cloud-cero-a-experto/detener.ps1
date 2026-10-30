$ErrorActionPreference = 'Stop'
foreach ($name in @('api-gateway', 'department-service', 'employee-service', 'config-server', 'service-registry')) {
    $pidFile = Join-Path $PSScriptRoot ".run/$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $servicePid = [int](Get-Content -LiteralPath $pidFile)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $servicePid" -ErrorAction SilentlyContinue
    $expectedJar = Join-Path $PSScriptRoot "$name/target/$name-0.0.1-SNAPSHOT.jar"
    if ($process -and $process.CommandLine.Contains($expectedJar)) {
        Stop-Process -Id $servicePid -ErrorAction SilentlyContinue
        Write-Host "$name detenido"
    }
    Remove-Item -LiteralPath $pidFile
}
