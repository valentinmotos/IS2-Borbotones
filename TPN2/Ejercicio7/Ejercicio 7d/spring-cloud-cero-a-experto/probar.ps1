$ErrorActionPreference = 'Stop'
$gateway = 'http://127.0.0.1:8060/api/v1'
$deadline = (Get-Date).AddSeconds(120)
do {
    try {
        $null = Invoke-RestMethod "$gateway/employees" -TimeoutSec 5
        break
    } catch {
        if ((Get-Date) -gt $deadline) { throw 'El gateway no encuentra employee-service. Revisa Eureka y los logs.' }
        Start-Sleep -Seconds 3
    }
} while ($true)
$id = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$department = @{ id = $id; name = 'Sistemas' } | ConvertTo-Json
$employee = @{ id = $id; departmentId = $id; name = 'Ana'; position = 'Developer' } | ConvertTo-Json
$null = Invoke-RestMethod "$gateway/departments" -Method Post -ContentType 'application/json' -Body $department
$null = Invoke-RestMethod "$gateway/employees" -Method Post -ContentType 'application/json' -Body $employee
$result = Invoke-RestMethod "$gateway/departments/$id"
if ($result.name -ne 'Sistemas' -or @($result.employees).Count -ne 1 -or $result.employees[0].name -ne 'Ana') {
    throw 'Fallo la consulta departamento -> empleados a traves del gateway.'
}
$message = Invoke-RestMethod "$gateway/departments/assistant/hello"
if ($message -ne 'Hi') { throw 'No se cargo la configuracion centralizada.' }
$result | ConvertTo-Json -Depth 5
Write-Host 'OK: altas, gateway, descubrimiento, comunicacion Feign y configuracion centralizada.'
