param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$services = [ordered]@{ 'service-registry' = 8761; 'config-server' = 8088; 'employee-service' = 8082; 'department-service' = 8083; 'api-gateway' = 8060 }
$java = (Get-Command java -ErrorAction Stop).Source
foreach ($port in $services.Values) {
    $listener = New-Object System.Net.Sockets.TcpClient
    try {
        if ($listener.ConnectAsync('127.0.0.1', $port).Wait(300)) { throw "El puerto $port esta ocupado. Detene el servicio que lo usa antes de iniciar." }
    } catch [System.AggregateException] {
        # Connection refused: el puerto esta libre.
    } finally { $listener.Dispose() }
}
if (-not $SkipBuild) {
    & mvn -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion. Revisa la salida de Maven.' }
}
New-Item -ItemType Directory -Force '.run' | Out-Null
function Wait-Endpoint([string]$Url, [System.Diagnostics.Process]$Process) {
    $deadline = (Get-Date).AddSeconds(180)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) { throw "El proceso $($Process.Id) termino. Revisa .run/*.err.log y .run/*.out.log." }
        try {
            $null = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            return
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "Se agoto el tiempo de espera para $Url. Revisa .run/."
}
try {
    foreach ($name in $services.Keys) {
        $jar = Join-Path $PSScriptRoot "$name/target/$name-0.0.1-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "No existe $jar. Ejecuta sin -SkipBuild." }
        $process = Start-Process -FilePath $java -ArgumentList @('-Xms64m', '-Xmx384m', '-jar', "`"$jar`"", '--spring.profiles.active=local,native', '--server.address=127.0.0.1', '--eureka.instance.prefer-ip-address=true', '--eureka.instance.ip-address=127.0.0.1', '--eureka.instance.lease-renewal-interval-in-seconds=5', '--eureka.client.registry-fetch-interval-seconds=5', '--spring.cloud.loadbalancer.cache.ttl=5s') -WindowStyle Hidden -WorkingDirectory $PSScriptRoot -RedirectStandardOutput "$PSScriptRoot/.run/$name.out.log" -RedirectStandardError "$PSScriptRoot/.run/$name.err.log" -PassThru
        $process.Id | Set-Content ".run/$name.pid"
        $url = "http://127.0.0.1:$($services[$name])"
        if ($name -eq 'service-registry') { $url += '/' }
        elseif ($name -eq 'config-server') { $url += '/employee-service/default' }
        else { $url += '/actuator/health' }
        Wait-Endpoint $url $process
        Write-Host "$name listo en puerto $($services[$name])"
    }
    Write-Host 'Gateway: http://localhost:8060 | Eureka: http://localhost:8761'
    Write-Host 'Probar: .\probar.ps1 | Detener: .\detener.ps1'
} catch {
    & "$PSScriptRoot/detener.ps1"
    throw
}
