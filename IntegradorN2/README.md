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
| Administración | http://localhost:5173 | `/`, `/guia`, `/componentes` |
| Sitio promocional | http://localhost:5174 | `/`, `/empresa`, `/como-comprar`, `/creditos` |

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

Cada módulo consume `api` desde `src/lib/api.js`. Los interceptores agregan `Authorization: Bearer <token>` leyendo la sesión en cada solicitud. El token se guarda en `sessionStorage` con claves distintas (`dulcemayor.admin.token` y `dulcemayor.public.token`), se elimina ante un 401 y se notifica a la interfaz. Un 403 conserva la sesión. `ApiError` expone `message`, `status` y `campo`, respetando el contrato `{mensaje, campo}` del backend. El futuro login puede usar `tokenStore.set(token)` y `{ skipAuth: true }` para la petición de autenticación; no hay credenciales precargadas ni autenticación ficticia. La protección de rutas por rol corresponde a E1-02.

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
