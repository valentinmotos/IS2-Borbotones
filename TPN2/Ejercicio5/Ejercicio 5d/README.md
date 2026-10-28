# TPN2 · Ejercicio 5d · Tinder de Mascotas con JWT

Reimplementación independiente de `TPN1/Ejercicio5/Ejercicio 5c`. Reutiliza sus entidades
`Usuario`, `Zona`, `Mascota`, `Foto`, `Voto` y sus enumeraciones `Sexo` y `Tipo`.
Mantiene la auditoría con Hibernate Envers. El proyecto de TPN1 queda intacto.

El acceso por `HttpSession` se reemplazó por una API REST con Spring Security y JWT.
La interfaz HTML/CSS/JavaScript consume esa API desde el mismo servidor; no necesita Node,
compilar un frontend ni descargar recursos de una CDN.

## Ejecutar

Requisitos: **JDK 17 o superior** y **Maven 3.6.3 o superior**.

Desde la raíz del repositorio:

```powershell
cd 'TPN2/Ejercicio5/Ejercicio 5d'
mvn spring-boot:run
```

Abrir **http://localhost:8080**. Por defecto usa una base H2 persistente en
`./data/mascotas-jwt`, relativa a la carpeta desde la que se ejecuta el proceso.
Las zonas Centro, Norte, Sur, Este y Oeste se crean al iniciar si no existen zonas.
No se crean usuarios ni contraseñas predeterminados. La consola H2 está deshabilitada.

Para generar un ejecutable:

```powershell
mvn clean package
java -jar target/mascotas-jwt-0.0.1-SNAPSHOT.jar
```

## Probar los flujos

1. Registrarse, elegir una zona y agregar una mascota con foto opcional.
2. Cerrar sesión y registrar una segunda cuenta en la misma zona. Agregar otra mascota
   de la misma especie y distinto sexo.
3. En **Explorar**, seleccionar la mascota propia y votar a la otra.
4. Ingresar con la primera cuenta: en **Votos y matches** aparece el voto recibido.
   Aceptarlo produce un match. Votar en sentido inverso también produce un match.
5. En **Mis mascotas** se pueden editar los datos y la foto, o dar una baja lógica.
   En **Mi perfil** se pueden actualizar nombre, apellido, zona y foto.

La exploración excluye mascotas propias, inactivas, de otras zonas, de distinta especie,
del mismo sexo o ya votadas. Solo se puede votar desde una mascota propia y activa.
Los votos y matches son visibles únicamente para sus participantes.
La notificación de votos ocurre en la interfaz; este ejercicio no requiere SMTP ni envía correos.

## Seguridad aplicada

- Registro y login devuelven `accessToken`, `tokenType: Bearer`, `expiresIn` y un DTO de usuario.
- Las contraseñas se almacenan con **BCrypt**. Se valida el límite de 72 bytes del algoritmo.
  Los hashes no se devuelven por la API ni se almacenan en las revisiones de Envers.
- El JWT está firmado con **HS256**. Incluye `iss`, `sub` (ID de usuario), `iat`, `exp`, `jti`,
  `tipo: access` y `version`. El filtro valida firma, emisor, vencimiento, tipo, existencia,
  estado del usuario y versión vigente antes de autenticar.
- Los JWT vencen a los **15 minutos** por defecto. Se vuelve a iniciar sesión al vencer;
  no hay un refresh token sin vencimiento.
- Se utiliza `SessionCreationPolicy.STATELESS`: no se crea una sesión HTTP para autenticar.
  En cada petición se consulta el usuario para verificar su estado y la versión del token.
- El cliente envía `Authorization: Bearer <token>` en todas las solicitudes privadas,
  incluidas las fotos. No se aceptan tokens en la URL ni en cookies.
- El token permanece **solo en memoria del navegador**. Al recargar o cerrar la pestaña
  hay que volver a ingresar. No se guarda en `localStorage` ni `sessionStorage`.
- `POST /api/auth/logout` incrementa la versión persistida del usuario e invalida **todos**
  sus JWT anteriores, incluso si alguien intenta reutilizarlos desde otra pestaña o cliente.
- La identidad y la propiedad se resuelven en el servidor desde el JWT. Un ID enviado por
  el cliente no permite editar perfiles o mascotas de otro dueño ni aceptar sus votos.
- Respuestas `400` para datos inválidos, `401` para autenticación inválida, `403` para
  operaciones sobre recursos ajenos, `404` para recursos inexistentes/inactivos,
  `409` para duplicados y `413` para archivos demasiado grandes.
- Las fotos son privadas para usuarios autenticados. Se comprueba su contenido real PNG/JPEG,
  máximo 2 MB de entrada y 16 megapíxeles; se recodifican para descartar metadatos.
- Se usan DTO, validación de entradas, escape del contenido de usuario en HTML, CSP y
  las cabeceras de seguridad de Spring. Los votos duplicados y mails repetidos también
  tienen restricciones de unicidad en la base.
- CSRF está deshabilitado porque la autenticación usa exclusivamente un encabezado Bearer
  que agrega el cliente. Si se cambia a cookies de autenticación, se debe activar protección CSRF.
  No se habilita CORS: frontend y API se sirven desde el mismo origen.

## Configuración

| Variable | Predeterminado | Uso |
|---|---|---|
| `PORT` | `8080` | Puerto HTTP |
| `JWT_SECRET` | Clave aleatoria al arrancar | Secreto Base64 de al menos 32 bytes aleatorios |
| `JWT_EXPIRATION_SECONDS` | `900` | Duración del JWT, mayor que cero |
| `DB_URL` | `jdbc:h2:file:./data/mascotas-jwt` | URL JDBC |
| `DB_USERNAME` | `sa` | Usuario de base de datos |
| `DB_PASSWORD` | Vacío | Contraseña de base de datos |

Sin `JWT_SECRET` se genera una clave efímera: al reiniciar se invalidan los tokens anteriores.
Para conservar una clave entre reinicios, generar una y configurarla en el entorno antes de ejecutar:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
mvn spring-boot:run
```

No subir la clave al repositorio. Para MySQL, crear una base exclusiva para este ejercicio
y configurar `DB_URL=jdbc:mysql://localhost:3306/mascotas_jwt`, `DB_USERNAME` y `DB_PASSWORD`.
El driver ya está incluido. No conectar esta aplicación a la base del ejercicio original:
sus contraseñas sin cifrar no son compatibles con BCrypt.

La configuración es para un trabajo práctico local. Un despliegue público requiere HTTPS,
un secreto estable administrado fuera del código, límites de intentos de login y migraciones
de esquema en lugar de `ddl-auto=update`.

## API

Solo `GET /api/zonas`, `POST /api/auth/registro` y `POST /api/auth/login` son públicos.
El resto requiere `Authorization: Bearer <accessToken>`.

| Método | Ruta | Operación |
|---|---|---|
| GET | `/api/zonas` | Listar zonas |
| POST | `/api/auth/registro` | Registrar y obtener JWT |
| POST | `/api/auth/login` | Obtener JWT |
| POST | `/api/auth/logout` | Invalidar los tokens del usuario |
| GET / PUT | `/api/usuarios/me` | Consultar / actualizar el perfil propio |
| GET / PUT | `/api/usuarios/me/foto` | Consultar / cargar foto propia |
| GET / POST | `/api/mascotas` | Listar propias / crear mascota |
| PUT / DELETE | `/api/mascotas/{id}` | Editar / dar de baja una mascota propia |
| GET / PUT | `/api/mascotas/{id}/foto` | Consultar foto / cargarla si es propia |
| GET | `/api/mascotas/explorar?idMascotaPropia={id}` | Buscar compatibles |
| GET / POST | `/api/votos` | Consultar votos propios y recibidos / votar |
| POST | `/api/votos/{id}/responder` | Aceptar un voto recibido y hacer match |

Los cuerpos son JSON, excepto las fotos: `multipart/form-data` con campo `archivo`.

Registro:

```json
{"nombre":"Ana","apellido":"Pérez","mail":"ana@example.com","clave":"MiClave123!","clave2":"MiClave123!","idZona":"ID_DE_GET_API_ZONAS"}
```

Login:

```json
{"mail":"ana@example.com","clave":"MiClave123!"}
```

Crear/editar mascota:

```json
{"nombre":"Luna","sexo":"HEMBRA","tipo":"PERRO"}
```

Valores de sexo: `MACHO`, `HEMBRA`. Especies: `PERRO`, `GATO`, `CONEJO`.

Actualizar perfil:

```json
{"nombre":"Ana","apellido":"Pérez","idZona":"ID_DE_GET_API_ZONAS"}
```

Votar:

```json
{"idMascotaPropia":"ID_ORIGEN","idMascotaVotada":"ID_DESTINO"}
```

## Validación automática

```powershell
mvn test
```

Las pruebas de integración con MockMvc y una base H2 en memoria comprueban registro,
BCrypt, login, aislamiento de usuarios, JWT inválidos/vencidos, revocación al salir,
CRUD y bajas lógicas, exploración, votos, matches y fotos. No modifican la base local.

Referencias: [sesiones stateless en Spring Security](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)
y [firma y validación en JJWT](https://github.com/jwtk/jjwt/tree/0.12.6).
