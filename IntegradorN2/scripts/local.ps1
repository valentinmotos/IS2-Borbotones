param(
    [ValidateSet('Start', 'Stop', 'Status')]
    [string]$Action = 'Status'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$serverScript = Join-Path $PSScriptRoot 'serve-local.mjs'
$runtimeDir = Join-Path $projectRoot '.local'
$statePath = Join-Path $runtimeDir 'clients.json'
$ownedProcess = $null

if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
    $candidate = Get-CimInstance Win32_Process -Filter "ProcessId = $($state.pid)" -ErrorAction SilentlyContinue
    # Un PID guardado puede haber sido reutilizado: nunca detener otro proceso.
    if ($candidate -and $candidate.CommandLine -and $candidate.CommandLine.Contains($serverScript)) {
        $ownedProcess = $candidate
    }
}

if ($Action -eq 'Stop') {
    if ($ownedProcess) {
        Stop-Process -Id $ownedProcess.ProcessId
        Write-Output 'Clientes detenidos.'
    } else {
        Write-Output 'No hay un despliegue administrado por este script en ejecucion.'
    }
    return
}

if ($Action -eq 'Status') {
    if ($ownedProcess) {
        Write-Output "Clientes activos (PID $($ownedProcess.ProcessId))."
        Write-Output 'Administracion: http://localhost:5173/'
        Write-Output 'Sitio promocional: http://localhost:5174/'
    } else {
        Write-Output 'Clientes detenidos.'
    }
    return
}

if ($ownedProcess) {
    Write-Output 'Los clientes ya estan iniciados.'
    return
}

foreach ($app in @('client-admin', 'client-public')) {
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot "$app/dist/index.html"))) {
        throw 'Faltan los builds. Ejecutar npm run build antes de iniciar.'
    }
}
foreach ($port in @(5173, 5174)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "El puerto $port esta ocupado. Detener el servidor anterior antes de iniciar."
    }
}

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
$nodePath = (Get-Command node -ErrorAction Stop).Source
$serverProcess = Start-Process -FilePath $nodePath -ArgumentList "`"$serverScript`"" -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeDir 'clients.log') -RedirectStandardError (Join-Path $runtimeDir 'clients-error.log') -PassThru
@{ pid = $serverProcess.Id } | ConvertTo-Json | Set-Content -LiteralPath $statePath

try {
    foreach ($attempt in 1..30) {
        $serverProcess.Refresh()
        if ($serverProcess.HasExited) { throw 'El servidor termino. Revisar .local/clients-error.log.' }
        try {
            $admin = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173/' -TimeoutSec 1
            $public = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5174/' -TimeoutSec 1
            if ($admin.StatusCode -eq 200 -and $public.StatusCode -eq 200) {
                Write-Output 'Administracion: http://localhost:5173/'
                Write-Output 'Sitio promocional: http://localhost:5174/'
                return
            }
        } catch [System.Net.WebException] { }
        Start-Sleep -Milliseconds 200
    }
    throw 'Los clientes no respondieron a tiempo. Revisar los logs de .local/.'
} catch {
    Stop-Process -Id $serverProcess.Id -ErrorAction SilentlyContinue
    throw
}
