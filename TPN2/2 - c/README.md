# TP2 REST Cliente Servidor

Proyecto Spring Boot con dos aplicaciones locales:

- `servidor`: API REST en `http://localhost:8000`, JPA y SQLite3.
- `cliente`: sitio HTML en `http://localhost:8080`, Thymeleaf, DTOs y `RestTemplate`.

## Requisitos

- Java instalado.
- No hace falta Maven global: se incluye `mvnw.ps1`.

## Compilar

```powershell
.\mvnw.ps1 clean package -DskipTests
```

## Ejecutar

En una terminal:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

En otra terminal:

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Luego abrir:

- Cliente HTML: `http://localhost:8080`
- API libros: `http://localhost:8000/api/libros`
- API personas: `http://localhost:8000/api/personas`
- API domicilios: `http://localhost:8000/api/domicilios`
- API localidades: `http://localhost:8000/api/localidades`

## Estructura

Cada controller delega en un solo service. Los services contienen la logica de negocio y usan otros services cuando necesitan relaciones. Los DAO del cliente consumen el backend con `RestTemplate`; los DAO del servidor acceden a la base por Spring Data JPA.

El servidor carga datos iniciales para `Localidad` y `Libro` al iniciar.
