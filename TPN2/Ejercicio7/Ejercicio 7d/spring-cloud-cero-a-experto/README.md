# Spring Cloud - Ejercicio 7d

Clonado de https://github.com/David-DAM/spring-cloud-cero-a-experto, commit `f5ecb79`.
La documentación del autor se conserva en [README-original.md](README-original.md).

## Ejecutar en Windows sin Docker

Requisitos: JDK 21 o superior y Maven 3.6+. Se verificó con Java 26.0.2 y Maven 3.9.16.
La primera compilación necesita Internet para descargar las dependencias.

Desde PowerShell, en esta carpeta:

```powershell
.\iniciar.ps1
.\probar.ps1
```

Si la política de PowerShell impide ejecutar scripts:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\probar.ps1
```

El inicio compila los cinco módulos, comprueba que los puertos estén libres y arranca cada servicio en segundo plano, esperando que responda. Si ya compilaste, podés usar `.\iniciar.ps1 -SkipBuild`. Los logs y los identificadores de procesos se guardan en `.run/`.

| Servicio | URL |
| --- | --- |
| API Gateway | http://localhost:8060 |
| Eureka | http://localhost:8761 |
| Config Server | http://localhost:8088/employee-service/default |
| Employee Service | http://localhost:8082/actuator/health |
| Department Service | http://localhost:8083/actuator/health |

El gateway expone APIs REST, no una página web. Ejemplos:

```powershell
Invoke-RestMethod http://localhost:8060/api/v1/employees
Invoke-RestMethod http://localhost:8060/api/v1/departments
Invoke-RestMethod http://localhost:8060/api/v1/departments/assistant/hello
```

`probar.ps1` espera el descubrimiento de servicios, crea un departamento y un empleado con identificadores nuevos, consulta el departamento con sus empleados a través del gateway y comprueba la configuración centralizada. Los datos se almacenan en memoria y se pierden al reiniciar, tal como en el proyecto original.

Para detener únicamente los procesos iniciados por este proyecto:

```powershell
.\detener.ps1
```

El perfil `local` permite las peticiones sin autenticación y enlaza los servicios a `127.0.0.1`. Es un modo de desarrollo para esta PC. El script activa también `native`, requerido por Config Server. El límite de peticiones del gateway sigue activo.

## Compilar y probar

```powershell
mvn -B -ntp clean verify
```

Incluye las pruebas de contexto de los cinco módulos, la prueba del filtro del gateway, el contrato de empleados y la prueba de integración HTTP de departamentos. Las pruebas no necesitan procesos externos. El contrato se genera en el módulo proveedor `employee-service`; la integración de departamentos reemplaza el cliente de empleados con una implementación de prueba.

## Ejecutar con Keycloak

La configuración OAuth2 original se conserva cuando no se activa el perfil `local`. Este modo necesita Docker con Compose y Keycloak en el puerto 8080; el gateway utiliza 8060.

```powershell
docker compose up -d
```

Esperá a que http://localhost:8080/realms/davinchicoder/.well-known/openid-configuration responda. Con los servicios locales detenidos, iniciá en terminales separadas y en este orden:

```powershell
mvn -f service-registry/pom.xml spring-boot:run
mvn -f config-server/pom.xml spring-boot:run
mvn -f employee-service/pom.xml spring-boot:run
mvn -f department-service/pom.xml spring-boot:run
mvn -f api-gateway/pom.xml spring-boot:run
```

Esperá a que cada servicio termine de iniciar antes de ejecutar el siguiente comando. Las credenciales educativas importadas son `davinchicoder/password` (lectura y escritura) y `pepe/password` (lectura). Para obtener un token:

```powershell
$token = Invoke-RestMethod 'http://localhost:8080/realms/davinchicoder/protocol/openid-connect/token' -Method Post -Body @{
    grant_type = 'password'
    client_id = 'api-gateway'
    username = 'davinchicoder'
    password = 'password'
}
Invoke-RestMethod http://localhost:8060/api/v1/employees -Headers @{ Authorization = "Bearer $($token.access_token)" }
```

El modo con Keycloak no se verificó en esta PC porque Docker no está instalado.

## Adaptaciones de esta copia

- POM agregador para construir los cinco módulos con un comando.
- Lombok actualizado a 1.18.46, con procesadores de anotaciones configurados para compilar con el JDK instalado. Compatibilidad: https://projectlombok.org/changelog.
- Modelos configurados con `@Jacksonized` para recibir JSON y deserializar respuestas de Feign.
- Filtro del gateway corregido para continuar la cadena cuando no hay contexto de seguridad.
- Perfil local, scripts de inicio, prueba y parada, y configuración nativa explícita.
- Contrato de empleados ubicado en su proveedor y pruebas sin dependencias externas.