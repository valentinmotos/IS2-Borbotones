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

## E2-07 · Perfil del minorista

El minorista autenticado edita sus datos personales (nombre, apellido y fecha de nacimiento; tiene que ser mayor de 18) y su **dirección de entrega** con el componente `DireccionField` de E1-04, y puede cambiar su clave cuando quiera. El documento y el correo se muestran pero no se editan: identifican a la persona y a su cuenta. El minorista sale siempre del token, así nadie puede ver ni modificar el perfil de otro.

**Desvío del UML:** el diagrama no le da dirección al `Minorista`. Se agregó `Minorista.direccionEntrega` (relación opcional con `Direccion`, columna `direccion_entrega_id`), porque al registrarse todavía no la tiene. La primera vez que guarda el perfil se crea la dirección; después se modifica la misma.

### Endpoints (MINORISTA)

- `GET /api/minorista/perfil`: datos del minorista, correo, documento y dirección de entrega (`departamentoId`, `localidadId`, `localidad`, `calle`, …, `latitud`, `longitud`; null si todavía no cargó una).
- `PUT /api/minorista/perfil` con `{"nombre", "apellido", "fechaNacimiento", "localidadId", "calle", "numeracion", "barrio", "manzanaPiso", "casaDepartamento", "referencia", "latitud", "longitud"}` → `200` con el perfil actualizado.
- Cambio de clave voluntario: `PUT /api/auth/clave` (E1-01), que devuelve un token nuevo.
- Para usar el componente de dirección, el minorista puede hacer `GET` de `/api/departamentos`, `/api/departamentos/{id}/localidades`, `/api/localidades/**` y `/api/geocodificacion`. Las altas, modificaciones y bajas siguen siendo internas.

### Cómo probarlo

1. Ingresá en el [sitio público](http://localhost:5174/login) con `minorista@dulcemayor.com.ar` y abrí **Mi cuenta → Mis datos**. El minorista de demo ya trae una dirección en el centro.
2. Cambiá el nombre, elegí otro departamento y localidad (por ejemplo Godoy Cruz), marcá el punto en el mapa y guardá.
3. Recargá la página: los datos y la dirección siguen en la localidad elegida.
4. En **Cambiar clave**, ingresá la actual y una nueva; cerrá sesión y entrá con la clave nueva.

Pruebas: `MinoristaServiceTest` (Mockito: dirección en la localidad elegida, segundo guardado sobre la misma dirección, menor de edad y ubicación faltante) y `PerfilMinoristaIntegrationTest` (criterio de aceptación de punta a punta, validaciones, acceso del minorista al componente de dirección y `403` para un preventor). Para ejecutarlas: `mvnw.cmd -Dtest=MinoristaServiceTest,PerfilMinoristaIntegrationTest test` desde `backend`.

### Para otros issues

- `MinoristaService.buscarPerfil(idUsuario)` y `Minorista.getDireccionEntrega()`: dirección de entrega del minorista para pedidos, facturas y rutas.
- `DireccionService.modificarDireccion(direccion, …)`: modificar una dirección existente con las mismas validaciones del alta.

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
docker compose exec ollama ollama pull qwen2.5:3b   descarga el modelo del chatbot (E3-04, una sola vez)
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
- **E3-04 (chatbot):** dentro de Docker, Ollama está en `http://ollama:11434` (valor por defecto de `OLLAMA_URL` en el compose); el modelo se elige con `OLLAMA_MODEL` (por defecto `qwen2.5:3b`). Ver la sección del chatbot.
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

## Pagos y Checkout Pro · E4-05

`PagoService` registra pagos parciales o totales en ARS o USD. Para USD toma y persiste la cotización vigente mediante `CotizacionDolarService`; al completar el saldo, marca la factura como pagada. Checkout Pro se integra del lado del servidor con Orders API (`POST /v1/orders`): el minorista inicia un checkout y el backend devuelve `checkoutUrl`. Solo un Webhook firmado seguido de una consulta de la Order a Mercado Pago puede confirmar el pago.

### Endpoints

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| POST | `/api/pagos` | JEFE, ADMINISTRATIVO | Registra un pago con `facturaId`, `formaDePagoId`, `monto`, `moneda` (`ARS`/`USD`) y `observacion`. |
| GET | `/api/pagos/factura/{facturaId}` | JEFE, ADMINISTRATIVO | Lista los pagos activos de una factura. |
| POST | `/api/minorista/facturas/{facturaId}/checkout` | Minorista dueño de la factura | Crea o reutiliza una Order pendiente y devuelve la URL a la que debe redirigir el cliente. |
| GET | `/api/minorista/facturas/{facturaId}/checkout` | Minorista dueño de la factura | Consulta el estado local del último checkout de esa factura. |
| POST | `/api/webhooks/mercadopago/orders` | Público; valida `x-signature` | Recibe el evento Order, consulta `/v1/orders/{id}` y registra el pago cuando está acreditado. |

El checkout cobra el saldo pendiente en ARS. El backend no toma montos del navegador: calcula el saldo desde la factura y los pagos registrados. Mientras haya una Order activa no permite registrar otro pago para esa factura.

### Credenciales y Webhook

Copiá `.env.example` como `.env` y completá `MERCADOPAGO_ACCESS_TOKEN` y `MERCADOPAGO_WEBHOOK_SECRET` al final del proyecto. Son valores privados del backend; no los pongas en `client-admin` ni `client-public`, ni los subas al repositorio. Si Mercado Pago te da credenciales de prueba, usalas primero. Las URLs opcionales de resultado se configuran en `MERCADOPAGO_SUCCESS_URL`, `MERCADOPAGO_FAILURE_URL` y `MERCADOPAGO_PENDING_URL`.

En **Tus integraciones → tu aplicación → Webhooks**, configura el evento **Order (Mercado Pago)**. La ruta que debe recibir es:

```text
https://<dominio-publico-del-backend>/api/webhooks/mercadopago/orders
```

Mercado Pago necesita una URL HTTPS pública; para probar en tu PC usá un túnel HTTPS que apunte al backend local (por ejemplo, puerto `8080`) y pega esa URL completa. Copiá la firma secreta que genera esa configuración a `MERCADOPAGO_WEBHOOK_SECRET`. El Access Token se copia en `MERCADOPAGO_ACCESS_TOKEN`.

Con las credenciales vacías el servidor compila y arranca, pero al intentar crear un checkout responde `503` hasta configurar el Access Token. `mvnw.cmd test` cubre pagos, creación de Orders, verificación de firma e idempotencia sin contactar Mercado Pago.

## Chatbot de presupuesto con Ollama · E3-04

El visitante escribe qué quiere («quiero 10 alfajores y 5 chupetines») y recibe un presupuesto. **La IA solo interpreta el texto** y devuelve productos y cantidades; el backend valida cada producto contra el catálogo, el stock y el precio vigente, y calcula los totales.

- **Adapter:** `service/ChatbotProvider` (Target, contrato `interpretar(mensaje, productosDelCatalogo)`) y `service/adapter/OllamaChatbotAdapter` (Adapter de `POST /api/chat` de Ollama). Le exige al modelo un JSON con forma fija (`format` con JSON Schema: `{"items":[{"producto","cantidad"}]}`), temperatura 0 y un prompt de sistema con los nombres del catálogo. Si Ollama no responde, da error o devuelve algo que no es JSON, lanza `ServicioNoDisponibleException` (503).
- **Servicio:** `PresupuestoService.armarPresupuesto(mensaje)`. Busca cada renglón por nombre exacto (sin mayúsculas ni tildes); si no, por palabras tolerando plurales e ignorando conectores y empaques («3 cajas de chicles» ↔ «Chicle de menta»), y ante un empate prefiere el nombre que empieza con la palabra pedida («chocolates» → «Chocolate con leche», no «Alfajor de chocolate»). La IA no siempre copia el nombre exacto, por eso lo decide el backend. Excluye, con su motivo: lo que no está en el catálogo, lo ambiguo, lo que no tiene stock, lo que tiene stock insuficiente (informa los disponibles) y lo que no tiene precio vigente. Totales con `BigDecimal`; el total en dólares usa `CotizacionDolarService` (E1-08) y queda en `null` si no hay cotización. No reserva stock.
- **Configuración** (`.env`): `OLLAMA_URL` (vacío = `http://localhost:11434`; en Docker, `http://ollama:11434`), `OLLAMA_MODEL` (vacío = `qwen2.5:3b`) y `OLLAMA_TIMEOUT_MS` (vacío = 60000, porque sin GPU el modelo tarda unos segundos).
- **Datos de demo:** el seeder carga 5 productos con precio y stock. «Chupetín frutal» queda **sin stock** y «Chocolate con leche 25 g» con solo 5 unidades, para probar las exclusiones.

### Endpoint (público)

| Método | Ruta | Request → Response |
|---|---|---|
| POST | `/api/publico/chatbot/presupuesto` | `{"mensaje": "..."}` (obligatorio, hasta 500 caracteres) → `200` presupuesto · `400` mensaje vacío o largo · `503` Ollama no disponible |

```json
{
  "items": [{"idProducto": "…", "codigo": "ALF-001", "nombre": "Alfajor de chocolate", "presentacion": "Caja x 12",
             "cantidad": 10, "precioUnitario": 6000.00, "subtotal": 60000.00}],
  "excluidos": [{"producto": "Chupetín frutal", "cantidad": 5, "motivo": "Sin stock."}],
  "total": 60000.00, "cotizacionDolar": 1450.0, "fechaCotizacion": "2026-11-11", "totalDolares": 41.38
}
```

### Cómo probarlo

Criterio: con «quiero 10 alfajores y 5 chupetines» devuelve un presupuesto con total correcto y excluye productos sin stock.

```
cd IntegradorN2
docker compose --profile ollama up -d ollama
docker compose exec ollama ollama pull qwen2.5:3b         una sola vez (~1,9 GB)
cd backend && ./mvnw spring-boot:run                      con la base recién creada, para que corra el seeder

curl -X POST http://localhost:8080/api/publico/chatbot/presupuesto -H "Content-Type: application/json" \
     -d "{\"mensaje\":\"quiero 10 alfajores y 5 chupetines\"}"
```

Devuelve el alfajor con subtotal 60000 y el chupetín en `excluidos` con «Sin stock.». Con Ollama apagado (`docker compose stop ollama`) responde `503` con el mensaje para el usuario.

Tests (no necesitan Ollama): `OllamaChatbotAdapterTest` (traducción de la API con `MockRestServiceServer`), `PresupuestoServiceTest` (criterio y exclusiones) y `ChatbotIntegrationTest` (endpoint sin token, contra los productos del seeder, 400 y 503).

### Para otros issues

- **E3-05 (chat del sitio):** disponible en `/presupuesto`, desde la navegación y la página de inicio. Envía `POST /api/publico/chatbot/presupuesto` con `{mensaje}` (hasta 500 caracteres), muestra `items`, `excluidos` con su `motivo`, `total` y `totalDolares` (con `cotizacionDolar` y `fechaCotizacion`), y señala cuando el asistente está procesando. Ante un `503`, ofrece un formulario alternativo que arma el presupuesto con productos del catálogo, valida el stock y muestra totales en pesos y, si hay cotización, en dólares.
- **`ServicioNoDisponibleException`:** cualquier service que dependa de un servicio externo sin respaldo puede lanzarla para responder `503` con `{mensaje}`.
## Catálogo público · E3-01

Listado público de productos con foto, descripción, presentación por bulto y precio mayorista vigente. **Solo aparecen los productos activos con stock mayor que cero y precio vigente hoy.** Stock y precio se consultan en cada pedido, sin caché.

- **Consulta:** `ProductoRepository.buscarCatalogo(id, texto, categoria, hoy)`, una sola consulta que arma `ProductoCatalogoDTO` (los parámetros vacíos no filtran). `CatalogoService` la usa para el listado y para el detalle.
- **Pantalla:** `client-public/src/modules/catalog/pages/CatalogoPage.jsx`, ruta `/catalogo` («Catálogo» en el menú). Búsqueda por nombre (consulta 300 ms después de dejar de escribir) y filtro por categoría.

### Endpoints (públicos)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/publico/catalogo?nombre=&categoriaId=` | `ProductoCatalogoDTO[]` ordenados por nombre |
| GET | `/api/publico/catalogo/{id}` | `ProductoCatalogoDTO` · `404` si no existe, no tiene stock o no tiene precio |
| GET | `/api/publico/catalogo/categorias` | categorías activas |

`ProductoCatalogoDTO(id, codigo, nombre, descripcion, presentacion, unidadesPorBulto, precio, stock, categoriaId, categoriaNombre, imagenId)`. La foto se pide a `GET /api/imagenes/{imagenId}`.

### Cómo probarlo

Criterio: un producto que llega a stock 0 deja de mostrarse en el catálogo sin recargar la base.

1. Con la base recién creada (el seeder carga 5 productos; «Chupetín frutal» sin stock), abrir <http://localhost:5174/catalogo>: aparecen 4 productos y el chupetín no.
2. En el panel, descontar todo el stock de «Chocolate con leche 25 g» (Stock → ajuste) y actualizar el catálogo: el chocolate ya no aparece.

Test: `CatalogoPublicoIntegrationTest` (sin token, solo con stock, búsqueda, categoría, detalle con 404 y el criterio).

### Para otros issues

- **E3-03 (detalle):** `GET /api/publico/catalogo/{id}` ya trae precio en pesos y `stock`; el precio en dólares se calcula con `GET /api/publico/cotizacion`.
- **E4-02 (pedido del preventor):** `CatalogoService.buscar(nombre, categoriaId)` o el endpoint del listado; el `stock` sirve para avisar antes de cargar más de lo disponible.

## Facturación automática · E4-04

Procesar un pedido **CARGADO** o **SIN_STOCK** verifica el stock de todas sus líneas, todo en una sola transacción:

- **Con stock suficiente:** descuenta el stock de cada producto (movimiento de egreso con la observación «Factura N° … · pedido N° …»), emite la `Factura` en `PENDIENTE_PAGO` con número secuencial y el detalle copiado del pedido, pasa el pedido a **FACTURADO** (con su registro en el historial de E4-01) y publica `FacturaEmitidaEvent`.
- **Sin stock suficiente:** el pedido pasa a **SIN_STOCK** y la respuesta trae los faltantes (producto, cantidad pedida, stock disponible). No se toca el stock. Si ya estaba en SIN_STOCK, no se repite el registro del historial. Cuando se repone, se vuelve a procesar.

Hay dos formas de procesar: un pedido (botón del panel de E4-06) o todos los pendientes en orden de carga (`POST /api/pedidos/procesar`, cada pedido en su propia transacción).

La factura en PDF se arma **en memoria con OpenPDF** cada vez que se descarga y no se guarda. Contiene: empresa (razón social y CUIT del mayorista), número, fecha y estado; comercio, responsable, documento y dirección; número de pedido y preventor; renglones; subtotal, descuento con el código de promoción y total.

- **Service:** `FacturacionService` (`facturarPedido`, `procesarPedidosPendientes`, `buscarFactura`, `buscarFacturaPorPedido`, `listarFactura`, `listarFacturaPorEstado`) y `FacturaPdfService` (`generarFacturaPdf`, `nombreArchivo`).
- **Reutilizado de:** `IntegradorN1/.../VentaService` (revalidar stock por línea y descontarlo), TP2 Ej. 4e `FacturaReportService` (contenido de la factura y nombre `factura-00000012.pdf`) y TP2 Ej. 2d `ReporteService` (PDF en memoria; allí con iText, acá con OpenPDF).

### Endpoints (JEFE y ADMINISTRATIVO)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/api/pedidos/{id}/facturar` | `ResultadoFacturacionResponse` · `200` con `estadoPedido` FACTURADO (y `factura`) o SIN_STOCK (y `faltantes`) · `400` si el pedido no está CARGADO ni SIN_STOCK o ya tiene factura · `404` si no existe · `409` si otra factura tomó el mismo número al mismo tiempo (reintentar) |
| POST | `/api/pedidos/procesar` | `ResultadoFacturacionResponse[]`, uno por pedido pendiente; si uno falla, trae `estadoPedido = null` y el motivo en `mensaje` |
| GET | `/api/pedidos/{id}/factura` | `FacturaResponse` · `404` si todavía no se facturó |
| GET | `/api/facturas?estado=` | `FacturaResponse[]` de la más nueva a la más vieja (`estado` opcional: `PENDIENTE_PAGO`, `PAGADA`, `ANULADA`) |
| GET | `/api/facturas/{id}` | `FacturaResponse` |
| GET | `/api/facturas/{id}/pdf` | `application/pdf` con `Content-Disposition: attachment; filename="factura-00000012.pdf"` |

`ResultadoFacturacionResponse(pedidoId, numeroPedido, estadoPedido, factura, faltantes[], mensaje)`.
`FacturaResponse(id, numeroFactura, numeroVisible, fechaFactura, subtotal, descuento, total, estado, pedidoId, numeroPedido, comercio, detalles[])`.
`FaltanteStockDTO(productoId, producto, cantidadPedida, stockDisponible)`.

### Cómo probarlo

Criterio: un pedido con stock se factura solo, descuenta el stock y deja la factura descargable.

1. Como `preventor@dulcemayor.com.ar`, cargar un pedido desde el portal de preventa (E4-02) y anotar su id y las cantidades.
2. Como `administrativo@dulcemayor.com.ar`, mirar el saldo del producto con `GET /api/stock/{productoId}`.
3. `POST /api/pedidos/{id}/facturar` → `estadoPedido: "FACTURADO"` y la factura con su número.
4. `GET /api/stock/{productoId}` → el saldo bajó en la cantidad del pedido. `GET /api/pedidos/{id}/historial` muestra CARGADO → FACTURADO.
5. `GET /api/facturas/{facturaId}/pdf` descarga `factura-0000000N.pdf`.
6. Sin stock: cargar un pedido con más bultos que el saldo → `SIN_STOCK` con los faltantes. Reponer (`Stock → ingreso`) y `POST /api/pedidos/procesar` → queda FACTURADO.

Tests: `FacturacionServiceTest` (con stock, sin stock, SIN_STOCK repetido, producto dado de baja, estado inválido, factura ya emitida, 404, número en conflicto y procesamiento masivo), `FacturaPdfServiceTest` (contenido del PDF extraído como texto) y `FacturacionIntegrationTest` (criterio de punta a punta, numeración N y N+1, SIN_STOCK → reponer → procesar, 401/403/404).

### Para otros issues

- **Firma del contrato:** es `FacturacionService.facturarPedido(idPedido, idUsuario)`, no `facturarPedido(idPedido)`. Se agregó `idUsuario` porque E4-01 exige el responsable en el historial; se saca del token (`@AuthenticationPrincipal UsuarioAutenticado`). Está comentado en el código (`NOTA (E4-04)`).
- **E4-06 (panel de almacén):** [Almacén y facturación](#almacén-y-facturación--e4-06) usa la facturación individual y la descarga de PDF.
- **E4-07 (correo de factura):** cada factura confirmada activa el envío descrito en [Correo de confirmación de factura](#correo-de-confirmación-de-factura--e4-07).
- **E4-05 (pagos):** la factura nace en `PENDIENTE_PAGO`; `PagoService.registrar` o la confirmación acreditada del Webhook de Mercado Pago la pasa a `PAGADA` cuando se cubre el saldo.
- **E4-08 (minorista):** no usar `/api/facturas/**` (solo gestión). Exponer las facturas bajo `/api/minorista/**`, validar que la factura sea de un punto de venta del minorista logueado (`FacturaRepository.findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse`) y recién ahí llamar a `FacturaPdfService`.
- **E5-03 (reportes):** OpenPDF ya está en el `pom.xml` (`openpdf.version`).
- **Numeración:** es el último número + 1, y la columna `numero_factura` es única. Si se necesitara más concurrencia, se reemplaza por una secuencia de la base sin cambiar la firma.

## Almacén y facturación · E4-06

El panel `/almacen` está disponible para Jefe y Administrativo. El listado admite un filtro por estado (`GET /api/pedidos?estado=`). Al abrir un pedido (`GET /api/pedidos/{id}`), el detalle muestra productos, importes, faltantes de stock y, cuando ya se facturó, la factura y los pagos registrados.

1. Como Preventor, cargá un pedido en `/preventa`. Como Administrativo, abrí **Almacén y facturación**, filtrá por **Cargado** y abrí su detalle.
2. Pulsá **Procesar y facturar**. Con stock suficiente, el pedido queda **Facturado**, se descuentan las unidades y se habilita **Descargar PDF**. Si hay faltantes, el pedido queda **Sin stock** y podés volver a procesarlo después de reponer.
3. Registrá un pago parcial en efectivo y completá el saldo por transferencia. El detalle muestra los dos cobros y la factura queda **Pagada** al cubrir el total.

## Correo de confirmación de factura · E4-07

Al facturar un pedido, individualmente o en lote, se envía en segundo plano un correo al contacto registrado del minorista del punto de venta. Si no hay un contacto válido, se usa el correo de su cuenta. El mensaje incluye el comercio, el número de pedido, sus productos, los importes y la factura PDF adjunta. El envío se activa después de confirmar la transacción.

Para probarlo, configurá el [SMTP de desarrollo](#servicio-de-correo-asíncrono), cargá un pedido con stock desde Preventa y facturalo con `POST /api/pedidos/{id}/facturar` como Jefe o Administrativo. En la bandeja de prueba, abrí «Factura N° … · Dulce Mayor» y comprobá el detalle y el adjunto `factura-00000001.pdf` (el número depende de la factura emitida).

## Panel del minorista: compras y facturas · E4-08

En el sitio público, **Mi cuenta → Mis compras** muestra al minorista todos los pedidos de sus puntos de venta, del más nuevo al más viejo, con el estado del envío y el de la factura. El detalle de cada pedido trae los productos, los importes, la **línea de tiempo** del envío (historial de E4-01) y la factura, que se puede descargar en PDF. Si la factura está pendiente de pago y tiene saldo, aparece **«Pagar ahora»**, que abre el Checkout Pro de Mercado Pago de E4-05.

El minorista sale siempre del token y cada consulta filtra por `pedido.puntoDeVenta.minorista`. Si pide un pedido o una factura de otro minorista recibe **404**, aunque conozca el id (igual que el checkout de E4-05), así no se revela que existe. La línea de tiempo no expone el usuario interno que hizo cada cambio; solo el estado, la fecha, la observación y el transportista.

- **Service:** `CompraMinoristaService` (`listarPedidoPorMinorista`, `buscarPedidoDelMinorista`, `descargarFacturaDelMinorista`, todos con el id del usuario del token). El PDF lo arma `FacturaPdfService` de E4-04 y el saldo sale de `PagoService.saldoPendienteEnPesos` de E4-05.
- **Pantallas:** `client-public/src/modules/compras/` (`ComprasPage`, `CompraDetallePage`, `LineaTiempo`, `AccionesFactura`).
- **Reutilizado de:** `IntegradorN1/.../CompraClienteController` (el cliente sale de la sesión y una compra ajena se rechaza) y su `CompraClienteIntegrationTest`.

### Endpoints (MINORISTA)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/minorista/compras` | `CompraMinoristaResponse[]` del más nuevo al más viejo |
| GET | `/api/minorista/compras/{pedidoId}` | `CompraMinoristaDetalleResponse` · `404` si no es suyo o no existe |
| GET | `/api/minorista/facturas/{facturaId}/pdf` | `application/pdf` con `Content-Disposition: attachment; filename="factura-00000012.pdf"` · `404` si no es suya |
| POST | `/api/minorista/facturas/{facturaId}/checkout` | Ya existía (E4-05): «Pagar ahora» lo llama y redirige a `checkoutUrl` |

`CompraMinoristaResponse(pedidoId, numeroPedido, fechaHoraCarga, comercio, subtotal, descuento, total, estadoPedido, facturaId, numeroFactura, fechaFactura, estadoFactura, saldoPendiente, puedePagar)`. Los campos de factura y `saldoPendiente` son `null` mientras el pedido no se facturó; `numeroFactura` es el número visible con ceros.
`CompraMinoristaDetalleResponse(compra, codigoPromocion, detalles[{productoId, codigo, producto, presentacion, cantidad, precioUnitario, subtotal}], lineaDeTiempo[{estado, fechaHora, observacion, transportista}])`.

Para que Mercado Pago devuelva al minorista a su panel después de pagar, configurá `MERCADOPAGO_SUCCESS_URL` (y, si querés, `MERCADOPAGO_FAILURE_URL` y `MERCADOPAGO_PENDING_URL`) con `http://localhost:5174/cuenta/compras` o la URL pública equivalente.

### Cómo probarlo

Criterio: el minorista ve todos sus pedidos, descarga cada factura y no puede ver pedidos de otro minorista.

1. Como `preventor@dulcemayor.com.ar`, cargar dos pedidos para «Kiosco Centro» desde el portal de preventa (E4-02).
2. Como `administrativo@dulcemayor.com.ar`, facturar uno con `POST /api/pedidos/{id}/facturar`.
3. Entrar al [sitio público](http://localhost:5174/login) con `minorista@dulcemayor.com.ar` y abrir **Mi cuenta → Mis compras**: aparecen los dos pedidos; el facturado muestra «Pendiente de pago», «Descargar factura» y «Pagar ahora».
4. Abrir el detalle: la línea de tiempo muestra Recibido → Facturado con fecha y hora, y En reparto y Entregado como pendientes. «Descargar factura» baja `factura-0000000N.pdf`.
5. «Pagar ahora» redirige a Mercado Pago si están las credenciales de E4-05; si no, muestra el mensaje del servidor (503).
6. Con el token del minorista, pedir `GET /api/minorista/compras/{id}` de un pedido de otro minorista → `404`. Con un token de preventor, `GET /api/minorista/compras` → `403`.

Tests: `CompraMinoristaServiceTest` (Mockito: listado con y sin factura, `puedePagar` solo con saldo, detalle con líneas y línea de tiempo, pedido y factura ajenos → 404, usuario sin perfil) y `CompraMinoristaIntegrationTest` (criterio de punta a punta con dos minoristas, PDF descargable, 401/403/404). Para ejecutarlas: `mvnw.cmd "-Dtest=CompraMinoristaServiceTest,CompraMinoristaIntegrationTest" test` desde `backend`.

### Para otros issues

- **«Pagar ahora» y la forma de pago (desvío del plan):** el plan pide el botón solo en los pedidos "pendientes de pago con billetera", pero `Pedido` no guarda la forma de pago elegida (ni en el UML ni en el código). Por eso `puedePagar` es true para toda factura `PENDIENTE_PAGO` con saldo. Si un issue agrega la forma de pago al pedido, filtrar por `BILLETERA_VIRTUAL` en `CompraMinoristaService.compra(...)`. Está comentado con `NOTA (E4-08)`.
- **Página de pago:** E4-05 quedó como Checkout Pro (redirección a Mercado Pago), no como una página de QR dentro del sitio; el botón usa ese checkout tal cual.
- **UML:** `listarFacturaPorMinorista(idMinorista)` no se agregó a `FacturacionService`. La lectura del minorista vive en `CompraMinoristaService` y recibe el id del **usuario** del token, para que nadie pida las compras de otro pasando un id.
- **E5-02 (entrega):** cuando el distribuidor marca EN_REPARTO o ENTREGADO, el historial de E4-01 registra el transportista y la línea de tiempo del minorista lo muestra sin cambios.
- **E5-03 (reportes):** `PedidoRepository.findByPuntoDeVenta_Minorista_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(minoristaId)` sirve para reportes por cliente.

## Ruta del preventor con próximo punto más cercano · E5-01

El portal de Preventa muestra **Mi ruta de hoy**: un mapa con todos los comercios del preventor, el próximo punto propuesto y el recorrido sugerido. Al cerrar (cargar) un pedido, ese comercio queda visitado y la pantalla propone el siguiente punto más cercano y vuelve a dibujar el recorrido.

- **Ruta del día:** ningún issue da de alta rutas, así que `RutaService` la arma sola la primera vez que el preventor la consulta en el día. Hay una `Ruta` por preventor y por día, con una `ParadaRuta` por cada punto de venta activo que tiene asignado. Los comercios que se dan de alta durante el día se suman como paradas nuevas.
- **Visita:** `PreventaService.cargarPedido` llama a `RutaService.registrarVisita(pedido)`, que asocia el pedido a la parada del comercio. Si es un segundo pedido al mismo comercio, se agrega otra parada visitada. Con todas las paradas visitadas, la ruta pasa a `REALIZADA`.
- **Próximo punto:** es la parada pendiente más cercana a la posición actual, medida en línea recta con la fórmula de Haversine (`utils/DistanciaGeografica`). El **recorrido sugerido** aplica la heurística del vecino más cercano: desde la posición, siempre a la parada pendiente más próxima.
- **Posición:** se toma del GPS del celular (`navigator.geolocation`, que solo funciona en `localhost` o con HTTPS). Sin GPS, se parte del último punto visitado. Si todavía no visitó ninguno, el recorrido empieza por la primera parada pendiente y no se informa distancia.
- **Limitación conocida:** el recorrido son tramos en línea recta, no el camino por calle.
- `ParadaRuta.orden` es el orden de alta de la parada. El orden de visita se calcula en cada consulta y no se guarda.

### Endpoint

- `GET /api/preventa/ruta?latitud=…&longitud=…` (solo PREVENTOR; los dos parámetros son opcionales) → `{id, fecha, estado, preventorId, distribuidorId, paradas[{id, orden, puntoDeVentaId, nombreComercio, calle, numeracion, localidad, latitud, longitud, visitada, pedidoId}], siguiente, distanciaKm, recorridoSugerido[idParada…]}`. La ruta sale del preventor del token. Una posición incompleta o fuera de rango responde `400` con "La posición actual no es válida.".

### Cómo probarlo

1. Ingresá como `preventor@dulcemayor.com.ar` y abrí [Preventa](http://localhost:5173/preventa). Los datos de demo traen 4 comercios en Capital, Godoy Cruz, Guaymallén y Las Heras. Si tu base ya tenía datos, vaciala para que el `DataSeeder` los cargue.
2. En **Mi ruta de hoy** se ven los 4 puntos numerados según el recorrido, la línea punteada y la tarjeta **Próximo punto**.
3. Tocá **Cargar pedido aquí**, agregá un producto del catálogo y tocá **Cargar pedido**.
4. La tarjeta pasa a "Pedido cerrado. Próximo punto" con otro comercio y su distancia. El punto visitado aparece con ✓ en gris y el recorrido se vuelve a dibujar.

Pruebas: `RutaServiceTest` (Mockito: armado de la ruta, punto más cercano, recorrido, visitas, ruta realizada, posición inválida), `DistanciaGeograficaTest` y `PreventaIntegrationTest.alCerrarUnPedidoLaRutaProponeElSiguientePuntoMasCercano` (criterio de aceptación de punta a punta, más `400`, `403` y `401`). Para ejecutarlas: `mvnw.cmd -Dtest=RutaServiceTest,DistanciaGeograficaTest,PreventaIntegrationTest test` desde `backend`.

### Para otros issues

- **Contrato del kickoff (E5-02):** `RutaService.siguientePunto(idRuta, latitud, longitud)` devuelve la `ParadaRutaResponse` pendiente más cercana, o `null` si no quedan pendientes. Hoy "pendiente" significa "sin pedido". Para el distribuidor, una parada pendiente es un pedido no entregado, así que E5-02 va a tener que parametrizar ese criterio.
- E5-02 arma la ruta del distribuidor con `crearRuta(fecha, null, idDistribuidor)` + `agregarParada(idRuta, idPuntoDeVenta, idPedido, orden)` y la lista con `listarRutaPorDistribuidor`. El endpoint del distribuidor lo expone E5-02, no `/api/preventa`.
- `client-admin/src/modules/preventa/components/RutaMap.jsx` recibe `{ruta, posicion, origen}` con la forma de `RutaResponse` y se puede reutilizar en el portal del distribuidor.
- `DistanciaGeografica.kilometros(...)` y `coordenadasValidas(...)` sirven para cualquier cálculo de distancia.

## Portal móvil del distribuidor · E5-02

La sección [Distribución](http://localhost:5173/distribucion) es el portal del distribuidor y está pensada para usarla desde el celular. Ahí el distribuidor toma los pedidos listos, ve su ruta de reparto en el mapa y marca cada pedido como entregado. Al entregar, el sistema propone el siguiente punto y le avisa por correo a ese comercio que el pedido va en camino.

- **Tomar pedidos:** el portal lista los pedidos `FACTURADO` cuya factura está `PAGADA`, porque E4-05 deja el pedido "pendiente de envío" recién al pagarse. Al tomarlos pasan a `EN_REPARTO` (evento `INICIAR_REPARTO` del State de E4-01), el distribuidor queda como transportista y se suman como paradas a su **ruta de reparto del día**. La operación es todo o nada: si un pedido de la selección no está listo, no se toma ninguno.
- **Ruta de reparto:** se guarda una `Ruta` por distribuidor y por día, con una `ParadaRuta` por pedido. Si quedaron pedidos sin entregar en rutas de días anteriores, pasan a la de hoy y la ruta vieja queda `INCOMPLETA`. Cuando se entrega el último pedido, la ruta pasa a `REALIZADA`.
- **Entregado:** el pedido pasa a `ENTREGADO` y el historial registra la fecha, el usuario, el transportista y la observación. Después se calcula el siguiente punto con el mismo criterio de E5-01 (la parada pendiente más cercana) desde el GPS. Sin GPS, se parte del comercio recién entregado.
- **Correo "en camino":** cuando se confirma la entrega, `PedidoEnCaminoCorreoService` le manda la plantilla `correo/pedido-en-camino` al responsable del comercio siguiente. Si el envío falla, solo queda en el log. Cada parada recibe **un solo** aviso (`ParadaRuta.fechaAvisoEnCamino`), aunque vuelva a quedar como siguiente porque el distribuidor entregó fuera de orden.
- **Datos de demo:** el `DataSeeder` crea el perfil `Distribuidor` "Dante Distribuidor" (legajo D-001) para `distribuidor@dulcemayor.com.ar`.

### Endpoints (solo DISTRIBUIDOR; el Jefe no hereda este rol)

| Método | Ruta | Request → Response |
|---|---|---|
| GET | `/api/distribucion/pedidos-listos` | → `[{id, numeroPedido, numeroFactura, nombreComercio, calle, numeracion, localidad, total}]` |
| POST | `/api/distribucion/reparto` | `{pedidoIds: [...]}` → `RutaResponse` (`400` si un pedido no está facturado o no está pagado) |
| GET | `/api/distribucion/ruta?latitud=…&longitud=…` | → `RutaResponse` de la ruta de hoy (la posición es opcional) |
| GET | `/api/distribucion/pedidos/{id}` | → `PedidoAlmacenResponse` (productos a entregar). `404` si el pedido no está en su ruta |
| POST | `/api/distribucion/pedidos/{id}/entrega` | `{latitud?, longitud?, observacion?}` → `{historial, ruta, siguiente, avisoEnCaminoA}` (`404` si el pedido no está en su ruta y `400` si ya se entregó) |

Las paradas de `RutaResponse` ahora traen también `numeroPedido` y `estadoPedido`. En la ruta de reparto, `visitada` significa "entregado".

### Cómo probarlo

1. Como preventor, cargá dos pedidos en comercios distintos (por ejemplo Kiosco Centro y Almacén Godoy Cruz).
2. Como administrativo, entrá a [Almacén y facturación](http://localhost:5173/almacen), procesá los dos pedidos y registrá el pago en efectivo de cada factura.
3. Ingresá como `distribuidor@dulcemayor.com.ar` (clave de demo) y abrí **Distribución**. En **Listos para reparto** elegí los dos pedidos y tocá **Tomar para reparto**.
4. El mapa muestra los dos puntos y el **Próximo punto**. Tocá **Entregado** en uno y confirmá. Aparece "Pedido N° … entregado. Le avisamos a … que vas en camino.", el punto pasa a ✓ y en Mailtrap llega el correo "Tu pedido N° … está en camino" para el otro comercio.
5. El historial del pedido (`GET /api/pedidos/{id}/historial` como administrativo) termina en `ENTREGADO`, con la fecha y el transportista.

Pruebas: `DistribucionServiceTest` (Mockito: tomar pedidos, pedido sin pagar o sin facturar, siguiente punto con y sin GPS, aviso único, ruta realizada, `404` fuera de ruta, pedidos de días anteriores) y `DistribucionIntegrationTest` (criterio de aceptación de punta a punta con el correo mockeado, más `401` y `403` para los otros roles). Para ejecutarlas: `mvnw.cmd -Dtest=DistribucionServiceTest,DistribucionIntegrationTest test` desde `backend`.

### Para otros issues

- El paso de `FACTURADO` a `EN_REPARTO` lo hace E5-02 desde el portal (`DistribucionService.tomarPedidos`). Si más adelante el almacén asigna los repartos, tiene que llamar a `PedidoService.avanzar(id, INICIAR_REPARTO, usuario, obs, idDistribuidor)` y a `RutaService.agregarAReparto(ruta, pedido)`.
- El criterio de "parada pendiente" ahora vive en `ParadaRuta.estaPendiente()`: sin pedido en la ruta del preventor y pedido no entregado en la de reparto (`Ruta.esDeReparto()`). `RutaService.siguientePunto` (contrato del kickoff) funciona igual para las dos.
- `DestinatarioMinorista.buscar(minorista)` da el correo del responsable de un comercio. Lo usan E4-07 y E5-02, y sirve para E5-05 o E6-03.
- `DistribuidorService.buscarDistribuidorPorUsuario(idUsuario)` da el perfil del distribuidor a partir del usuario del token.
- `RutaMap.jsx` acepta `etiquetaHecha` (por defecto "Visitado"; el portal del distribuidor usa "Entregado").
- E6-04 puede cargar pedidos en `EN_REPARTO` o `ENTREGADO` con `PedidoService.avanzar(...)` usando el distribuidor de demo como transportista.
