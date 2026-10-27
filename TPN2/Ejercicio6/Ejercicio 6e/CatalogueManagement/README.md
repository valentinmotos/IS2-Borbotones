# Ejercicio 6e (2) y (3): CI/CD — GitHub Actions y Jenkins con Docker

Servicio `CatalogueManagement` (CRUD de libros) con dos pipelines:

- **Parte A:** GitHub Actions. En cada push o pull request a `main` ejecuta los tests y luego
  compila el proyecto (solo CI).
- **Parte B:** Jenkins corriendo en Docker. Descarga el código de GitHub, compila, ejecuta
  los tests y construye la imagen Docker de la aplicación.

## Estructura

```text
IS2-Borbotones/
├── .github/workflows/catalogue-management.yml   <- Parte A: GitHub Actions
└── TPN2/Ejercicio6/Ejercicio 6e/CatalogueManagement/
    ├── Jenkinsfile                               <- Parte B: pipeline de Jenkins
    ├── Dockerfile                                <- imagen de la aplicación
    ├── jenkins/
    │   ├── Dockerfile                            <- Jenkins + Docker CLI + plugins
    │   └── docker-compose.yml                    <- levanta Jenkins en Docker
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

## Parte A: GitHub Actions

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

## Parte B: Jenkins + Docker + GitHub

```text
 Desarrollador --push--> GitHub --checkout--> Jenkins (contenedor Docker)
                                                 |
                       +-------------------------+--------------------------+
                       v                         v                          v
                 Build (mvn clean          Test (mvn test,          Build Docker Image
                 package -DskipTests)      reporte JUnit)           (docker build)
                                                                            |
                                                                            v
                                                        Docker del host: catalogue-management:latest
```

### 1. Levantar Jenkins en Docker

```bash
cd jenkins
docker compose up -d --build
```

Es el equivalente al `docker run` del video. Jenkins corre como `root` y monta
`/var/run/docker.sock`, así puede construir imágenes en el Docker de la PC. La imagen
de [jenkins/Dockerfile](jenkins/Dockerfile) agrega el cliente de Docker y deja instalados
los plugins del video: Pipeline, Git, Maven Integration, Docker Pipeline, JUnit y Stage View.
Los datos de Jenkins se guardan en el volumen `jenkins_home`, así que no se pierden al reiniciar.

### 2. Primer ingreso

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Abrir `http://localhost:8080`, pegar esa contraseña, instalar los plugins sugeridos y crear
el usuario (o continuar como `admin`).

### 3. Configurar Maven

**Manage Jenkins → Tools → Maven installations → Add Maven**: nombre `Maven`
(tiene que coincidir con `tools { maven 'Maven' }` del Jenkinsfile), marcar
**Install automatically** y guardar.

### 4. Crear el pipeline

**New Item** → nombre `catalogue-mgmt-cicd` → tipo **Pipeline** → descripción
"CI/CD para Catalogue Management". En la sección **Pipeline** hay dos opciones:

- **Pipeline script** (como en el video): pegar el contenido del [Jenkinsfile](Jenkinsfile).
- **Pipeline script from SCM**: Git, URL `https://github.com/valentinmotos/IS2-Borbotones.git`,
  rama `*/main` y Script Path `TPN2/Ejercicio6/Ejercicio 6e/CatalogueManagement/Jenkinsfile`.

**Apply → Save → Build Now**. En **Pipeline Overview** se ven las etapas y sus logs.

### 5. Etapas del Jenkinsfile

| Etapa              | Qué hace                                                                   |
|--------------------|----------------------------------------------------------------------------|
| Checkout           | Clona el repo de GitHub. Hay que indicar la rama (`main`); sin la rama el checkout falla, como pasa en el video |
| Build              | `mvn clean package -DskipTests` dentro de la carpeta del proyecto          |
| Test               | `mvn test` y publica los resultados con el plugin JUnit                    |
| Build Docker Image | `docker build` con las etiquetas `catalogue-management:<número de build>` y `:latest` |

Para probar una rama que todavía no se mergeó, cambiar `branch: 'main'` en el Jenkinsfile.

### 6. Verificar la imagen

```bash
docker images catalogue-management
docker run -d --rm -p 8081:8080 catalogue-management:latest
```

La API queda en `http://localhost:8081/api/books` (el 8080 lo usa Jenkins).

### Próximos pasos (mencionados en el video)

- Subir la imagen a Docker Hub y desplegarla (por ejemplo en un servidor o en la nube).
- Agregar un webhook de GitHub para que el pipeline se dispare solo en cada push.
- Sumar análisis de seguridad y cobertura de código (por ejemplo SonarQube).
