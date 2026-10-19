# Contrato de la API REST

Base por defecto: `http://localhost:8080/api`. Todas las operaciones requieren HTTP Basic. Usuario de ejemplo: `demo`, clave: `Consultorio123!`.

Las solicitudes JSON se envían con `Content-Type: application/json`. Los IDs son UUID de texto. `eliminado` e `id` son controlados por el servidor: no se aceptan entidades completas como solicitudes de alta/edición.

## Recursos

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/pacientes`, `/medicos`, `/usuarios`, `/historias` | 200, lista de registros activos |
| GET | `/pacientes/{id}`, `/medicos/{id}`, `/usuarios/{id}` | 200, registro activo |
| POST | `/pacientes`, `/medicos`, `/usuarios`, `/historias` | 201, registro creado y encabezado `Location` |
| PUT | `/pacientes/{id}`, `/medicos/{id}`, `/usuarios/{id}`, `/historias/{id}` | 200, registro modificado |
| DELETE | `/pacientes/{id}`, `/medicos/{id}`, `/usuarios/{id}`, `/historias/{id}` | 204, baja lógica |
| GET | `/historias/{id}` | 200, expediente compuesto por la fachada |
| GET | `/historias/{id}/detalles` | 200, atenciones activas, fecha descendente |
| GET | `/historias/{id}/detalles/{detalleId}` | 200, atención que pertenece a esa historia |
| POST | `/historias/{id}/detalles` | 201, atención creada y `Location` |
| PUT | `/historias/{id}/detalles/{detalleId}` | 200, atención modificada |
| DELETE | `/historias/{id}/detalles/{detalleId}` | 204, baja lógica de atención |
| POST | `/pacientes/{id}/foto` | 201, metadata de la foto, reemplaza la anterior |
| GET | `/pacientes/{id}/foto` | 200, bytes PNG/JPEG y su `Content-Type` |
| DELETE | `/pacientes/{id}/foto` | 204, baja de la foto y desvinculación del paciente |

Una baja repetida retorna 404 porque el recurso dejó de estar activo. Borrar una historia da de baja sus atenciones en la misma transacción; una referencia clínica activa impide borrar paciente, usuario o médico (409). No se exponen rutas para borrado físico o recuperación.

## Cuerpos de las solicitudes

Paciente o médico, alta/edición:

```json
{"nombre":"Ana","apellido":"García","documento":"32123456"}
```

Se exigen campos no vacíos; nombre y apellido hasta 100 caracteres, documento hasta 30. Se eliminan espacios iniciales/finales. Documento único dentro de cada tipo, incluyendo bajas.

Usuario:

```json
{"nombreUsuario":"recepcion","clave":"Consultorio456!"}
```

Nombre de hasta 80 caracteres; clave de al menos 8 caracteres y como máximo 72 bytes UTF-8, por el límite de BCrypt. En alta la clave es obligatoria; en edición `"clave":null` (o su omisión) conserva la anterior. La unicidad del nombre se comprueba sin distinguir mayúsculas; al iniciar sesión se usa el nombre exacto. La respuesta nunca contiene la clave ni su hash.

Historia:

```json
{"pacienteId":"ID_PACIENTE","usuarioId":"ID_USUARIO"}
```

Ambos registros referenciados deben existir y estar activos. La respuesta incluye objetos `paciente` y `usuario`, sin contraseña ni bytes de foto.

Atención:

```json
{
  "fechaHistoria":"2026-01-15",
  "detalleHistoria":"Control clínico con evolución favorable y seguimiento.",
  "medicoId":"ID_MEDICO",
  "tipo":"CONTROL"
}
```

Fecha ISO `yyyy-MM-dd` no futura; texto no vacío de hasta 10.000 caracteres. Strategy `GENERAL` exige al menos 10 caracteres tras quitar espacios en extremos; `CONTROL`, al menos 20. El tipo selecciona la validación de esa operación y no se persiste. El médico y la historia deben estar activos. Las rutas anidadas verifican que el detalle pertenezca a la historia indicada.

Foto: `multipart/form-data` con una parte llamada **`archivo`**. Admite PNG/JPEG de hasta 2 MB y 20 millones de píxeles, comprueba MIME y decodificación. La respuesta JSON incluye `id`, `eliminado`, `nombre` y `mime`; el contenido solo se entrega por GET binario. La foto anterior se conserva marcada como eliminada al reemplazarla. El nombre enviado no se utiliza para escribir archivos en disco.

## Respuesta del expediente

```json
{
  "historia": {
    "id":"ID_HISTORIA",
    "eliminado":false,
    "paciente":{"id":"ID_PACIENTE","nombre":"Ana","apellido":"García","documento":"32123456","eliminado":false,"foto":null},
    "usuario":{"id":"ID_USUARIO","nombreUsuario":"demo","eliminado":false}
  },
  "atenciones":[
    {
      "id":"ID_DETALLE",
      "eliminado":false,
      "fechaHistoria":"2026-01-15",
      "detalleHistoria":"Control clínico con evolución favorable y seguimiento.",
      "medico":{"id":"ID_MEDICO","nombre":"Luis","apellido":"Pérez","documento":"27876543","eliminado":false}
    }
  ],
  "totalAtenciones":1
}
```

El detalle no serializa una referencia inversa a su historia; se evita una estructura circular. No se paginan listas en este ejercicio.

## Errores

| Estado | Significado |
|---|---|
| 400 | Validación, fecha futura, tipo desconocido, JSON o archivo inválido |
| 401 | Credenciales faltantes, inválidas o usuario dado de baja |
| 404 | Registro inexistente/eliminado o atención de otra historia |
| 409 | Documento/usuario duplicado o referencias activas que impiden una baja |

Los errores de negocio y validación retornan `{"mensaje":"Descripción del problema"}`. Los errores del filtro de seguridad pueden no incluir ese formato; ambos clientes contemplan ese caso. Archivos que superan el límite multipart se rechazan antes de entrar al servicio.

## Recorrido con PowerShell

Con el backend iniciado, ejecutar los siguientes comandos. Solo crean datos ficticios locales; cambiar documento/nombre de usuario si ya se ejecutaron antes.

```powershell
$base = 'http://localhost:8080/api'
$tokenDemo = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('demo:Consultorio123!'))
$cabeceras = @{ Authorization = "Basic $tokenDemo" }

$paciente = Invoke-RestMethod "$base/pacientes" -Headers $cabeceras -Method Post -ContentType 'application/json; charset=utf-8' -Body '{"nombre":"Ana","apellido":"Garcia","documento":"REST-123"}'
$medicos = Invoke-RestMethod "$base/medicos" -Headers $cabeceras
$usuarios = Invoke-RestMethod "$base/usuarios" -Headers $cabeceras
$historiaJson = @{ pacienteId = $paciente.id; usuarioId = $usuarios[0].id } | ConvertTo-Json
$historia = Invoke-RestMethod "$base/historias" -Headers $cabeceras -Method Post -ContentType 'application/json' -Body $historiaJson

$atencionJson = @{
    fechaHistoria = (Get-Date).ToString('yyyy-MM-dd')
    detalleHistoria = 'Consulta de prueba con evolucion favorable.'
    medicoId = $medicos[0].id
    tipo = 'GENERAL'
} | ConvertTo-Json
Invoke-RestMethod "$base/historias/$($historia.id)/detalles" -Headers $cabeceras -Method Post -ContentType 'application/json' -Body $atencionJson
Invoke-RestMethod "$base/historias/$($historia.id)" -Headers $cabeceras
```

Las solicitudes también se pueden reproducir con Postman o cualquier cliente HTTP. Usar la pestaña Basic Auth y los cuerpos anteriores.
