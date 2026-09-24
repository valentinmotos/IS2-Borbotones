# E-Commerce Zero

Trabajo Integrador de IS2. El plan completo, con las decisiones de diseño, las convenciones y todos los issues, está en [docs/PlanDeTrabajo.md](docs/PlanDeTrabajo.md).

## Requisitos

- **JDK 21 o superior.** El proyecto compila con `release 21`.
- No hace falta instalar Maven: el repo trae el Maven Wrapper (`mvnw`), que descarga la versión correcta la primera vez.
- Tampoco hace falta instalar una base de datos: se usa SQLite embebido.

## Cómo correrlo

Desde la carpeta `IntegradorN1`:

```bash
./mvnw spring-boot:run        # Linux, macOS o Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd o PowerShell)
```

La app queda en <http://localhost:8080>.

Para compilar y correr los tests:

```bash
./mvnw clean verify
```

## Base de datos

- El archivo de la base es `data/zero.db`, relativo a la carpeta desde donde se corre la app. La carpeta `data/` se crea sola al arrancar.
- Está en el `.gitignore`: cada uno tiene su base local.
- Hibernate crea y actualiza las tablas solo (`ddl-auto=update`).
- Al arrancar con la base vacía, el `DataSeeder` (`config/DataSeeder.java`) carga los datos iniciales. Si la base ya tiene datos, no carga nada.
- **Resetear la base:** frenar la app, borrar `data/zero.db` y volver a levantarla. Hay que hacerlo cuando cambia una entidad de forma incompatible o cuando alguien agrega datos nuevos al seeder.
- Los tests usan una base en memoria y no tocan `data/zero.db`.

## Estructura

```
com.zero.ecommerce
 ├─ config        seguridad, mail, scheduling, DataSeeder
 ├─ entity        entidades JPA (todas extienden BaseEntity)
 │   └─ enums
 ├─ repository    interfaces Spring Data JPA
 ├─ service       lógica de negocio y validaciones
 ├─ dto           reportes, dashboard, catálogo
 ├─ controller
 │   ├─ publico
 │   ├─ cliente
 │   └─ admin
 ├─ scheduler     newsletter, alertas de precio
 └─ exception     ErrorServiceException y handler global

resources/templates: layout/  fragments/  publico/  cliente/  admin/  email/  error/
resources/static:    vendor/template/  css/  js/  img/
```

## Reglas de código (resumen)

El detalle está en la sección "Convenciones técnicas" del plan. El revisor de cada PR rechaza lo que no las cumpla.

- **Capas:** controller → service → repository. Un controller nunca usa un repository, y los services no saben nada de la vista.
- **Inyección de dependencias por constructor.** Nada de `@Autowired` sobre atributos.
- **Entidades:** todas extienden `BaseEntity` (`id` String UUID y `eliminado`). Las herencias usan `InheritanceType.JOINED`.
- **Baja lógica:** `eliminarX()` pone `eliminado = true` y `listarXActivo()` filtra por `eliminado = false`. No se borra nada físicamente.
- **Validaciones:** cada service tiene su `validar(...)`, que lanza `ErrorServiceException` con un mensaje en español. Si el controller no la atrapa, el `GlobalExceptionHandler` vuelve a la página anterior con el mensaje en el flash attribute `error`. Los mensajes de éxito van en el flash attribute `exito`.
- **Nombres** según el diagrama: `crearProducto`, `buscarProductoPorCodigo`, `listarProductoActivo`.
- **Rutas:** `/admin/...` para el panel, `/cliente/...` para el cliente logueado y el resto es público.
- **Vistas:** toda vista decora `layout/publico.html` o `layout/admin.html` y usa los fragments del kit de componentes. No se permiten estilos inline ni CSS por página.

## Seguridad

Por ahora Spring Security deja todo público (`SecurityConfig`, marcado con `TODO E1-01`). CSRF está activado, así que los formularios POST tienen que usar `th:action` para que Thymeleaf agregue el token. E1-01 agrega el login y las reglas por rol.
