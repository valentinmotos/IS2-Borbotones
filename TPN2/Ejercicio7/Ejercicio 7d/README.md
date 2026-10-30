# Ejercicio 7d - Spring Cloud

Repositorio clonado en [spring-cloud-cero-a-experto](spring-cloud-cero-a-experto/README.md).

Desde PowerShell, en esta carpeta:

```powershell
cd '.\spring-cloud-cero-a-experto'
.\iniciar.ps1
.\probar.ps1
```

Gateway: http://localhost:8060. Eureka: http://localhost:8761.

Para detener los cinco servicios: ` .\detener.ps1`.

El perfil local funciona sin Docker ni Keycloak y escucha solamente en esta PC.
La configuración original con OAuth2/Keycloak se conserva para ejecutar sin el perfil `local`.
