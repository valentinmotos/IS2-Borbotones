# TP2 REST Cliente Servidor - Ejercicio 3c (Template Method)

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

## Template Method

Es el mismo proyecto del ejercicio 2c, refactorizado con clases base abstractas. Cada clase base tiene un metodo plantilla que define los pasos fijos del algoritmo, y las subclases solo redefinen los pasos que cambian.

### Servidor

- `service/BaseService<E>`: el metodo plantilla es `guardar(entidad)` (`final`). Siempre hace `completarRelaciones` -> `asignarValoresPorDefecto` -> `dao.save`. `DomicilioService`, `PersonaService`, `LibroService` y `PrestamoService` redefinen `completarRelaciones` para traer las entidades relacionadas desde la base; `PrestamoService` tambien redefine `asignarValoresPorDefecto`. Tambien trae `listar`, `buscar` y `eliminar` comunes.
- `controller/BaseController<E>`: endpoints REST comunes (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`). Cada controller concreto solo indica su ruta y su service.
- `model/EntidadBase`: clase `@MappedSuperclass` con el `id` comun a todas las entidades.

### Cliente

- `controller/BaseController<D>`: el metodo plantilla es `mostrarFormulario(model, dto)` (`final`), que usan `nuevo` y `editar`. Siempre agrega el DTO al modelo -> llama a `cargarDatosFormulario(model)` -> devuelve la vista `carpeta/formulario`. Cada controller redefine `cargarDatosFormulario` para cargar los combos (localidades, domicilios, personas, libros). Tambien trae `listar`, `guardar` y `eliminar` comunes.
- `service/BaseService<D>`: el metodo plantilla es `nuevo()` (`final`). Siempre hace `crearDTO` (abstracto) -> `inicializarRelaciones` -> `asignarValoresPorDefecto`. Por ejemplo, `PrestamoService` inicializa libro y persona vacios y asigna las fechas por defecto.
- `dao/BaseDAO<D>` y `dto/BaseDTO`: acceso REST comun con `RestTemplate` y el `id` comun a los DTOs, necesarios para que los services y controllers base sean genericos.

El servidor carga datos iniciales para `Localidad`, `Libro` y los envios automaticos al iniciar.
