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

## Cómo correrlo

1. Levantar PostgreSQL. La forma más rápida es Docker (el `docker-compose.yml` completo llega con E0-09):
   ```
   docker run -d --name dulcemayor-db -e POSTGRES_DB=dulcemayor -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
   ```
2. Variables de entorno: copiar `.env.example` como `.env` y completarlo. Para el contenedor de arriba no hace falta definir nada, porque los valores por defecto ya apuntan a `localhost:5432/dulcemayor` con usuario y clave `postgres`.
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
- Datos iniciales: `config/DataSeeder` corre solo si **todas** las tablas están vacías. Para volver a cargarlos, vaciar la base (por ejemplo `docker rm -f dulcemayor-db` y crear el contenedor de nuevo).

## Estructura del backend

```
com.dulcemayor
 ├─ config        SecurityConfig, AsyncConfig, SchedulingConfig, DataSeeder
 ├─ entity        entidades JPA (todas extienden BaseEntity)
 │   └─ enums
 ├─ repository    repositorios Spring Data JPA (uno por entidad)
 ├─ service       lógica de negocio
 ├─ dto           records de request/response
 │   └─ mapper    MapStruct (E0-08)
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

### Seguridad provisoria

`SecurityConfig` deja la API sin estado, sin CSRF y **con todas las rutas abiertas**, para poder probar la API antes del login. E1-01 la reemplaza con el filtro JWT y las reglas por rol. Lo que tiene que mantener: `STATELESS`, CSRF deshabilitado, `ErroresSeguridadHandler` como entry point y access denied handler, y `/error` abierto. `@EnableMethodSecurity` ya está activo, así que `@PreAuthorize` funciona.

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
