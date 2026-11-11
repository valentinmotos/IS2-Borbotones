# Dulce Mayor

Trabajo Integrador N°2 de IS2 · equipo Los Borbotones. Sistema de gestión para un mayorista de golosinas de Mendoza.

| Carpeta | Qué es |
|---|---|
| `backend/` | API REST Spring Boot 3.5 + PostgreSQL (Java 21) |
| `client-admin/` | Panel React de gestión (E0-02) |
| `client-public/` | Sitio público React (E0-02) |
| `docs/` | Enunciado, plan de trabajo y diagrama UML |

## Requisitos

- Java 21 (no hace falta instalar Maven: se usa el wrapper `mvnw`).
- PostgreSQL 16, local o en Docker.
- Node.js 22.12 o superior y npm (para los clientes React).

## Clientes React · E0-02

Dos aplicaciones independientes de Vite + React, en la rama de trabajo `e0-02`:

| Cliente | URL local | Rutas disponibles |
|---|---|---|
| Administración | http://localhost:5173 | `/login`, inicio por rol, `/guia`, `/componentes` |
| Sitio promocional | http://localhost:5174 | `/`, `/empresa`, `/como-comprar`, `/creditos`, `/login`, `/cuenta` |

Ambas tienen navegación responsive, menú móvil, recuperación de rutas desconocidas, enlace de salto al contenido y títulos por página. Las páginas de inicio funcionan sin el backend. Las tipografías y las fotografías de referencia se sirven localmente. No se simulan usuarios, stock ni ventas. Los ABM, login, catálogo y pedidos corresponden a los siguientes issues; E0-02 deja la estructura y el transporte HTTP preparados para integrarlos.

### Instalación y desarrollo

Desde `IntegradorN2/` (esta carpeta, no la raíz del repositorio):

```sh
npm ci
npm run dev
```

El comando inicia los dos clientes con recarga automática. También se pueden iniciar individualmente con `npm run dev -w client-admin` y `npm run dev -w client-public`. Los puertos son fijos: si están ocupados, el comando falla en lugar de cambiar la URL silenciosamente.

### Despliegue local del build de producción

```sh
npm run deploy:local
```

Compila ambas aplicaciones y sirve sus directorios `dist/` en los puertos 5173 y 5174, con recuperación de rutas SPA y proxy de API. Mantener el proceso abierto; `Ctrl+C` detiene ambos servidores. Para servir un build ya generado, usar `npm run preview`. Antes de cambiar de desarrollo a preview, detener el proceso anterior para liberar los puertos.

Este despliegue está limitado a la computadora local. Vite preview es una herramienta de verificación local, no el hosting público de producción. Docker y el despliegue completo del sistema corresponden a E0-09.

En Windows, después de `npm run build`, también se puede mantener el despliegue en segundo plano, sin una ventana de terminal abierta:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local.ps1 Start
powershell -ExecutionPolicy Bypass -File scripts/local.ps1 Status
powershell -ExecutionPolicy Bypass -File scripts/local.ps1 Stop
```

El script verifica los puertos y ambos inicios, guarda los logs en `.local/` (ignorada por Git) y solo detiene su propio proceso. No configura arranque automático con Windows.

### Configuración de los clientes

Cada aplicación incluye su propio `.env.example`. Copiarlo como `.env` **dentro de esa aplicación** cuando se necesite cambiar los valores. No hace falta crear un `.env` para usar los valores por defecto.

| Variable | Valor por defecto | Uso |
|---|---|---|
| `VITE_API_URL` | `/api` | URL base del cliente HTTP; puede ser una URL absoluta de la API. |
| `API_PROXY_TARGET` | `http://localhost:8080` | Destino de `/api` en desarrollo y preview. |
| `VITE_PUBLIC_URL` | `http://localhost:5174` | Enlace desde administración al sitio promocional. |
| `VITE_ADMIN_URL` | `http://localhost:5173` | Enlace desde el sitio promocional a administración. |

Las variables `VITE_*` son públicas y se incorporan al build: **nunca colocar secretos**. Después de cambiarlas, reiniciar desarrollo o recompilar para preview. Con la configuración por defecto, el navegador llama a `/api` en su mismo origen y Vite lo reenvía al backend, evitando depender de CORS durante el desarrollo local. Para una URL absoluta de otro origen, el backend de destino debe permitir CORS.

### Estructura y contrato HTTP

```text
client-admin/                 client-public/
  src/                          src/
    app/          rutas y layout
    components/   componentes transversales
    lib/api.js    instancia HTTP y sesión del cliente
    modules/      módulos con sus pages/ y components/
    main.jsx      entrada de React
packages/http/                transporte compartido y pruebas
scripts/serve-local.mjs       servidor de preview de ambos builds
tests/clients.test.mjs        integración de rutas, assets y proxy
```

Cada módulo consume `api` desde `src/lib/api.js`. Los interceptores agregan `Authorization: Bearer <token>` leyendo la sesión en cada solicitud. El token se guarda en `sessionStorage` con claves distintas (`dulcemayor.admin.token` y `dulcemayor.public.token`), se elimina ante un 401 y se notifica a la interfaz. Un 403 conserva la sesión. `ApiError` expone `message`, `status` y `campo`, respetando el contrato `{mensaje, campo}` del backend. El login React usa `{ skipAuth: true }` y guarda el token devuelto por la API. Al recargar, valida la sesión con `GET /api/auth/yo` antes de habilitar las rutas protegidas.

### Verificaciones

```sh
npm run lint              # ESLint para ambas aplicaciones y código compartido
npm test                  # 7 pruebas del transporte HTTP y sesiones
npm run test:integration  # compila; prueba rutas React, assets, fallback SPA y proxy
npm run build             # genera client-admin/dist y client-public/dist
```

Las pruebas de integración levantan una API aislada para verificar el proxy y el encabezado Bearer; no requieren PostgreSQL ni modifican datos del proyecto. Verifican el renderizado de las rutas React sin navegador. La revisión visual en un navegador se realiza por separado. `package-lock.json` fija las dependencias para instalaciones reproducibles con `npm ci`.

## Sistema de diseño · E0-03

Abrí [Sistema de diseño](http://localhost:5173/componentes) desde el menú de administración tras ejecutar `npm run dev`. También está disponible con `npm run deploy:local`. La galería funciona con datos de ejemplo en memoria y se reinicia al salir de la página. El registro de categorías muestra los ocho componentes en un flujo de trabajo. La sección «Referencia de componentes» indica dónde probar cada uno. La ficha del informe documenta el rediseño con Taste Skill y el origen de las [plantillas y sus licencias](docs/E0-03.md).

Los dos clientes cargan `packages/design/base.css` y `packages/design/components.css`. La base importa los tokens de `packages/design/tokens.css`, que definen la tipografía, la marca, el foco, los colores semánticos, el espaciado y el radio de los controles. Para reutilizar los componentes, importalos desde `packages/design/components.jsx`.

| Componente | Uso |
|---|---|
| `DataTable` | Recibe `caption`, `columns` con `key`, `label` y `render(row)` opcional, `rows`, `rowKey(row)` y un mensaje `empty` opcional. |
| `FormField` | Usalo en un `form` con `label`, los atributos del input y un `hint` o `error`. Para errores de API, pasá `mensaje` como `error` en el campo identificado por `campo`. |
| `ConfirmDialog` | Se controla con `open`, `title`, `children`, `onConfirm` y `onCancel`. Admite `confirmLabel`. Escape ejecuta la cancelación. Si se quita el control que abrió el modal, mové el foco a un elemento que siga disponible. |
| `StatusBadge` | Muestra el texto de estado en `children` con `tone` igual a `neutral`, `success`, `warning` o `danger`. |
| `Kpi` | En un `dl`, recibe `label`, `value` y un `detail` opcional. |
| `Alert` | Recibe el mensaje en `children`, los mismos tonos del badge y un `onDismiss` opcional. |
| `Pagination` | Recibe `page` desde 1, `pageCount` y `onPageChange(page)`. El módulo decide cómo recuperar o recortar las filas y ajusta la página si cambia el total. |
| `FilePicker` | Recibe `label`, `accept`, `maxBytes`, `hint` y `onChange(file, error)`. Devuelve el archivo seleccionado, o `null` si se quita o falla la validación local. La galería acepta JPG, PNG y WebP hasta 2 MiB. |

Para probar el conjunto, avanzá a la segunda página, agregá una categoría, repetí el nombre para ver el error y probá un archivo fuera del tipo admitido. Abrí «Quitar» y probá tanto cancelar con Escape como confirmar. Los KPI, la paginación y los avisos reflejan los cambios del listado.

## Cómo correrlo

**Todo con Docker:** ver [Docker · E0-09](#docker--e0-09) (`docker compose up --build -d`).

**Para desarrollar el servidor:**

1. Levantar solo PostgreSQL con el compose: `docker compose up -d db`.
2. Variables de entorno: copiar `.env.example` como `.env` y completarlo. Con los valores de `.env.example`, el servidor apunta a `localhost:5432/dulcemayor` con usuario y clave `postgres`.
3. Arrancar el servidor:
   ```
   cd backend
   mvnw.cmd spring-boot:run        (Windows)
   ./mvnw spring-boot:run          (Linux/Mac)
   ```
   La API queda en `http://localhost:8080`.
4. Tests (usan H2 en memoria, no necesitan PostgreSQL):
   ```
   cd backend
   mvnw.cmd test
   ```

## Base de datos

- Perfil `dev` (por defecto): PostgreSQL con `DB_URL`, `DB_USER` y `DB_PASSWORD`. Hibernate crea y actualiza las tablas a partir de las entidades (`ddl-auto=update`).
- Zona horaria: `DulceMayorApplication.main` fija `America/Argentina/Mendoza`. Sin eso, en Windows con zona Argentina el JVM manda `America/Buenos_Aires` y PostgreSQL 16 rechaza la conexión (`invalid value for parameter "TimeZone"`).
- Perfil `test`: H2 en memoria en modo PostgreSQL, con el esquema creado de cero en cada corrida.
- Datos iniciales: `config/DataSeeder` corre solo si **todas** las tablas están vacías. Para volver a cargarlos, vaciar la base (con Docker: `docker compose down -v` borra el volumen de la base).

## Estructura del backend

```
com.dulcemayor
 ├─ config        SecurityConfig, AsyncConfig, SchedulingConfig, DataSeeder
 ├─ entity        entidades JPA (todas extienden BaseEntity)
 │   └─ enums
 ├─ repository    repositorios Spring Data JPA (uno por entidad)
 ├─ service       lógica de negocio
 ├─ dto           records de request/response
 │   └─ mapper    MapStruct: CrudMapper y un mapper por ABM (E0-08)
 ├─ controller    @RestController bajo /api
 ├─ security      JWT (E1-01)
 ├─ scheduler     tareas programadas (E5-05)
 ├─ exception     excepciones de negocio y manejador global de errores
 └─ utils         TextoUtils, CuitUtils
```

## Entidades de ventas, pagos y rutas · E0-07

Entidades JPA y repositorios de `Pedido`, `DetallePedido`, `HistorialEstadoPedido`, `Factura`, `DetalleFactura`, `FormaDePago`, `Pago`, `CotizacionDolar`, `Ruta` y `ParadaRuta`, con los enums `EstadoPedido`, `EstadoFactura`, `TipoPago`, `MonedaPago` y `RutaEstado`. Los nombres son los del UML; los `idX` del diagrama son relaciones JPA (`@ManyToOne` / `@OneToOne`) con clave foránea.

| Tabla | Claves foráneas |
|---|---|
| `pedidos` | `punto_de_venta_id`, `preventor_id` |
| `detalles_pedido` | `pedido_id`, `producto_id` |
| `historial_estados_pedido` | `pedido_id`, `usuario_id` |
| `facturas` | `pedido_id` (único: un pedido tiene a lo sumo una factura) |
| `detalles_factura` | `factura_id`, `producto_id` |
| `pagos` | `factura_id`, `forma_de_pago_id` |
| `formas_de_pago`, `cotizaciones_dolar` | — (`cotizaciones_dolar.fecha` es única) |
| `rutas` | `preventor_id`, `distribuidor_id` (opcionales) |
| `paradas_ruta` | `ruta_id`, `punto_de_venta_id`, `pedido_id` (opcional) |

**Composiciones** (`@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)`): `Pedido.detalles`, `Factura.detalles` y `Ruta.paradas` (ordenadas por `orden`). Se guardan junto con el padre y una línea que se quita de la lista se borra. Para agregar, usar `agregarDetalle(...)` / `agregarParada(...)`, que completan las dos puntas de la relación.

Los montos son `double`, igual que en E0-05 (`VigenciaPrecio.precio`).

### Cómo probarlo

`mvnw.cmd test` → `VentasRepositoryTest` (`@DataJpaTest` sobre H2):
- lee `information_schema` y verifica que existen las 16 claves foráneas de la tabla;
- guarda un pedido con su detalle en cascada y verifica que quitar una línea la borra;
- factura con detalle y pago, y la consulta de facturas por minorista;
- cotización vigente (la de fecha más reciente), ruta con paradas ordenadas e historial de estados.

### Para otros issues

| Repositorio | Consultas | Lo usa |
|---|---|---|
| `PedidoRepository` | `findByEliminadoFalse()`, `findByEstadoAndEliminadoFalse(estado)`, `findByPreventor_IdAndEliminadoFalse(id)`, `findByPuntoDeVenta_IdAndEliminadoFalse(id)` | E4-01, E4-02 |
| `DetallePedidoRepository` | `findByPedido_IdAndEliminadoFalse(idPedido)` | E4-01 |
| `FacturaRepository` | `findByPedido_IdAndEliminadoFalse(idPedido)`, `findByEliminadoFalse()`, `findByEstadoAndEliminadoFalse(estado)`, `findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(idMinorista)` | E4-04, E4-06 |
| `PagoRepository` | `findByFactura_IdAndEliminadoFalse(idFactura)` | E4-05 |
| `CotizacionDolarRepository` | `findFirstByOrderByFechaDesc()` (última guardada), `findByFecha(fecha)`, `findAllByOrderByFechaDesc()` | E1-08 |
| `RutaRepository` | `findByFechaAndEliminadoFalse(fecha)`, `findByPreventor_IdAndEliminadoFalse(id)`, `findByDistribuidor_IdAndEliminadoFalse(id)` | E5-01 |

- `HistorialEstadoPedido` y `CotizacionDolar` heredan `eliminado` de `BaseEntity` aunque el UML no lo tenga: no se usa (nunca se dan de baja).
- `Factura.pedido` es único. Si E4 necesita volver a facturar un pedido cuya factura se anuló, hay que revisar esa restricción.

## Proyecto base E0-01

Esqueleto del servidor: dependencias (Web, Data JPA, Security, Validation, Mail, PostgreSQL, Lombok), perfiles, estructura de paquetes, `BaseEntity`, manejo global de errores y `DataSeeder`.

### Errores: respuesta JSON uniforme

Toda respuesta de error de la API tiene la misma forma (contrato "Errores" del kickoff):

```json
{ "mensaje": "Ya existe una categoría con ese nombre.", "campo": "nombre" }
```

`campo` es el dato del formulario que causó el error, o `null`. El cliente muestra `mensaje` tal cual.

| Situación | Código | Origen |
|---|---|---|
| Regla de negocio no cumplida | 400 | `ErrorServiceException` |
| Dato único repetido (nombre, código, CUIT…) | 409 | `DuplicadoException` |
| Registro inexistente o dado de baja | 404 | `RecursoNoEncontradoException` |
| DTO con `@Valid` inválido | 400 | primer campo inválido y su mensaje |
| JSON mal formado, parámetro faltante o de tipo incorrecto | 400 | |
| Ruta inexistente | 404 | |
| Método HTTP no soportado | 405 | |
| Sin sesión | 401 | Spring Security (`ErroresSeguridadHandler`) |
| Rol sin permiso | 403 | Spring Security (`ErroresSeguridadHandler`) |
| Archivo de más de 2 MB | 413 | |
| Cualquier otro error | 500 | mensaje genérico; el detalle va solo al log |

Nunca se devuelven stack traces.

### Seguridad

La API ya no está abierta: desde E1-01 exige JWT y aplica reglas por rol (ver la sección **Seguridad: JWT, BCrypt y roles · E1-01**). Se mantiene lo de E0-01: `STATELESS`, CSRF deshabilitado, `ErroresSeguridadHandler` como entry point y access denied handler, `/error` abierto y `@EnableMethodSecurity`.

### Cómo probarlo

- `mvnw.cmd test`: `DulceMayorApplicationTests` levanta la aplicación completa y `GlobalExceptionHandlerTest` verifica cada código de la tabla con su JSON.
- Manual: con PostgreSQL corriendo, `mvnw.cmd spring-boot:run`. El log tiene que mostrar la conexión a PostgreSQL (`HikariPool-1 - Start completed`) y `Base vacía: cargando datos iniciales.`. Después:
  ```
  curl -i http://localhost:8080/api/no-existe
  → 404 {"mensaje":"El recurso solicitado no existe.","campo":null}
  ```

### Para otros issues

- **Entidades:** extienden `com.dulcemayor.entity.BaseEntity`. Ya traen `id` (String UUID, generado, no editable) y `eliminado` (baja lógica, `false` por defecto). No declarar otro `@Id`.
- **Errores de negocio:** lanzar desde el service, nunca capturar en el controller:
  ```java
  throw new ErrorServiceException("El código postal debe tener 4 dígitos.", "codigoPostal"); // 400
  throw new DuplicadoException("Ya existe una categoría con ese nombre.", "nombre");         // 409
  throw new RecursoNoEncontradoException("No se encontró la categoría.");                   // 404
  ```
  Son no verificadas (`RuntimeException`): `@Transactional` deshace los cambios solo. El código copiado de IntegradorN1 con `throws ErrorServiceException` compila igual.
- **Validación de DTOs:** `@NotBlank(message = "El nombre es obligatorio.")` y similares en el record de request, más `@Valid` en el controller. El mensaje llega tal cual al cliente.
- **Datos de prueba:** completar el método de `DataSeeder` que le corresponde al issue (`cargarUbicacion` E1-03, `cargarEmpresa` / `cargarFormasDePago` E1-07, `cargarUsuarios` E1-01, `cargarCatalogo` E2-01…03, `cargarPuntosDeVenta` E2-06, `cargarPedidos` E6-04), pasando por los services.
- **Utilidades:** `TextoUtils.mismoNombre(a, b)` compara sin tildes ni mayúsculas (duplicados) y `TextoUtils.esCorreoValido`. `CuitUtils.esValido` / `CuitUtils.normalizar` validan y formatean CUIT. Las dos vienen de IntegradorN1.
- **Variables de entorno:** todas están listadas en `.env.example`. Si agregás una, sumala ahí.

## ABM base con Template Method · E0-08

Plantilla de ABM que copian todos los demás (E1-03, E1-07, E2-01, E2-05…) y ABM de **Categoría** completo como referencia, en la API y en el panel.

### Patrones

- **Template Method** · `service/AbstractCrudService<E, Q, S>` (clase abstracta) fija el algoritmo de cada operación y `CategoriaService` (clase concreta) redefine los ganchos:
  - Alta: `validar` → `nuevaEntidad` → `asignar` → guardar → `despuesDeGuardar` → DTO.
  - Modificación: buscar → `validar` → `asignar` → guardar → `despuesDeGuardar` → DTO.
  - Baja lógica: buscar → `antesDeEliminar` → `eliminado = true`.

  Los algoritmos son `protected final`; los métodos públicos (`crear`, `modificar`, `eliminar`, `buscar`, `listarActivos`) no son `final` para que funcione el proxy de `@Transactional` (mismo esquema que `CrudTemplate` de TP2 Ej. 4g). `controller/AbstractCrudController<Q, S>` hace lo mismo del lado REST.
- **DTO con MapStruct** · `dto/mapper/CrudMapper<E, Q, S>` (`aRespuesta`, `aRespuestas`, `actualizar`, que nunca copia `id` ni `eliminado`). MapStruct genera la implementación al compilar.
- `repository/BaseRepository<E>` (`@NoRepositoryBean`): `findByIdAndEliminadoFalse` y `findByEliminadoFalse(Sort)`, usados por la plantilla.

### Endpoints

Solo JEFE y ADMINISTRATIVO (E1-01); sin token responde 401 y con otro rol 403.

| Método | Ruta | Request → Response |
|---|---|---|
| GET | `/api/categorias` | → `200 [{id, nombre}]` activas, ordenadas por nombre |
| GET | `/api/categorias/{id}` | → `200 {id, nombre}` · `404` |
| POST | `/api/categorias` | `{nombre}` → `201 {id, nombre}` · `400` vacío · `409` duplicado |
| PUT | `/api/categorias/{id}` | `{nombre}` → `200` · `404` · `409` |
| DELETE | `/api/categorias/{id}` | → `204` (baja lógica) · `400` si tiene productos activos · `404` |

Reglas (de IntegradorN1): nombre obligatorio de hasta 120 caracteres; no puede repetirse entre las categorías activas sin importar mayúsculas, tildes ni espacios (`TextoUtils.mismoNombre`); una categoría dada de baja libera su nombre; no se da de baja una categoría con productos activos.

Pantalla: **Categorías** en el panel (`/categorias`). Tiene el listado paginado, un formulario de alta y edición que muestra debajo del campo los errores del servidor que traen `campo`, y la baja con modal de confirmación. El seeder carga Alfajores, Caramelos, Chicles, Chocolates y Gomitas.

### Cómo probarlo

- `mvnw.cmd test`:
  - `CategoriaServiceTest` (Mockito): alta, duplicado con tildes y mayúsculas, modificación conservando el propio nombre, baja con y sin productos, 404.
  - `CategoriaControllerTest` (`@WebMvcTest`): códigos HTTP y JSON de error.
  - `CategoriaIntegrationTest` (`@SpringBootTest` + H2): el ABM completo de punta a punta.
- Sin PostgreSQL, el servidor se puede levantar con H2: `mvnw.cmd spring-boot:run "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.profiles=test"`.
- Manual: con el servidor y `npm run dev` corriendo, entrar a <http://localhost:5173/categorias>, crear "Turrones" y después "turrónes": aparece *Ya existe una categoría con ese nombre.* debajo del campo. Editar y dar de baja desde la tabla.
  ```
  curl -i -X POST http://localhost:8080/api/categorias -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"nombre\":\"Alfajores\"}"
  → 409 {"mensaje":"Ya existe una categoría con ese nombre.","campo":"nombre"}
  ```

### Para otros issues: cómo armar un ABM nuevo

Copiar la estructura de Categoría:

1. **Repositorio:** `interface XRepository extends BaseRepository<X>` (en vez de `JpaRepository`).
2. **DTOs:** `record XRequest(...)` con Jakarta Validation y mensajes en español; `record XResponse(...)`. Nunca exponer la entidad.
3. **Mapper:** `@Mapper(componentModel = "spring") interface XMapper extends CrudMapper<X, XRequest, XResponse> {}`. Si el request trae ids de relaciones (`idDepartamento`), agregar `@Mapping(target = "departamento", ignore = true)` sobrescribiendo `actualizar`, y resolver la relación en `asignar`.
4. **Service:** `@Service @Transactional(readOnly = true) class XService extends AbstractCrudService<X, XRequest, XResponse>`. Implementar `validar(request, idActual)` (`idActual` es `null` en el alta), `nuevaEntidad()` y `mensajeNoEncontrado()`. Opcionales: `asignar` (normalizar o resolver relaciones; llamar a `super.asignar`), `antesDeEliminar` (hijos activos), `despuesDeGuardar`, `orden()`. Sumar los métodos con nombres del UML (`crearX`, `buscarX`, `listarXActivo`…) que delegan en la plantilla.
5. **Controller:** `@RestController @RequestMapping("/api/xs") class XController extends AbstractCrudController<XRequest, XResponse>`. Los endpoints propios (p. ej. localidades por departamento) se agregan en la misma clase.
6. **Pantalla:** `const xApi = crearCrudApi("/xs")` (`client-admin/src/lib/crudApi.js`) y una página en `src/modules/<modulo>/pages/` como `CategoriasPage.jsx`, con ruta en `App.jsx` y entrada en `AdminLayout.jsx`.
7. **Tests:** como mínimo, el caso duplicado con Mockito (`CategoriaServiceTest`).

Para usar la entidad desde otro service (p. ej. `ProductoService` necesita la `Categoria`), llamar a `categoriaService.buscarEntidad(id)`: devuelve la entidad activa o lanza 404. Los controllers nunca la usan.

## E1-04 · Dirección con mapa

Ingresá al panel como Jefe o Administrativo y abrí [Sistema de diseño → Dirección con mapa](http://localhost:5173/componentes#demo-direccion). Con los datos iniciales cargados, el ejemplo muestra Capital, Ciudad de Mendoza, el código postal 5500 y un punto en el mapa. Cambiá de departamento para probar la cascada. Elegí otra localidad y comprobá que su código postal se completa automáticamente. Podés marcar un punto con un clic o buscar la calle y numeración de la localidad seleccionada y elegir una coincidencia.

Para incluirlo en un formulario React, importá `DireccionField` desde `packages/address/DireccionField.jsx` y usá el cliente HTTP del portal:

```jsx
<DireccionField api={api} value={direccion} onChange={setDireccion} />
```

El componente es controlado. Para editar una dirección, inicializá `value` con `departamentoId`, `localidadId`, los campos de texto y las coordenadas numéricas `latitud` y `longitud`. La localidad seleccionada provee el código postal. El formulario anfitrión se encarga de validar que haya un punto y guardar la dirección.

La búsqueda se ejecuta solo al pulsar el botón y envía calle, numeración y localidad a través de la API del proyecto. El servidor guarda resultados en caché y limita las consultas externas a una por segundo por instancia. Se puede configurar otro servidor compatible con `NOMINATIM_URL`. Para el servicio público, consultá la [política de uso de Nominatim](https://operations.osmfoundation.org/policies/nominatim/).

## Empresa, sucursales y formas de pago · E1-07

El panel de administración incluye el ABM de empresa y sucursales en `/empresa`. La empresa valida el CUIT argentino y lo almacena normalizado. Cada sucursal guarda dirección, localidad, coordenadas y una foto subida por el servicio E1-05. El formulario muestra la ubicación en OpenStreetMap y permite abrir el punto en el mapa.

Las formas de pago se administran en la misma pantalla. La transferencia requiere alias; las opciones activas se devuelven en el endpoint público para la vidriera. Desactivar aplica baja lógica y se puede volver a activar desde la tabla de administración.

| Método | Ruta | Uso |
|---|---|---|
| GET / POST / PUT / DELETE | `/api/empresas` y `/api/empresas/{id}` | ABM de razón social y CUIT |
| GET / POST / PUT / DELETE | `/api/sucursales` y `/api/sucursales/{id}` | ABM de sucursales y dirección; admite `/empresa/{id}` |
| GET / POST / PUT / DELETE | `/api/formas-de-pago` y `/api/formas-de-pago/{id}` | ABM de formas activas |
| GET | `/api/formas-de-pago/administracion` | Lista activas e inactivas para el panel |
| PATCH | `/api/formas-de-pago/{id}/activar` | Reactiva una forma dada de baja |
| GET | `/api/publico/empresa`, `/api/publico/sucursales`, `/api/publico/formas-de-pago` | Datos activos para el sitio público |

El seeder crea Dulce Mayor, la sede central con foto y las tres formas de pago de demostración después de cargar las localidades. Los endpoints de administración requieren rol JEFE o ADMINISTRATIVO.

Las pruebas `EmpresaServiceTest`, `SucursalServiceTest` y `FormaDePagoServiceTest` cubren el formato del CUIT, CUIT inválido, sede central duplicada, alias obligatorio y el alta de un medio de pago activo. Para ejecutarlas: `mvnw.cmd -Dtest=EmpresaServiceTest,SucursalServiceTest,FormaDePagoServiceTest test` desde `backend`.

## E2-01 · ABM de productos

Ingresá al panel como Jefe o Administrativo y abrí [Productos](http://localhost:5173/productos). Completá código, nombre, categoría, presentación y unidades por bulto o pack. Podés agregar una descripción y tenés que adjuntar una imagen JPG, PNG o WEBP de hasta 2 MB. Al editar, el producto conserva la imagen actual si no elegís otra.

El listado admite búsqueda por código o nombre y filtro por categoría. La baja es lógica y el código queda reservado para mantener la identificación del producto. La API normaliza el código y devuelve `409` con `campo: "codigo"` ante un duplicado.

- `GET /api/productos` lista los productos activos. `GET /api/productos/buscar` admite `busqueda` y `categoriaId`.
- `POST /api/productos` y `PUT /api/productos/{id}` reciben `multipart/form-data`. Los campos de texto son `codigo`, `nombre`, `descripcion`, `presentacion`, `unidadesPorBulto` y `categoriaId`. El archivo se envía en `imagen`. Los cambios del producto y su nueva imagen se confirman juntos.
- `GET /api/productos/{id}` consulta un producto activo y `DELETE /api/productos/{id}` lo da de baja.

## E2-02 · Stock y movimientos de almacén

Cada producto tiene un saldo (`Stock`) y cada cambio queda registrado como un `MovimientoStock` con fecha, tipo (`INGRESO`, `AJUSTE` o `EGRESO`) y cantidad con signo. Un producto sin movimientos tiene saldo 0. **El saldo nunca queda negativo:** un movimiento que lo dejaría por debajo de cero se rechaza con `400` y no se registra. Dos movimientos simultáneos del mismo producto se serializan con un bloqueo de escritura sobre el producto. Los movimientos no se editan ni se borran: un error se corrige con un ajuste.

### Endpoints (JEFE y ADMINISTRATIVO)

- `GET /api/stock`: saldo de cada producto activo (`productoId`, `codigo`, `nombre`, `presentacion`, `cantidadActual`, `sinStock`).
- `GET /api/stock/{productoId}`: saldo de un producto.
- `GET /api/stock/{productoId}/movimientos`: historial, del más nuevo al más viejo.
- `POST /api/stock/ingresos` con `{"productoId": "...", "cantidad": 20, "observacion": "Remito 123"}` → `201` con el saldo nuevo.
- `POST /api/stock/ajustes` con `{"productoId": "...", "diferencia": -3, "observacion": "Rotura"}` → `201`. Si el saldo quedaría negativo: `400` con `campo: "diferencia"`.

### Cómo probarlo

1. Ingresá como Jefe o Administrativo y abrí [Stock y almacén](http://localhost:5173/stock). Los productos sin movimientos aparecen con 0 y una alerta de stock cero.
2. Registrá un ingreso de 20 unidades: el saldo pasa de 0 a 20 y la alerta desaparece para ese producto.
3. Cargá un ajuste de -21: la pantalla lo bloquea y la API lo rechaza. Un ajuste de -5 deja el saldo en 15.
4. El historial del producto muestra los dos movimientos.

Pruebas: `StockServiceTest` (Mockito) y `StockIntegrationTest` (criterio de aceptación de punta a punta, validaciones y `403` para un preventor). Para ejecutarlas: `mvnw.cmd -Dtest=StockServiceTest,StockIntegrationTest test` desde `backend`.

### Para otros issues

- `StockService.hayStock(idProducto, cantidad): boolean` para el catálogo (E3-01) y el estado `SIN_STOCK` del pedido (E4-01).
- `StockService.descontar(idProducto, cantidad[, observacion])` para la facturación (E4-04): registra un `EGRESO` y lanza `ErrorServiceException` si la cantidad supera el saldo. Corre en la transacción de quien lo llama.
- `StockService.reponer(idProducto, cantidad, observacion)` y `buscarStockPorProducto(idProducto)` según la firma del UML.

## E2-03 · Precios mayoristas con vigencias

Cada producto tiene un historial de precios (`VigenciaPrecio`). Al cargar un precio con su fecha desde, el precio vigente se cierra automáticamente el día anterior, así las vigencias nunca se superponen. **El precio vigente es la vigencia sin fecha de cierre.** Las vigencias no se editan ni se borran: un error se corrige cargando un precio nuevo.

Reglas: el precio debe ser mayor que cero; la fecha desde no puede ser anterior a hoy y tiene que ser posterior al inicio del precio vigente (si no, `400` con `campo: "fechaDesde"`). Dos altas simultáneas del mismo producto se serializan con un bloqueo de escritura sobre el producto.

### Endpoints (JEFE y ADMINISTRATIVO)

- `GET /api/productos/{id}/precios`: historial, del más nuevo al más viejo. Cada ítem trae `id`, `productoId`, `fechaDesde`, `fechaHasta` (null si está abierto), `precio` y `vigente`.
- `GET /api/productos/{id}/precios/vigente`: precio vigente; `404` si el producto todavía no tiene precio.
- `POST /api/productos/{id}/precios` con `{"fechaDesde": "2026-11-10", "precio": 15000}` → `201` con la vigencia creada.

### Cómo probarlo

1. Ingresá como Jefe o Administrativo, abrí [Productos](http://localhost:5173/productos) y tocá **Precios** en un producto.
2. Cargá tres precios con fechas crecientes (por ejemplo hoy, hoy + 10 y hoy + 20 días).
3. El historial muestra tres filas: cada una cierra el día antes de que empiece la siguiente y solo la última aparece como **Vigente**.

Pruebas: `VigenciaPrecioServiceTest` (Mockito) y `VigenciaPrecioIntegrationTest` (criterio de aceptación de punta a punta, validaciones y `403` para un preventor). Para ejecutarlas: `mvnw.cmd -Dtest=VigenciaPrecioServiceTest,VigenciaPrecioIntegrationTest test` desde `backend`.

### Para otros issues

- `VigenciaPrecioService.buscarPrecioVigente(idProducto): Optional<Double>` para el catálogo (E3-01), el presupuesto del chatbot (E3-04) y el precio unitario del pedido (E4-01). Vacío si el producto no tiene precio.
- `VigenciaPrecioService.buscarVigenciaPrecioVigente(idProducto): VigenciaPrecioResponse` (lanza `RecursoNoEncontradoException` si no hay precio).

## E2-06 · Puntos de venta y geolocalización

El preventor da de alta un comercio (`PuntoDeVenta`) de un minorista con su dirección y su ubicación en el mapa, usando el componente `DireccionField` de E1-04. El preventor del alta sale siempre del token. **La zona es el departamento** de la localidad de la dirección (el modelo no tiene otra entidad de zona). Las coordenadas son obligatorias: sin un punto marcado en el mapa el alta se rechaza con `400`.

### Endpoints

- `GET /api/puntos-de-venta?departamentoId=…` (JEFE, ADMINISTRATIVO, PREVENTOR): comercios activos, de todas las zonas o de una. Cada ítem trae nombre, minorista, preventor, dirección, localidad, departamento y `latitud`/`longitud`.
- `GET /api/puntos-de-venta/{id}` (mismos roles).
- `GET /api/puntos-de-venta/minoristas` (mismos roles): minoristas activos para elegir el responsable.
- `POST /api/puntos-de-venta` (solo PREVENTOR) con `{"nombreComercio", "minoristaId", "localidadId", "calle", "numeracion", "barrio", "manzanaPiso", "casaDepartamento", "referencia", "latitud", "longitud"}` → `201`. Un usuario sin perfil de preventor recibe `400` con un mensaje claro.

### Cómo probarlo

1. Ingresá como `preventor@dulcemayor.com.ar` y abrí [Puntos de venta](http://localhost:5173/puntos-de-venta). Los datos de demo ya traen el "Kiosco Centro" en Capital.
2. Cargá un kiosco nuevo: nombre, minorista, departamento, localidad, calle y numeración, y marcá el punto en el mapa (o buscá la dirección).
3. Al guardar, la pantalla cambia a la zona del comercio y el marcador aparece en el mapa y en la tabla.
4. Como Jefe o Administrativo, la misma pantalla muestra el mapa y la tabla, sin el formulario de alta.

Pruebas: `PuntoDeVentaServiceTest` (Mockito) y `PuntoDeVentaIntegrationTest` (criterio de aceptación de punta a punta con un preventor real, coordenadas faltantes, minorista inexistente y `403` para administrativo y distribuidor). Para ejecutarlas: `mvnw.cmd -Dtest=PuntoDeVentaServiceTest,PuntoDeVentaIntegrationTest test` desde `backend`.

### Para otros issues

- `PuntoDeVentaService.listarPuntoDeVentaPorPreventor(idPreventor)` para elegir un comercio existente en el portal del preventor (E4-02).
- `PuntoDeVentaService.listarPuntoDeVentaPorMinorista(idMinorista)` para el perfil del minorista (E2-07).
- `PreventorService.buscarPreventorPorUsuario(idUsuario)`: el preventor del token.
- `DireccionService.crearDireccion(...)`: alta de una dirección con validación de coordenadas, reutilizable por E2-07.

## Docker · E0-09

`docker-compose.yml` levanta la base, el servidor y los dos clientes. Ollama es opcional.

| Servicio | Imagen | URL local | Notas |
|---|---|---|---|
| `db` | `postgres:16-alpine` | `localhost:5432` | volumen `dulcemayor-db`; healthcheck con `pg_isready` |
| `backend` | `backend/Dockerfile` | <http://localhost:8080/api> | arranca cuando `db` está sana; perfil `dev` |
| `client-admin` | `client-admin/Dockerfile` | <http://localhost:5173> | nginx; `/api` → `backend:8080` |
| `client-public` | `client-public/Dockerfile` | <http://localhost:5174> | ídem |
| `ollama` | `ollama/ollama` | `localhost:11434` | solo con `--profile ollama` |

### Cómo se usa

```
cd IntegradorN2
copy .env.example .env                       (Windows; en Linux/Mac: cp .env.example .env)
docker compose up --build -d                 levanta todo (la primera vez tarda unos minutos)
docker compose ps                            estado de los servicios
docker compose logs -f backend               log del servidor
docker compose down                          baja los contenedores; los datos quedan en el volumen
docker compose down -v                       baja todo y BORRA la base (el seeder vuelve a cargar al subir)
docker compose --profile ollama up -d        también levanta Ollama
docker compose exec ollama ollama pull <modelo>   descarga el modelo del chatbot (E3-04)
```

Sin `.env` (o sin `DB_PASSWORD`), `docker compose up` se corta con *Falta DB_PASSWORD - copiá .env.example como .env y completalo*: la clave no está escrita en el compose. Los puertos publicados se cambian con `DB_PORT`, `BACKEND_PORT`, `ADMIN_PORT` y `PUBLIC_PORT` en el `.env`. Mientras corre el compose, `npm run dev` y `mvnw spring-boot:run` chocan con esos puertos: para desarrollar conviene `docker compose up -d db` y nada más.

### Cómo está armado

- **Servidor:** multi-stage. `maven:3.9-eclipse-temurin-21` compila (sin tests, porque los corre Jenkins) y `eclipse-temurin:21-jre` ejecuta el jar con un usuario sin privilegios. Se usa el Maven de la imagen y no `./mvnw`, para no depender de los finales de línea CRLF de Windows. `DB_URL` se arma en el compose apuntando al servicio `db`.
- **Clientes:** se construyen con contexto en `IntegradorN2/` porque son workspaces de npm y usan `packages/`. `node:22-alpine` corre `npm ci` y `vite build`; `nginx:alpine` sirve el `dist`. Los dos usan `docker/nginx-cliente.conf`:
  - las rutas de React Router devuelven `index.html`;
  - `/api/` se reenvía a `backend:8080`, así el navegador ve un solo origen y no hace falta CORS;
  - `/assets/` se cachea un año (los nombres llevan hash).
- Las variables `VITE_*` se pasan como `args` al construir y quedan dentro del JavaScript: **nunca** poner secretos ahí.
- `.env` no se versiona (`.gitignore`) y tampoco entra en las imágenes (`.dockerignore`).

### Cómo probarlo

Criterio: `docker compose up` levanta todos los servicios y la API responde.

```
docker compose up --build -d
docker compose ps                                   db (healthy), backend, client-admin y client-public "Up"
curl http://localhost:8080/api/categorias           401 en JSON (API sobre PostgreSQL, protegida desde E1-01)
curl http://localhost:5173/api/categorias           401 (API a través del nginx del panel)
curl http://localhost:5173/categorias               200 con el index.html (ruta de React servida por nginx)
curl http://localhost:5174/                         200 (sitio promocional)
docker compose down && docker compose up -d         los datos siguen; el log dice "La base ya tiene datos"
```

Hasta que Spring termina de iniciar (unos 10 segundos), nginx responde `502` en `/api`.

### Para otros issues

- **Variable de entorno nueva:** sumarla en `.env.example` y en `environment` del servicio `backend` (`NOMBRE: ${NOMBRE:-}`).
- **E1-09 (Jenkins):** construye las imágenes con `docker compose build` desde `IntegradorN2/`.
- **E3-04 (chatbot):** dentro de Docker, Ollama está en `http://ollama:11434` (valor por defecto de `OLLAMA_URL` en el compose); el modelo se elige con `OLLAMA_MODEL`.
- **E1-05 (imágenes):** nginx ya acepta cuerpos de hasta 3 MB en `/api`, así el límite de 2 MB lo controla el servidor y responde 413 con su JSON.

## Seguridad: JWT, BCrypt y roles · E1-01

Login con correo y clave, claves con BCrypt, JWT con vencimiento, cierre de sesión, clave por defecto `dulcemayor` con cambio obligatorio y reglas por rol validadas en el servidor.

Reutiliza la parte JWT de TP2 Ej. 5d (`JwtServicio`, `JwtFiltro`, hash ficticio en el login, cierre de sesión por versión del token) y las reglas de usuario de IntegradorN1 (`UsuarioService`: BCrypt, `LARGO_MINIMO_CLAVE`, validación de correo y clave, mensajes). IntegradorN1 usaba form login con sesión; acá la API es sin estado.

### Cómo funciona

- **Claves:** `PasswordEncoder` BCrypt (`SecurityConfig`). La base guarda solo el hash (`$2a$…`, 60 caracteres). Los correos se guardan sin espacios y en minúsculas.
- **Token:** `security/JwtService` firma con HS256 (`JWT_SECRET`, al menos 32 caracteres) y vence a los `JWT_EXPIRATION` segundos (una hora por defecto). Claims: `sub` = id del usuario, `rol`, `version`. Con `JWT_SECRET` vacío usa una clave al azar (solo desarrollo: reiniciar el servidor cierra todas las sesiones).
- **Filtro:** `security/JwtFiltro` lee `Authorization: Bearer <token>`, valida firma y vencimiento y comprueba en la base que el usuario exista, no esté dado de baja, esté activo y que la versión del token coincida con `Usuario.versionToken`. Si algo falla, la petición sigue sin autenticar y termina en 401.
- **Cierre de sesión:** `POST /api/auth/logout` incrementa `versionToken`, así todos los tokens anteriores del usuario dejan de servir. Cambiar la clave también lo incrementa.
- **Clave temporal:** el alta de empleados (`UsuarioService.crearUsuario(correo, rol)`) usa `dulcemayor` con `claveTemporal = true`. El login responde `debeCambiarClave: true` y, hasta cambiarla, el filtro no le da el rol sino la autoridad `CLAVE_TEMPORAL`: solo puede usar `/api/auth/**`; el resto responde 403 *Tenés que cambiar tu clave antes de continuar.*
- **CORS:** solo los orígenes de `CORS_ORIGINS` (por defecto `http://localhost:5173` y `http://localhost:5174`).
- **Desvío del UML:** `Usuario.versionToken` (int) no está en el diagrama; hace falta para invalidar tokens al cerrar sesión. `UsuarioService.login` devuelve el `Usuario` como dice el UML; `AuthService` emite el token y le devuelve DTOs al controller.

### Reglas por rol

En `config/SecurityConfig`; la primera regla que coincide gana:

| Ruta | Acceso |
|---|---|
| `POST /api/auth/login`, `/api/publico/**` | público |
| `/api/auth/**` | cualquier usuario autenticado (también con clave temporal) |
| `/api/categorias/**` | JEFE, ADMINISTRATIVO |
| `/api/minorista/**` | MINORISTA |
| resto de `/api/**` | personal interno: JEFE, ADMINISTRATIVO, PREVENTOR, DISTRIBUIDOR |
| cualquier otra ruta | denegada |

El **JEFE** tiene además los permisos de ADMINISTRATIVO y PREVENTOR (`RoleHierarchy`, que aplica también a `@PreAuthorize`). Por la regla general, un **minorista recibe 403 en todo endpoint interno**, aunque nadie lo haya marcado.

### Endpoints

| Método | Ruta | Acceso | Request → Response |
|---|---|---|---|
| POST | `/api/auth/login` | público | `{nombreUsuario, clave}` → `200 {token, rol, debeCambiarClave, nombreUsuario, expiraEnSegundos}` · `400` campos vacíos · `401` *Correo o clave incorrectos.* |
| PUT | `/api/auth/clave` | autenticado | `{claveActual, nuevaClave, confirmarClave}` → `200` igual que el login, con un **token nuevo** y `debeCambiarClave: false` · `400` con `campo` |
| GET | `/api/auth/yo` | autenticado | → `200 {id, nombreUsuario, rol, debeCambiarClave}` |
| POST | `/api/auth/logout` | autenticado | → `204` |

Sin token, o con un token vencido, inválido o de una sesión cerrada: `401 {"mensaje":"Tenés que iniciar sesión para acceder a este recurso.","campo":null}`. Rol sin permiso: `403 {"mensaje":"No tenés permisos para realizar esta acción.","campo":null}`.

Reglas de clave: al menos 8 caracteres, como máximo 72 bytes (límite de BCrypt), la confirmación tiene que coincidir y la nueva tiene que ser distinta de la actual.

### Usuarios de demo

`DataSeeder.cargarUsuarios()` (solo con la base vacía):

| Correo | Rol | Clave |
|---|---|---|
| `jefe@dulcemayor.com.ar` | JEFE | `Demo1234!` |
| `administrativo@dulcemayor.com.ar` | ADMINISTRATIVO | `Demo1234!` |
| `preventor@dulcemayor.com.ar` | PREVENTOR | `Demo1234!` |
| `distribuidor@dulcemayor.com.ar` | DISTRIBUIDOR | `Demo1234!` |
| `minorista@dulcemayor.com.ar` | MINORISTA | `Demo1234!` |
| `nuevo@dulcemayor.com.ar` | PREVENTOR | `dulcemayor` (temporal: obliga a cambiarla) |

Las claves de demo no son temporales, para no tener que cambiarlas en cada base nueva. Por ahora son solo cuentas de `Usuario`; los datos personales (`Persona`, `Empleado`, `Minorista`) los agregan E2-04 y E2-05. Si la base ya tenía datos de antes de E1-01, vaciarla (`docker compose down -v`) para que se carguen.

### Cómo probarlo

- `mvnw.cmd test`:
  - `UsuarioServiceTest` (Mockito + BCrypt real): clave por defecto encriptada y temporal, duplicados, login, cuenta sin activar, cambio de clave y cierre de sesión.
  - `JwtServiceTest`: claims, firma con otra clave, token alterado, configuración inválida.
  - `SeguridadIntegrationTest` (`@SpringBootTest` + H2): el **criterio de aceptación** de punta a punta (minorista con 403 en endpoints internos, primer ingreso con `dulcemayor` que obliga a cambiar la clave, hash BCrypt en la base), más reglas por rol, logout, usuario dado de baja y CORS.
- Manual, con la base vacía y el servidor corriendo:
  ```
  curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"nombreUsuario\":\"minorista@dulcemayor.com.ar\",\"clave\":\"Demo1234!\"}"
  → 200 {"token":"eyJ...","rol":"MINORISTA","debeCambiarClave":false,...}
  curl -i http://localhost:8080/api/categorias -H "Authorization: Bearer <token del minorista>"
  → 403 {"mensaje":"No tenés permisos para realizar esta acción.","campo":null}

  curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"nombreUsuario\":\"nuevo@dulcemayor.com.ar\",\"clave\":\"dulcemayor\"}"
  → 200 {...,"debeCambiarClave":true}
  curl -i -X PUT http://localhost:8080/api/auth/clave -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"claveActual\":\"dulcemayor\",\"nuevaClave\":\"MiClave2026\",\"confirmarClave\":\"MiClave2026\"}"
  → 200 con un token nuevo y "debeCambiarClave":false
  ```
  En PostgreSQL, `select nombre_usuario, clave, clave_temporal from usuarios;` muestra solo hashes BCrypt.

### Para otros issues

- **E1-02 (login React):** el personal inicia sesión en el panel administrativo y el minorista en la vidriera. Cada rol llega a su inicio: `/jefatura`, `/administracion`, `/preventa`, `/distribucion` o `/cuenta`. Jefe y Administrativo acceden a Categorías, Ubicación y Sistema de diseño. Todo el personal tiene acceso a la Guía de trabajo. El menú lateral y las guardas del panel usan la misma lista de permisos. Una URL de otro rol muestra **Acceso denegado**.
- Si el servidor marca `debeCambiarClave`, el portal lleva a `/cambiar-clave` antes de mostrar los módulos y reemplaza el token con el que devuelve `PUT /api/auth/clave`. Al cerrar sesión, llama a `POST /api/auth/logout` y borra el token local.
- **Identidad de quien llama:** nunca recibir el id del usuario por URL o cuerpo; sacarlo del token:
  ```java
  @GetMapping("/mis-pedidos")
  public List<PedidoResponse> misPedidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
      return pedidoService.listarPedidoPorUsuario(usuario.id());
  }
  ```
  `UsuarioAutenticado` trae `id`, `nombreUsuario`, `rol` y `claveTemporal`.
- **Reglas por rol de un recurso nuevo:** sumar una línea en `SecurityConfig` antes de la regla general de `/api/**`, p. ej. `.requestMatchers(HttpMethod.POST, "/api/pedidos/**").hasAnyRole("JEFE", "PREVENTOR")`. Para reglas finas sobre un método, `@PreAuthorize("hasRole('JEFE')")`. Lo que no se marque queda para el personal interno, nunca para el minorista.
- **Endpoints del minorista (E2-07, E4…):** van bajo `/api/minorista/**`. **Endpoints públicos** (catálogo, sucursales, cotización, chatbot, imágenes): bajo `/api/publico/**`.
- **E2-04 (registro de minoristas):** el portal público ofrece `/registro` con nombre, apellido, fecha de nacimiento, tipo y número de documento, correo y clave. `POST /api/publico/registro` crea el usuario inactivo, el perfil `Minorista` y el contacto de correo; envía un código de seis dígitos por SMTP que vence a los 15 minutos. `POST /api/publico/registro/activar` valida el correo y el código; hay hasta cinco intentos y `POST /api/publico/registro/reenviar` permite solicitar otro código con un intervalo mínimo de 60 segundos. El código se guarda con BCrypt. El login de una cuenta pendiente responde 401 *Tu cuenta todavía no está activada…*.
- **E2-05 (ABM de usuarios internos):** `/usuarios` y `/api/usuarios/internos` son exclusivos de Jefe. El alta permite Administrativo, Preventor y Distribuidor, con clave `dulcemayor` y cambio obligatorio. Preventores y Distribuidores incluyen nombre, apellido, fecha de nacimiento, tipo y número de documento y legajo; Administrativo solo requiere correo y rol. El reseteo vuelve a la clave temporal e invalida tokens anteriores. Se pueden editar roles, excepto el cambio directo entre Preventor y Distribuidor porque cada uno tiene una entidad de perfil distinta; nunca se permite cambiar el rol o dar de baja al último Jefe activo. La baja es lógica. El controller usa `AbstractCrudController` y el service `AbstractCrudService` (Template Method).
- **Tests con seguridad:** en `@WebMvcTest`, `@Import({ SecurityConfig.class, ErroresSeguridadHandler.class, JwtService.class })`, `@MockitoBean UsuarioRepository` y `@WithMockUser(roles = "ADMINISTRATIVO")` (ver `CategoriaControllerTest`). En `@SpringBootTest` alcanza con `@WithMockUser`, o con un token real como en `SeguridadIntegrationTest`.

## Servicio de correo asíncrono

`EmailService.enviar(destino, asunto, plantilla, variables)` procesa una plantilla Thymeleaf ubicada en `backend/src/main/resources/templates/` y manda el mensaje en segundo plano. Por ejemplo, `plantilla = "correo/prueba"` usa `templates/correo/prueba.html`. Pasá las variables de la plantilla en el mapa; el correo de prueba admite `nombre`, `mensaje`, `url` y `etiquetaEnlace`.

El envío usa las variables SMTP `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM` del `.env`. En desarrollo podés completar esos valores con credenciales de Mailtrap; para Gmail usá una clave de aplicación. Si el envío falla, el servicio registra el error y la operación que lo llamó continúa. `mvnw.cmd test` ejecuta `EmailServiceTest` con un servidor SMTP simulado.

## Cotización del dólar con Adapter · E1-08

- **Adapter:** `service/CotizacionProvider` (Target, contrato `obtenerVigente()`) y `service/adapter/DolarApiAdapter` (Adapter de DolarAPI, `DOLARAPI_URL`). Usa timeout de 3 segundos y pasa la fecha de UTC a la hora de Mendoza. Si falla, devuelve vacío y lo registra en el log.
- **Servicio:** `CotizacionDolarService.buscarCotizacionVigente()` guarda una cotización por día en `CotizacionDolar` y la cachea una hora (Caffeine, `config/CacheConfig`). Si la API falla, devuelve la última guardada; si no hay ninguna, responde 404. `listarCotizacion()` devuelve el historial.
- **Tarea programada:** `CotizacionDolarScheduler` actualiza la cotización cada hora (`app.cotizacion.intervalo-ms`). No corre en el perfil `test`.
- **Endpoint público:** `GET /api/publico/cotizacion` → `200 {"fecha":"2026-11-09","valorVenta":1450.0}` · `404` si nunca se pudo obtener.
- **Tests:** `CotizacionDolarServiceTest` (criterio: vigente y respaldo con la última guardada) y `CotizacionDolarIntegrationTest` (endpoint sin token y caché), los dos con DolarAPI mockeada.
- **Para otros issues (E3-02, E3-03, E4-05):** usar `cotizacionDolarService.buscarCotizacionVigente()`; no llamar al provider directo.
