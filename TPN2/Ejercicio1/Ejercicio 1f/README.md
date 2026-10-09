# Ejercicio 1f: ejemplo de MapStruct

Proyecto basado en [map-struct-example de Alejandro Calderon Hernandez](https://github.com/alejandrocalderonhernandez/map-struct-example),
commit de referencia `eb643975fb3263a4a2b6cf3bfc8834ef31f4800f`.

Usa Java 21, Spring Boot 3.3.3, MapStruct 1.6.0 y Lombok 1.18.42.
Lombok se ajusto para compilar con el JDK instalado y se corrigio la configuracion
de exclusion de Lombok al generar el JAR ejecutable. Mantiene los modelos,
DTOs, mapper y datos en memoria del ejemplo original. Se agrego un listado
de paises y una respuesta HTTP 404 para identificadores inexistentes.

## Compilar y verificar

Desde `TPN2/Ejercicio1/Ejercicio1f`, con JDK 21 o superior:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Si tenes Maven instalado tambien podes usar `mvn test` y `mvn package`.
Las pruebas levantan un servidor HTTP en un puerto libre y verifican el listado,
la conversion a DTO y las respuestas 404 y 400.

## Ejecutar y consultar

```powershell
.\mvnw.cmd spring-boot:run
```

La API se ejecuta en `http://localhost:8080`. Para probar desde PowerShell:

```powershell
$paises = Invoke-RestMethod http://localhost:8080/country
$paises | Select-Object id, name, continent
Invoke-RestMethod "http://localhost:8080/country/$($paises[0].id)" | ConvertTo-Json -Depth 10
```

Tambien se pueden consultar esas URLs con un navegador o Postman.
Los UUID se generan en cada inicio: primero consultar `GET /country` y elegir un `id`.
La base de datos es una coleccion en memoria; no requiere instalar un motor de base de datos.
Los datos precargados son los del ejemplo original y se usan como datos de demostracion.

Si el puerto 8080 esta ocupado:

```powershell
java -jar target/mapstruct-example-0.0.1-SNAPSHOT.jar --server.port=8081
```

## Conversiones de MapStruct

- `Country.location.continent` se convierte en `CountryDto.continent`.
- `Language.isOfficial` se convierte en `LanguageDto.isOfficialLanguage`.
- `Language.speakersCount` se convierte en `LanguageDto.speakersTotal`.
- Las colecciones de idiomas y ecosistemas se convierten a sus respectivos DTOs.
- Los campos internos que no existen en el DTO se omiten en la respuesta.

Maven genera `CountryMapperImpl` al compilar, en `target/generated-sources/annotations`.
