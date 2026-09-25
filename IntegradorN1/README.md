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
 ├─ entities      entidades JPA (todas extienden BaseEntity)
 │   └─ enums
 ├─ repositories  interfaces Spring Data JPA
 ├─ services      lógica de negocio y validaciones
 ├─ dto           reportes, dashboard, catálogo
 ├─ controllers
 │   ├─ publico
 │   ├─ cliente
 │   └─ admin
 ├─ scheduler     newsletter, alertas de precio
 └─ exception     ErrorServiceException y handler global

resources/templates: layout/  fragments/  publico/  cliente/  admin/  email/  error/
resources/static:    vendor/template/  css/  js/  img/
```

## Kit de componentes E0-03

La página `/dev/componentes` muestra todos los fragments funcionando dentro del layout público de E0-02. Los datos de esa ruta son estáticos y no se guardan en la base.

| Componente | Fragmento y firma | Uso |
|---|---|---|
| Mensajes | `fragments/mensajes :: mensajes` | `<div th:replace="~{fragments/mensajes :: mensajes}"></div>` |
| Tabla | `fragments/tabla :: tabla(encabezados, filas, verUrl, editarUrl, eliminarUrl)` | `<div th:replace="~{fragments/tabla :: tabla(${encabezados}, ${filas}, ${verUrl}, ${editarUrl}, ${eliminarUrl})}"></div>` |
| Formulario | `fragments/formulario :: formulario(action, method, errorNombre, errorEmail)` | `<form th:replace="~{fragments/formulario :: formulario(${action}, 'post', ${errorNombre}, ${errorEmail})}"></form>` |
| Tabla de registros | `fragments/tabla :: tablaRegistros(encabezados, registros, baseUrl)` | `<div th:replace="~{fragments/tabla :: tablaRegistros(${encabezados}, ${registros}, ${baseUrl})}"></div>` |
| Campo de texto | `fragments/formulario :: campoTexto(nombre, etiqueta, valor, requerido)` | `<div th:replace="~{fragments/formulario :: campoTexto('observacion', 'Observación', ${observacion}, true)}"></div>` |
| Campo de selección | `fragments/formulario :: campoSelect(nombre, etiqueta, opciones, seleccionado, requerido)` | `<div th:replace="~{fragments/formulario :: campoSelect('tipoPago', 'Tipo de pago', ${opciones}, ${tipoPago}, true)}"></div>` |
| Acciones de formulario | `fragments/formulario :: acciones(cancelarUrl)` | `<div th:replace="~{fragments/formulario :: acciones('/admin/...')}"></div>` |
| Modal | `fragments/modal-confirmar :: modal(id, titulo, mensaje, actionUrl)` | `<div th:replace="~{fragments/modal-confirmar :: modal(${id}, ${titulo}, ${mensaje}, ${actionUrl})}"></div>` |
| Paginación | `fragments/paginacion :: paginacion(actual, total, baseUrl)` | `<nav th:replace="~{fragments/paginacion :: paginacion(${paginaActual}, ${totalPaginas}, ${baseUrl})}"></nav>` |
| Card de producto | `fragments/card-producto :: card(imagen, nombre, precio, enOferta)` | `<article th:replace="~{fragments/card-producto :: card(${imagen}, ${nombre}, ${precio}, ${enOferta})}"></article>` |
| Badge de estado | `fragments/badge-estado :: badge(estado)` | `<span th:replace="~{fragments/badge-estado :: badge(${estado})}"></span>` |
| Filtros | `fragments/filtros :: filtros(action)` | `<form th:replace="~{fragments/filtros :: filtros(${action})}"></form>` |
| KPI | `fragments/kpi :: kpi(titulo, valor, descripcion, icono)` | `<article th:replace="~{fragments/kpi :: kpi(${titulo}, ${valor}, ${descripcion}, ${icono})}"></article>` |

`mensajes` consume los atributos flash existentes `error` y `exito`; no crea un mecanismo nuevo. `tabla` recibe encabezados y filas como listas, y sus URLs pueden ser `null` para ocultar acciones. La tabla muestra un estado vacío cuando `filas` está vacío. `formulario` recibe errores ya calculados por el backend y no valida reglas de negocio.

El modal usa Bootstrap y recibe una URL de confirmación configurable. La paginación es solamente visual y genera enlaces con `?page=`; no implementa paginación backend. Los estados de `badge-estado` corresponden a `EstadoOrdenCompra` y `EstadoFactura`: `PENDIENTE_COMPLETAR`, `PENDIENTE_PAGO`, `PENDIENTE_ENVIO`, `PENDIENTE_ENTREGA`, `ENTREGADO`, `PAGADA`, `ANULADA` y `SIN_DEFINIR`.

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

El acceso se realiza desde `/login` con el correo de la cuenta. Las contraseñas se guardan con BCrypt. Las rutas `/admin/usuarios/**` y `/admin/configuracion/**` requieren el rol `JEFE`; el resto de `/admin/**` permite `JEFE` y `ADMINISTRATIVO`, y `/cliente/**` requiere `CLIENTE`. El resto del sitio es público.

El seeder crea las siguientes cuentas para desarrollo cuando la base está vacía:

| Rol | Correo | Contraseña |
|---|---|---|
| JEFE | `jefe@zero.com.ar` | `Jefe123!` |
| ADMINISTRATIVO | `admin@zero.com.ar` | `Admin123!` |

CSRF está activado; los formularios POST deben usar `th:action` para que Thymeleaf agregue el token.

## ABM de categorías y subcategorías E1-05

La sección `/admin/categorias` permite listar las categorías con subcategorías, crear y editar tanto categorías como subcategorías, y eliminar/desactivar solo cuando no hay productos activos relacionados. El árbol activo se expone por `CategoriaService.listarArbolActivo()` y está listo para consumirlo en el catálogo público.

### Validaciones y reglas

- El nombre es obligatorio y se normaliza quitando espacios en los extremos.
- No se permiten categorías duplicadas ni subcategorías duplicadas dentro de la misma categoría.
- La baja de una categoría o subcategoría queda bloqueada si tiene productos activos relacionados.
- El servicio valida la existencia de la categoría padre antes de guardar una subcategoría.
- Los errores y los mensajes de éxito se muestran con flash messages y los formularios conservan los datos ingresados al fallar.

### Seed inicial

El `DataSeeder` crea 4 categorías y 12 subcategorías en total cuando la base está vacía, sin duplicarlas si la aplicación se reinicia sobre una base ya cargada.

## ABM de referencia E0-06: Nacionalidad

Entrar a `/admin/nacionalidades` o a **Configuración → Nacionalidades** en el panel.
Permite listar las activas, crear, editar y confirmar una baja lógica en un modal.
Las operaciones exitosas vuelven al listado con `exito`; las validaciones vuelven al
formulario con `error` y conservan el nombre ingresado (Post/Redirect/Get).

El nombre es obligatorio, se guarda sin espacios en los extremos y se compara sin
importar mayúsculas, incluidas letras como Ñ. Un nombre ya utilizado, incluso por
una nacionalidad eliminada, sigue reservado para evitar duplicados históricos.
Editar el mismo registro sin cambiar su nombre está permitido. La búsqueda por ID
o nombre devuelve solo registros activos; si no existe, el service lanza
`ErrorServiceException`. El controller traduce los IDs inexistentes o eliminados a 404.
`listarNacionalidad()` incluye eliminadas y `listarNacionalidadActiva()` las filtra.

El seeder carga Argentina, Bolivia, Brasil, Chile, Colombia, España,
Italia, Paraguay, Perú, Uruguay y Venezuela **solo si toda la base está vacía**.
No modifica una base existente. En una base con datos se pueden dar de alta desde
el ABM; no hace falta borrar información para probarlo.

### Crear otro ABM a partir de este

1. Usar la entidad y el repository correspondientes. Agregar consultas de listado
   ordenado, listado activo y búsqueda por ID activo como en `NacionalidadRepository`.
2. Copiar la estructura de `services/NacionalidadService.java`, respetando los nombres
   del UML. Inyectar el repository por constructor. Concentrar reglas en `validar`,
   excluir el ID actual al comprobar duplicados y validar antes de modificar la entidad.
   Las escrituras usan `@Transactional(rollbackFor = ErrorServiceException.class)`
   porque la excepción de negocio es checked. La baja cambia `eliminado`, nunca borra.
3. Tomar `controllers/admin/NacionalidadController.java` como referencia: inyectar
   únicamente el service, definir GET para vistas y POST para cambios, y manejar
   validaciones con mensajes flash y destinos explícitos. No recibir una entidad JPA
   completa desde el formulario: aceptar solo los campos editables.
4. Copiar `templates/admin/nacionalidades/listado.html` y `formulario.html`, cambiar
   las rutas y los textos y mantener `layout:decorate="~{layout/admin}"`. Agregar
   el enlace en el menú del layout.
5. Reutilizar los fragments del kit. Para catálogos de un nombre están disponibles:
   - `fragments/tabla :: tablaNombre(registros, baseUrl)`: recibe registros con `id`
     y `nombre`; genera edición por fila y botones que abren `#eliminar-{id}`.
   - `fragments/formulario :: formularioNombre(action, nombre, cancelarUrl)`:
     formulario POST con nombre precargado y token CSRF generado por `th:action`.
   - `fragments/modal-confirmar :: modal(id, titulo, mensaje, actionUrl)`: incluir
     un modal por registro con ID `eliminar-{id}` y acción POST de baja.
   - `fragments/mensajes :: mensajes`: muestra `error` y `exito`.
   Para ABM con varios campos (ver Formas de pago, E1-08):
   - `fragments/tabla :: tablaRegistros(encabezados, registros, baseUrl)`: cada registro
     es un `dto.FilaTablaDTO(id, nombre, celdas)`, armado en el service. `nombre` se
     usa en las etiquetas de accesibilidad y en el modal; `celdas` son las columnas.
   - `fragments/formulario :: campoTexto(...)`, `campoSelect(...)` y `acciones(cancelarUrl)`:
     se incluyen dentro de un `<form th:action="@{...}" method="post" class="zero-form">`.
     `campoSelect` recibe las opciones como `Map` valor → texto (usar `LinkedHashMap`
     para respetar el orden).
   Para campos adicionales, extender primero el kit; las firmas anteriores de
   `tabla` y `formulario` siguen disponibles para los ejemplos de E0-03.
6. Agregar datos iniciales al grupo correspondiente del `DataSeeder`, respetando
   sus dependencias y el control de base vacía.
7. Adaptar `NacionalidadServiceTest` (JUnit + Mockito) y `NacionalidadIntegrationTest`
   (MockMvc + SQLite en memoria). Cubrir duplicados en alta y edición, campos
   obligatorios, baja lógica, IDs inválidos, flashes y CSRF. Ejecutar `./mvnw test`.

| Método | Ruta | Operación |
|---|---|---|
| GET | `/admin/nacionalidades` | Listar activas |
| GET | `/admin/nacionalidades/nueva` | Formulario de alta |
| POST | `/admin/nacionalidades` | Crear |
| GET | `/admin/nacionalidades/{id}/editar` | Formulario de edición |
| POST | `/admin/nacionalidades/{id}/editar` | Modificar |
| POST | `/admin/nacionalidades/{id}/eliminar` | Baja lógica |