# TP2 - Ejercicio 1e - API Externa (Clima)

Proyecto Spring Boot que consume la API externa de **OpenWeatherMap** desde el backend con `RestTemplate` y muestra el clima actual de una ciudad en un frontend HTML/Bootstrap.

## Funcionamiento

1. El frontend (`static/index.html` + `static/js/app.js`) hace `fetch` a `GET /api/clima?ciudad=Mendoza`.
2. `ClimaController` delega en `ClimaService`.
3. `ClimaDAO` llama con `RestTemplate` a `https://api.openweathermap.org/data/2.5/weather`.
4. `ClimaService` arma un `ClimaDTO` con los datos necesarios y lo devuelve como JSON al frontend.

## Requisitos

- Java instalado.
- No hace falta Maven global: se incluye `mvnw.ps1`.
- API key de OpenWeatherMap: copiar `apikey.properties.example` como `apikey.properties` (en esta carpeta) y poner la key. Ese archivo no se sube al repositorio.

## Ejecutar

```powershell
.\mvnw.ps1 spring-boot:run
```

Luego abrir:

- Frontend: `http://localhost:8080`
- API: `http://localhost:8080/api/clima?ciudad=Mendoza`
