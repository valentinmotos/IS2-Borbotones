param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$api = $BaseUrl.TrimEnd('/') + '/api'

function Solicitar {
    param([string]$Metodo, [string]$Ruta, $Datos = $null, [hashtable]$Cabeceras = @{})
    $parametros = @{ Uri = "$api$Ruta"; Method = $Metodo; Headers = $Cabeceras }
    if ($null -ne $Datos) {
        $parametros.ContentType = 'application/json; charset=utf-8'
        $parametros.Body = [System.Text.Encoding]::UTF8.GetBytes(($Datos | ConvertTo-Json -Depth 5 -Compress))
    }
    $resultado = Invoke-RestMethod @parametros
    return $resultado
}
function Credenciales {
    param([string]$Mail, [string]$Clave)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("${Mail}:${Clave}")
    @{ Authorization = 'Basic ' + [Convert]::ToBase64String($bytes) }
}

$sufijo = [Guid]::NewGuid().ToString('N')
$clave = 'mascotas123'
$mailAna = "ana.$sufijo@prueba.test"
$mailJuan = "juan.$sufijo@prueba.test"
$zona = Solicitar POST '/zonas' @{ nombre = "Centro $sufijo"; descripcion = 'Prueba REST' }
$ana = Solicitar POST '/usuarios' @{ nombre = 'Ana'; apellido = 'Perez'; mail = $mailAna; clave = $clave; clave2 = $clave; zonaId = $zona.id }
$juan = Solicitar POST '/usuarios' @{ nombre = 'Juan'; apellido = 'Lopez'; mail = $mailJuan; clave = $clave; clave2 = $clave; zonaId = $zona.id }
$login = Solicitar POST '/auth/login' @{ mail = $mailAna; clave = $clave }
if ($login.id -ne $ana.id) { throw 'Fallo el login' }
$authAna = Credenciales $mailAna $clave
$authJuan = Credenciales $mailJuan $clave
$luna = Solicitar POST '/mascotas' @{ nombre = 'Luna'; sexo = 'HEMBRA'; tipo = 'PERRO' } $authAna
$toby = Solicitar POST '/mascotas' @{ nombre = 'Toby'; sexo = 'MACHO'; tipo = 'PERRO' } $authJuan
$candidatos = @(Solicitar GET "/mascotas/$($luna.id)/candidatos" $null $authAna)
if ($candidatos.Count -ne 1 -or $candidatos[0].id -ne $toby.id) { throw 'Fallo la exploracion' }
$voto = Solicitar POST '/votos' @{ mascota1Id = $luna.id; mascota2Id = $toby.id } $authAna
$recibidos = @(Solicitar GET '/votos/recibidos' $null $authJuan)
if ($recibidos.Count -ne 1 -or $recibidos[0].id -ne $voto.id) { throw 'Fallo la consulta de votos' }
$respuesta = Solicitar PUT "/votos/$($voto.id)/respuesta" $null $authJuan
if (-not $respuesta.match) { throw 'Fallo la respuesta al voto' }
$matches = @(Solicitar GET '/matches' $null $authAna)
if ($matches.Count -ne 1 -or $matches[0].id -ne $voto.id) { throw 'Fallo la consulta de matches' }
$editada = Solicitar PUT "/mascotas/$($luna.id)" @{ nombre = 'Lunita'; sexo = 'HEMBRA'; tipo = 'PERRO' } $authAna
if ($editada.nombre -ne 'Lunita') { throw 'Fallo la modificacion' }
Solicitar DELETE "/mascotas/$($luna.id)" $null $authAna | Out-Null
$propias = @(Solicitar GET '/mascotas' $null $authAna)
if ($propias.Count -ne 0) { throw 'Fallo la baja logica' }
Write-Host 'PRUEBA REST OK: registro, login, mascotas, candidatos, votos, match, edicion y baja.'
Write-Host "API: $api"
Write-Host "Match creado: $($voto.id)"
