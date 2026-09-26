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
 ├─ exception     ErrorServiceException y handler global
 └─ utils         funciones estáticas reutilizables (ej: TextoUtils.mismoNombre)

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
| Filtro por selección | `fragments/filtros :: filtroSelect(action, nombre, etiqueta, opciones, seleccionado)` | `<form th:replace="~{fragments/filtros :: filtroSelect(${baseUrl}, 'pais', 'País', ${paises}, ${pais})}"></form>` |
| Campo de clave | `fragments/formulario :: campoClave(nombre, etiqueta, requerido, ayuda)` | `<div th:replace="~{fragments/formulario :: campoClave('clave', 'Clave', false, 'Dejala vacía para no cambiarla.')}"></div>` |
| Checkbox | `fragments/formulario :: campoCheckbox(nombre, etiqueta, marcado)` | `<div th:replace="~{fragments/formulario :: campoCheckbox('tls', 'Usar TLS', ${tls})}"></div>` |
| Selección agrupada | `fragments/formulario :: campoSelectAgrupado(nombre, etiqueta, grupos, seleccionado, requerido)` | `<div th:replace="~{fragments/formulario :: campoSelectAgrupado('departamentoId', 'Departamento', ${grupos}, ${departamentoId}, true)}"></div>` |
| KPI | `fragments/kpi :: kpi(titulo, valor, descripcion, icono)` | `<article th:replace="~{fragments/kpi :: kpi(${titulo}, ${valor}, ${descripcion}, ${icono})}"></article>` |
| Switch | `fragments/formulario :: campoSwitch(nombre, etiqueta, marcado)` | `<div th:replace="~{fragments/formulario :: campoSwitch('enOferta', 'En oferta', ${enOferta})}"></div>` |
| Texto largo | `fragments/formulario :: campoTextoLargo(nombre, etiqueta, valor, requerido)` | `<div th:replace="~{fragments/formulario :: campoTextoLargo('descripcion', 'Descripción', ${descripcion}, true)}"></div>` |
| Solo lectura | `fragments/formulario :: campoSoloLectura(nombre, etiqueta, valor)` | `<div th:replace="~{fragments/formulario :: campoSoloLectura('codigo', 'Código', ${codigo})}"></div>` |
| Fecha | `fragments/formulario :: campoFecha(nombre, etiqueta, valor, requerido)` | `<div th:replace="~{fragments/formulario :: campoFecha('fechaNacimiento', 'Fecha de nacimiento', ${fechaNacimiento}, true)}"></div>` |
| Tabla con imagen | `fragments/tabla :: tablaRegistrosImagen(encabezados, registros, baseUrl)` | `<div th:replace="~{fragments/tabla :: tablaRegistrosImagen(${encabezados}, ${productos}, '/admin/productos')}"></div>` |
| Campo de filtro | `fragments/filtros :: campoFiltroTexto(nombre, etiqueta, valor, ayuda)` y `campoFiltroSelect(nombre, etiqueta, opciones, seleccionado)` | `<div th:replace="~{fragments/filtros :: campoFiltroTexto('buscar', 'Buscar', ${buscar}, 'Código o nombre')}"></div>` |
| Categoría → subcategoría | `fragments/categoria :: cascada(arbol, nombreCategoria, nombreSubCategoria, categoriaId, subCategoriaId, esFiltro)` | `<th:block th:replace="~{fragments/categoria :: cascada(${arbol}, 'categoriaId', 'subCategoriaId', ${categoriaId}, ${subCategoriaId}, false)}"></th:block>` |

`mensajes` consume los atributos flash existentes `error` y `exito`; no crea un mecanismo nuevo. `tabla` recibe encabezados y filas como listas, y sus URLs pueden ser `null` para ocultar acciones. La tabla muestra un estado vacío cuando `filas` está vacío. `formulario` recibe errores ya calculados por el backend y no valida reglas de negocio.

El modal usa Bootstrap y recibe una URL de confirmación configurable. La paginación es solamente visual y genera enlaces con `?page=` (o `&page=` si `baseUrl` ya trae filtros); no implementa paginación backend.

`tablaRegistrosImagen` recibe `dto.FilaTablaImagenDTO(id, nombre, imagenId, celdas)`; con `imagenId` nulo muestra `img/default-image.png`. Los campos de filtro van dentro de un `<form method="get" class="zero-filters zero-filters-amplio">` (cuatro por fila), con los botones en un `<div class="zero-filters-acciones">`. `categoria :: cascada` recibe `CategoriaService.listarArbolActivo()`: con `esFiltro = true` muestra "Todas" y no es obligatorio; con `false` se incluye dentro de un `div.form-row`. Sin JavaScript muestra todas las subcategorías agrupadas por categoría, y `static/js/categoria-cascada.js` deja solo las de la categoría elegida. Los estados de `badge-estado` corresponden a `EstadoOrdenCompra` y `EstadoFactura`: `PENDIENTE_COMPLETAR`, `PENDIENTE_PAGO`, `PENDIENTE_ENVIO`, `PENDIENTE_ENTREGA`, `ENTREGADO`, `PAGADA`, `ANULADA` y `SIN_DEFINIR`.

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
| CLIENTE | `cliente@zero.com.ar` | `Cliente123!` |

CSRF está activado; los formularios POST deben usar `th:action` para que Thymeleaf agregue el token.

## Registro y activación de cuenta E2-05

Desde **Crear cuenta** (header o login) se entra a `/registro`: correo, contraseña y confirmación.

- **Validaciones (RF01):** correo obligatorio y con formato válido, que no esté registrado (incluidas
  cuentas dadas de baja o pendientes de activar), clave de al menos 8 caracteres y confirmación igual.
  Si falla, el formulario vuelve con el correo, pero nunca con la clave.
- **Alta:** `UsuarioService.registrarCliente(correo, clave, confirmacion)` crea el `Usuario` con rol
  `CLIENTE`, la clave con BCrypt, el correo en minúsculas y un `codigoActivacion` aleatorio de 6 dígitos.
- **Correo (RF02):** `email/activacion.html`, con el código y un botón a `/registro/activar?correo=...`.
  Lo manda `enviarCodigoActivacion(usuario, urlActivacion)`, que es asincrónico.
- **Activación:** en `/registro/activar` se ingresan correo y código. Si coinciden, `activarCuenta` pone el
  código en `null` y redirige al login con "¡Tu cuenta quedó activada!". Antes de eso, el login muestra
  "La cuenta todavía no está activada" con un link a esta página.
- **Reenviar código:** botón en la misma página. `reenviarCodigoActivacion` genera un código nuevo y el
  anterior deja de servir.
- **Sin correo configurado:** el envío falla en el log, pero el código también queda en el log
  (`Código de activación de ...: 123456`), así se puede activar la cuenta en desarrollo.

| Método | Ruta | Operación |
|---|---|---|
| GET | `/registro` | Formulario de registro |
| POST | `/registro` | Registrar cliente y mandar el código |
| GET | `/registro/activar?correo=` | Formulario de activación |
| POST | `/registro/activar` | Activar la cuenta |
| POST | `/registro/reenviar` | Generar y mandar un código nuevo |

Para otros issues: `UsuarioService.LARGO_MINIMO_CLAVE` es la regla de clave del registro (E2-06 y E2-08
aplican la misma).

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

## ABM de productos E2-01

Entrar a **Productos** en el panel (`/admin/productos`, `JEFE` y `ADMINISTRATIVO`).

- **Listado:** miniatura, código, nombre, talle, subcategoría, oferta, precio vigente y stock actual, de a
  10 por página. Se filtra por categoría → subcategoría, por oferta y con un buscador por código o nombre
  (sin importar mayúsculas ni tildes). La paginación conserva los filtros.
- **Precio y stock:** el precio es el de la vigencia actual (`VigenciaPrecioService`, E2-03), o "Sin precio" si
  el producto todavía no tiene uno; el stock sale de `StockService.buscarStockActual` (E2-02). Los dos se
  arman en `ProductoService.armarFila`.
- **Formulario:** código, nombre, talle, descripción, categoría → subcategoría, switch de oferta e imagen
  (fragment `input-imagen` de E1-06, con vista previa).

### Validaciones y reglas

- Código, nombre, descripción, talle y subcategoría son obligatorios; la imagen es obligatoria al crear.
  Al editar, si no se elige un archivo, se conserva la imagen actual.
- El código no puede repetirse entre productos activos (sin importar mayúsculas). **No se edita:**
  `modificarProducto` del diagrama no lo recibe, así que en la edición se muestra de solo lectura.
- La subcategoría y su categoría tienen que estar activas.
- La baja es lógica. Una vez dado de baja, el código se puede volver a usar.
- Los datos se validan antes de guardar la imagen y todo corre en una transacción: si algo falla, no queda
  una imagen suelta. Si falla la validación, el formulario vuelve con los datos cargados (el archivo hay
  que elegirlo de nuevo).

### Para otros issues

- `crearProducto(codigo, nombre, descripcion, talle, enOferta, idImagen, idSubCategoria)` recibe una imagen ya
  guardada con `ImagenService.crearImagen`: es la que usa el seeder de E2-02. El formulario usa
  `crearProductoConImagen` / `modificarProductoConImagen`, que reciben el `MultipartFile`.
- `listarProductoActivo(texto, idCategoria, idSubCategoria, enOferta)` devuelve los activos filtrados (cada
  filtro puede ser `null`). `buscarProductoPorCodigo` y `buscarProductoPorNombre` devuelven solo activos;
  como cada talle es un producto, `buscarProductoPorNombre` devuelve el primero por talle.
- Los productos de demostración los carga el seeder de E2-02 (ver abajo).

| Método | Ruta | Operación |
|---|---|---|
| GET | `/admin/productos?buscar=&categoria=&subcategoria=&oferta=&page=` | Listar activos con filtros |
| GET | `/admin/productos/nuevo` | Formulario de alta |
| POST | `/admin/productos` | Crear (multipart) |
| GET | `/admin/productos/{id}/editar` | Formulario de edición |
| POST | `/admin/productos/{id}/editar` | Modificar (multipart) |
| POST | `/admin/productos/{id}/eliminar` | Baja lógica |

## Lectura de stock y catálogo de demostración E2-02

### StockService (lectura)

El stock se guarda como movimientos: cada `Stock` tiene en `cantidadActual` el saldo del producto después
del movimiento.

- `buscarStockActual(idProducto): int`: el `cantidadActual` del último movimiento activo del producto (por
  `fecha`), o 0 si nunca tuvo movimientos. El diagrama devuelve un `Stock`, pero se respeta el contrato del
  kickoff de la Etapa 2 (`int`). Lo usa el listado de `/admin/productos` en la columna "Stock actual".
- `buscarStock(id)` (solo activos) y `listarStock()` (todos, del más reciente al más antiguo).
- La escritura (`crearStock`, registrar y revertir movimientos) es de E3-03.

### Catálogo de demostración

Con la base vacía, el `DataSeeder` carga **20 productos** en las 12 subcategorías: 6 en oferta y varios
modelos en más de un talle (Remera Dry Fit M y L, Zapatilla Run 41 y 42, Calza Fit S y M, etc.), siguiendo la
regla de un producto por talle. Todos arrancan con stock 0 y una vigencia de precio de demostración de
$10.000, que se puede modificar desde **Precios**.

- Las fotos están en `src/main/resources/seed/img/`. Son de Unsplash (licencia libre); el origen y el autor de
  cada una están en `seed/img/CREDITOS.md`.
- Se cargan con `ImagenService.crearImagen`, así pasan las mismas validaciones que en el formulario. Para
  pasarle un archivo del classpath se usa `utils/ArchivoEnMemoria`, un `MultipartFile` en memoria.
- Cada producto tiene su propia `Imagen` aunque varios talles compartan la foto (`Producto.imagen` es 1 a 1).
- **Para verlos en una base que ya existe** hay que borrar `data/zero.db` y volver a levantar la app.
- En los tests el seeder también corre: `ProductoIntegrationTest` da de baja esos productos al empezar cada
  test (se revierte al terminar) para partir de un catálogo vacío.

## Actualización de precios por inflación E2-04

En **Precios → Actualización por inflación** (`/admin/precios/actualizacion`) se puede aplicar un aumento
a todo el catálogo, a una categoría o a una subcategoría. La vista previa muestra precios actuales, precios
nuevos redondeados a múltiplos de $10 y diferencias; la confirmación crea una vigencia nueva por producto
y cierra las anteriores en una sola transacción. El porcentaje debe ser mayor que cero y la fecha desde no
puede ser anterior a hoy ni igual o anterior al inicio de alguno de los precios vigentes. Si un producto del
alcance no tiene precio vigente, se informa y no se aplica ninguna actualización. La confirmación también
comprueba que los precios no hayan cambiado desde la vista previa.

## Perfil del cliente E2-07

Un cliente logueado entra desde **Mi cuenta → Mi perfil** (`/cliente/perfil`, solo `CLIENTE`). Para probar
sin registrarse está el cliente del seeder: `cliente@zero.com.ar` / `Cliente123!`.

- **Datos (RF03 y RF05):** nombre, apellido, sexo, fecha de nacimiento, tipo y número de documento,
  nacionalidad y teléfono celular (se guarda como `ContactoTelefonico` `CELULAR`).
- **Dirección de entrega:** fragment `direccion` de E1-04, con la cascada país → provincia → departamento →
  localidad.
- **Foto de perfil opcional:** fragment `input-imagen` de E1-06, guardada con `ImagenService` como `PERSONA`.
  Al editar, si no se elige un archivo, se conserva la actual.
- La primera vez se crea el `Cliente` y se asocia al usuario logueado; las siguientes se modifica el mismo
  cliente, su dirección y su teléfono (no se crean otros).
- **Sexo** no está en el diagrama, pero lo pide el enunciado: se agregó a `Cliente` como enum `Sexo`.

### Validaciones

- Todos los datos son obligatorios salvo la foto y los campos opcionales de la dirección.
- Mayor de 18 años, y la fecha no puede ser futura.
- DNI de 7 u 8 números (se guarda sin puntos) o pasaporte de 6 a 15 letras o números.
- El documento es único por tipo y número entre todas las personas activas (clientes y empleados).
- Si algo falla, el formulario vuelve con los datos cargados, incluida la dirección.

### Para otros issues

- `ClienteService.buscarClientePorUsuario(idUsuario)` devuelve un `Optional`: vacío si el cliente todavía no
  cargó su perfil (lo usa E2-08 en `perfilCompleto`).
- `crearCliente`, `modificarCliente`, `validar`, `listarCliente`, `listarClienteActivo` y
  `asociarClienteUsuario(cliente, usuario)` siguen el diagrama. La pantalla usa `guardarPerfilCliente`, que
  crea o modifica según corresponda.

| Método | Ruta | Operación |
|---|---|---|
| GET | `/cliente/perfil` | Formulario con los datos guardados |
| POST | `/cliente/perfil` | Crear o modificar el perfil (multipart) |

## ABM de ubicación E1-03

Entrar a **Configuración → Ubicación** (`/admin/configuracion/ubicacion`, solo `JEFE`). Hay una
pestaña por entidad: Países, Provincias, Departamentos y Localidades. Cada listado se filtra por su
padre (`?pais=`, `?provincia=`, `?departamento=`) y el botón de alta precarga el padre filtrado.
En Localidades el departamento se elige con los selects en cascada país → provincia → departamento de E1-04.

- El nombre es obligatorio y no puede repetirse dentro del mismo padre. La comparación ignora
  mayúsculas, tildes y espacios en los extremos: "Guaymallén" y "GUAYMALLEN" son duplicados.
- El código postal de la localidad es obligatorio: 4 dígitos (`5500`) o CPA (`M5500ABC`).
- El padre tiene que existir y estar activo.
- La baja es lógica. No se puede eliminar un país, provincia o departamento con hijos activos.
- Solo se comparan los registros activos, así que un registro eliminado se puede volver a crear.

El seeder carga Argentina, sus 24 provincias (incluida CABA), los 18 departamentos de Mendoza y las
principales localidades de Gran Mendoza con su código postal. **Solo corre con la base vacía:** para
cargar estos datos sobre una base existente hay que borrar `data/zero.db` y volver a levantar la app.

## Fragment de dirección E1-04

Se incluye con una línea dentro de un `<form class="zero-form">`:

```html
<div th:replace="~{fragments/direccion :: direccion(${direccionForm})}"></div>
```

- `direccionForm` es un `DireccionForm`: `new DireccionForm()` en el alta, `DireccionForm.desde(direccion)`
  al editar, o el mismo objeto recibido con `@ModelAttribute` para volver al formulario después de un error.
- Envía `calle`, `numeracion`, `barrio`, `manzanaPiso`, `casaDepartamento`, `referencia` y `localidadId`
  (más `paisId`, `provinciaId` y `departamentoId`, que solo sirven para precargar los selects).
- Los selects se cargan en cascada con `static/js/ubicacion-cascada.js`. El código postal se completa solo
  al elegir la localidad y no se envía.
- `DireccionService` tiene `crearDireccion` (devuelve la `Direccion` para asociarla), `modificarDireccion`,
  `eliminarDireccion` (baja lógica), `buscarDireccion` y `buscarDireccionPorCalleNumeracion`. Calle,
  numeración y una localidad activa son obligatorias; el resto es opcional y se guarda `null` si viene vacío.
- Para solo los selects, sin el resto de la dirección:
  `fragments/ubicacion :: cascada(hasta, paisId, provinciaId, departamentoId, localidadId)`, con `hasta`
  igual a `'departamento'` o `'localidad'`. Lo usa el formulario de Localidad.

Endpoints JSON públicos (solo registros activos; un id vacío o inexistente devuelve `[]`):

| Endpoint | Devuelve |
|---|---|
| `GET /api/ubicacion/paises` | `[{id, nombre}]` |
| `GET /api/ubicacion/provincias?pais={id}` | `[{id, nombre}]` |
| `GET /api/ubicacion/departamentos?provincia={id}` | `[{id, nombre}]` |
| `GET /api/ubicacion/localidades?departamento={id}` | `[{id, nombre, codigoPostal}]` |

La página de prueba `/dev/direccion` guarda una dirección real y, al guardar, se recarga con `?id=` para
mostrar los selects precargados.

## Empresa, correo y envío E1-07

### Empresa

**Configuración → Empresa** (`/admin/configuracion/empresa`, solo `JEFE`): ABM de la sede central y sus
sucursales, con razón social, CUIT, tipo, dirección (fragment de E1-04), un correo y un teléfono.

- El CUIT se valida con su dígito verificador (`utils/CuitUtils`), se acepta con o sin guiones y se guarda
  como `XX-XXXXXXXX-X`. No puede repetirse entre empresas activas. CUITs válidos para probar:
  `30-71234567-1`, `20-12345678-6`.
- Hay **una sola `SEDE_CENTRAL`**: no se puede crear otra, pasarla a sucursal ni darla de baja. Es la
  empresa que envía los correos y a la que pertenece el stock.
- La baja de una sucursal es lógica e incluye su dirección y sus contactos.
- Los contactos usan `ContactoService`, que maneja toda la jerarquía `Contacto` (correo y teléfono) y
  se reutiliza en Proveedores (E3-01).
- El seeder carga "Zero Indumentaria Deportiva S.A." (CUIT `30-71567890-6`) como sede central.

### Cómo se envían los correos

```
Zero ──(SMTP, usuario y clave)──► servidor SMTP (Gmail) ──► bandeja del destinatario
```

La app arma el correo (template Thymeleaf + logo) y lo entrega a un **servidor SMTP externo** con
Jakarta Mail (`spring-boot-starter-mail`). El servidor es el que lo reparte a la bandeja del destinatario.
Java no reparte correos por su cuenta: **sin un servidor SMTP configurado no sale ningún correo**.

- El proveedor no está en el código: el servidor, puerto, usuario, clave y TLS se cargan en
  **Configuración → Correo** (`/admin/configuracion/correo`, solo `JEFE`) y se guardan en la base.
  Cambiar de Gmail a otro proveedor (Brevo, SendGrid, Mailgun, Amazon SES) es cambiar esos datos.
- `EmailService` arma el `JavaMailSender` en cada envío con esa configuración: un cambio en la pantalla
  aplica sin reiniciar.
- La configuración **no la carga el seeder**, porque lleva credenciales reales. Como cada integrante tiene
  su propia base (`data/zero.db`), **cada uno la carga una vez en la suya**. Hace falta internet.
- La clave nunca se muestra en la pantalla; al editar, si se deja vacía se conserva la guardada. Se guarda
  sin cifrar en la base local (queda para la revisión de seguridad E5-06).

### Configurar Gmail (gratis)

1. Usar una cuenta de Gmail **del proyecto** (por ejemplo `zero.tienda.tp@gmail.com`), no una personal:
   todos los correos salen con esa cuenta como remitente.
2. En https://myaccount.google.com/security activar la **Verificación en 2 pasos**.
3. En https://myaccount.google.com/apppasswords crear una **clave de aplicación** (nombre: "Zero").
   Google muestra 16 letras una sola vez: copiarlas. **No es la contraseña de la cuenta**: Gmail no deja
   que una aplicación entre con la contraseña normal.
4. En **Configuración → Correo** cargar:

| Campo | Valor |
|---|---|
| Servidor SMTP | `smtp.gmail.com` |
| Puerto | `587` |
| Correo | la cuenta de Gmail |
| Clave | las 16 letras de la clave de aplicación, sin espacios |
| Usar TLS | marcado |

5. Guardar y usar **Enviar correo de prueba**. Es sincrónico a propósito: la pantalla muestra si Gmail lo
   aceptó o el error que devolvió. Si no llega, revisar spam.

Una cuenta de Gmail común permite unos **500 destinatarios por día**, suficiente para el TP. Para una
tienda real se usaría un proveedor de correo transaccional, sin cambiar código.

| Error en la prueba | Causa probable |
|---|---|
| `535 ... Username and Password not accepted` | Se usó la contraseña de la cuenta en lugar de la clave de aplicación, o la clave se revocó |
| `Connection refused` / `connect timed out` | Sin internet, puerto o servidor mal escrito, o un firewall bloquea el puerto 587 |
| `Todavía no se configuró el correo de la empresa.` | Falta guardar la cuenta de envío |

### Enviar correos desde otro issue

```java
// Ejemplo de E4-05: los datos se pasan ya armados (ver la advertencia de abajo).
emailService.enviar(usuario.getNombreUsuario(), "Tu compra fue confirmada", "compra-confirmada",
        Map.of("nombre", cliente.getNombre(), "identificador", orden.getIdentificadorCompra(),
                "total", orden.getTotal()));
```

- Es `@Async`: vuelve enseguida y, si el envío falla, lo registra en el log sin romper la operación que
  lo llamó (la compra se confirma igual aunque Gmail no responda).
- **Pasar valores simples o DTOs, no entidades con relaciones perezosas.** El envío corre en otro hilo,
  sin la sesión de Hibernate: si el template recorre, por ejemplo, `orden.detalles`, falla con
  `LazyInitializationException` y el correo no sale (solo queda el error en el log).
- El template va en `templates/email/{nombre}.html`, decora `email/base.html` y pone su contenido en
  `layout:fragment="contenido"` (copiar `email/prueba.html`). **Los correos usan estilos inline y tablas**,
  porque los clientes de correo ignoran el CSS externo: es la única excepción a la regla del template.
- Todos los templates reciben `asunto` y `empresa` (razón social, dirección, correo y teléfono de la sede
  central) sin que haya que pasarlos. El logo va embebido como `cid:logo`.
- **Links dentro del correo** (por ejemplo "Activá tu cuenta" en E2-05): no usar `@{...}`, porque en el
  envío no hay request. Armar la URL absoluta y pasarla como variable. Un link a `localhost:8080` solo
  abre en la máquina donde corre la app: para la demo alcanza.

### Probar sin enviar correos reales

Los tests (`mvnw test`) usan **GreenMail**, un servidor SMTP en memoria en el puerto 3025 (dependencia
solo de test): verifican el envío, el HTML y el logo sin que salga nada de la máquina. Ver
`CorreoIntegrationTest` y `EmailAsyncIntegrationTest`.

## Catálogo público E3-05

La home y las rutas `/catalogo/{categoriaId}` y
`/catalogo/{categoriaId}/{subCategoriaId}` muestran únicamente productos activos que tengan
stock mayor a cero y una vigencia de precio abierta. El menú público de escritorio y celular se
genera desde el árbol de categorías activas. Las páginas de catálogo incluyen breadcrumb, cards
responsive y paginación mediante el parámetro `page`.

La firma compartida con las siguientes issues es
`CatalogoService.listar(CatalogoFiltro)`, que devuelve objetos `ProductoCatalogoDTO` sin exponer
entidades JPA a la vista.
## ABM de proveedores E3-01

**Proveedores** (`/admin/proveedores`, `JEFE` y `ADMINISTRATIVO`): listado con buscador por razón social y
formulario con una lista dinámica de contactos. Cada fila tiene tipo (correo, celular o fijo), valor, ámbito
(`TipoContacto`) y observación; "Agregar contacto" suma una fila y "Quitar" la saca
(`static/js/proveedor-contactos.js`, que clona el `<template id="plantilla-contacto">` de la vista).

### Validaciones y reglas (RF24)

- Razón social obligatoria y única entre los proveedores activos, sin importar mayúsculas.
- Al menos un correo con formato válido y al menos un **celular para WhatsApp**.
- El celular se guarda **solo con dígitos**, en formato internacional: se puede escribir
  `+54 9 261 412-3456` y queda `5492614123456`. Tiene que tener entre 11 y 15 dígitos, sin 0 adelante, y los
  de Argentina empiezan con `549` y tienen 13 dígitos (sin el 0 del área ni el 15). Así `wa.me` abre el chat
  correcto.
- Los contactos se crean, modifican y dan de baja con `ContactoService`, el mismo de Empresa y Cliente.
- Al editar, cada fila lleva el id de su contacto: se **modifica ese contacto** en lugar de crear otro. Las
  filas quitadas se dan de baja y las nuevas se crean. Si un correo pasa a ser teléfono (o al revés), se da de
  baja el anterior y se crea uno nuevo, porque son clases distintas.
- La baja del proveedor es lógica e incluye sus contactos.

### Para otros issues

- `Proveedor.buscarCelularActivo()`: el celular al que se le escribe por WhatsApp. El botón del listado abre
  `https://wa.me/{telefono}`; lo pueden usar la compra a proveedor (E3-03) y la reposición (E5-04).
- `Proveedor.listarCorreoActivo()` y `listarTelefonoActivo()`: los contactos activos del proveedor.
- `ProveedorService.listarProveedorActivo()` para los selects de proveedor.
- El seeder carga 4 proveedores con sus contactos: Distribuidora Deportiva Cuyo S.A., Calzados y Textiles del
  Plata S.R.L., Indumentaria Atlética San Juan y Accesorios Fitness Andina.

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
