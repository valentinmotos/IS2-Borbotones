# Ejercicio 6e (2): CI/CD Parte A — Spring Boot + GitHub Actions

Servicio `CatalogueManagement` (CRUD de libros) con un pipeline de **Integración Continua**
en GitHub Actions que, en cada push o pull request a `main`, ejecuta los tests y luego
compila el proyecto. Es la Parte A del video: solo CI, todavía sin despliegue (CD).

## Estructura

```text
IS2-Borbotones/
├── .github/workflows/catalogue-management.yml   <- pipeline de CI
└── TPN2/Ejercicio6/Ejercicio 6e/CatalogueManagement/
    ├── pom.xml
    └── src/
        ├── main/java/com/example/CatalogueManagement/
        │   ├── controller/BookController.java
        │   ├── entity/Book.java
        │   ├── repository/BookRepository.java
        │   ├── service/BookService.java
        │   └── CatalogueManagementApplication.java
        └── test/java/com/example/CatalogueManagement/
            ├── controller/BookControllerTest.java
            ├── service/BookServiceTest.java
            └── CatalogueManagementApplicationTests.java
```

GitHub solo lee los workflows de la carpeta `.github/workflows` en la **raíz del repositorio**.
Como este repo contiene todos los ejercicios, el archivo está en la raíz y usa
`working-directory` para trabajar sobre esta carpeta. Con el filtro `paths` el pipeline
solo se dispara cuando cambia algo de este proyecto o del propio workflow.

## Pipeline

```text
 git push / pull request a main
              |
              v
   +----------------------+        +-------------------------------+
   | job: test            |        | job: build  (needs: test)     |
   | - checkout           |  OK    | - checkout                    |
   | - JDK 17 (Temurin)   | -----> | - JDK 17 (Temurin)            |
   | - mvn -B test        |        | - mvn -B clean verify         |
   | - sube los reportes  |        | - sube el JAR como artefacto  |
   +----------------------+        +-------------------------------+
              |
           FALLA -> el build no se ejecuta (no se genera un JAR roto)
```

- **Disparadores:** `push` a `main`, `pull_request` hacia `main` y ejecución manual
  (`workflow_dispatch`) desde la pestaña **Actions**.
- **test:** corre los 13 tests y sube `target/surefire-reports` como artefacto
  `test-results`, aunque los tests fallen (`if: always()`).
- **build:** depende de `test` (`needs: test`), así que solo se ejecuta si los tests pasan.
  Genera el JAR y lo sube como artefacto `catalogue-management-jar`.

## API

| Método | Endpoint          | Descripción         |
|--------|-------------------|---------------------|
| GET    | `/api/books`      | Lista los libros    |
| GET    | `/api/books/{id}` | Busca un libro      |
| POST   | `/api/books`      | Crea un libro (201) |
| PUT    | `/api/books/{id}` | Actualiza un libro  |
| DELETE | `/api/books/{id}` | Elimina (204)       |

Usa H2 en memoria, así que no requiere base de datos instalada (tampoco en el pipeline).

## Ejecutar localmente

```bash
./mvnw test             # tests
./mvnw spring-boot:run  # http://localhost:8080/api/books
```

En PowerShell usar `.\mvnw.cmd`. El `pom.xml` incluye `-Dnet.bytebuddy.experimental=true`
para que Mockito funcione también con versiones de Java más nuevas que 17.

## Disparar el pipeline

1. Hacer un cambio en el proyecto (por ejemplo, en `BookController`).
2. `git add .`, `git commit -m "trigger pipeline"` y `git push`.
3. Abrir un pull request hacia `main` (o mergear): en la pestaña **Actions** del repositorio
   aparece la ejecución "Catalogue Management CI" con los jobs `test` y `build`.
