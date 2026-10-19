$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path '.runtime' | Out-Null
$backendDir = Join-Path $PSScriptRoot 'backend'
$uiDir = Join-Path $PSScriptRoot 'UI'
foreach ($port in 8080,3000) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "El puerto $port ya esta ocupado. Detene el proceso anterior." }
}
if (!(Test-Path -LiteralPath 'UI/node_modules/react-scripts/bin/react-scripts.js')) {
    Push-Location -LiteralPath $uiDir
    try { & npm.cmd ci --no-audit --no-fund; if ($LASTEXITCODE -ne 0) { throw 'Fallo npm ci' } } finally { Pop-Location }
}
Push-Location -LiteralPath $backendDir
try { & mvn.cmd -B package; if ($LASTEXITCODE -ne 0) { throw 'Fallo Maven' } } finally { Pop-Location }
$env:BROWSER = 'none'
$env:HOST = '127.0.0.1'
$backend = Start-Process -FilePath 'java' -ArgumentList '-jar','target/shopease-0.0.1-SNAPSHOT.jar' -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $PSScriptRoot '.runtime/backend.log') -RedirectStandardError (Join-Path $PSScriptRoot '.runtime/backend-error.log')
$ui = Start-Process -FilePath 'node' -ArgumentList 'node_modules/react-scripts/bin/react-scripts.js','start' -WorkingDirectory $uiDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $PSScriptRoot '.runtime/ui.log') -RedirectStandardError (Join-Path $PSScriptRoot '.runtime/ui-error.log')
@{ backend = $backend.Id; ui = $ui.Id } | ConvertTo-Json | Set-Content -LiteralPath '.runtime/processes.json'
Write-Host 'ShopEase iniciando en http://localhost:3000; API http://localhost:8080. Logs en .runtime'
