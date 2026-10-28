# Ejercicio 5e · Tinder de Mascotas con OAuth2 y OpenID Connect

Versión independiente del Tinder de Mascotas de `TPN2/Ejercicio5/Ejercicio 5d`, cuyo
modelo proviene de `TPN1/Ejercicio5/Ejercicio 5c`. Conserva perfiles, zonas, mascotas,
fotos, bajas lógicas, exploración, votos y matches, y la auditoría Hibernate Envers.
El login propio con JWT se reemplaza por **OAuth 2.0 Authorization Code con PKCE y
OpenID Connect**. Los proyectos anteriores quedan intactos.

Se utiliza un proveedor de identidad local para ejecutar el ejercicio completo sin
cuentas, secretos ni servicios externos. OAuth2 es el protocolo de autorización;
OpenID Connect agrega la autenticación del usuario. Auth0 es una posible plataforma
proveedora de estos protocolos; este proyecto no depende de una cuenta Auth0.

## Arquitectura

| Módulo | Dirección predeterminada | Responsabilidad |
|---|---|---|
| `autorizacion` | `http://127.0.0.1:9000` | Registro, login, consentimiento, emisión de tokens y JWKS |
| `cliente` | `http://127.0.0.1:8080` | Interfaz del Tinder, login OIDC, sesión y llamadas autenticadas a la API |
| `servidor` | `http://127.0.0.1:8000` | API de mascotas protegida como OAuth2 Resource Server |

```text
Navegador              Cliente                 Autorización               API
    | Ingresar            |                          |                     |
    |-------------------->|                          |                     |
    |<--------------------| redirección + state/nonce/PKCE                  |
    | Login y consentimiento ----------------------->|                     |
    |<-----------------------------------------------|                     |
    | Callback con código |                          |                     |
    |-------------------->| código + verifier ------>|                     |
    |                     |<---- ID/access/refresh ---|                     |
    | Cookie HttpOnly     |                          |                     |
    |<--------------------|                          |                     |
    | Consultar mascotas  |                          |                     |
    |-------------------->| Authorization: Bearer access_token ------------>|
    |                     |                          |       firma/JWKS, iss,
    |                     |                          |       aud, exp, scopes
    |                     |<-----------------------------------------------|
    |<--------------------|                          |                     |
```

Las contraseñas se envían únicamente al proveedor de identidad. El navegador no recibe
access tokens ni refresh tokens: el cliente los guarda en su sesión del lado del servidor.
El ID token identifica al usuario en el cliente; las llamadas a la API usan el **access token**.

## Ejecutar

Requisitos: **Java 17 o superior** y **Maven 3.6.3 o superior**.

Desde la raíz del repositorio, en cada terminal:

```powershell
cd 'TPN2/Ejercicio5/Ejercicio5e'
```

Iniciar los tres servicios en terminales separadas:

```powershell
mvn -pl autorizacion spring-boot:run
```

```powershell
mvn -pl servidor spring-boot:run
```

```powershell
mvn -pl cliente spring-boot:run
```

Abrir **http://127.0.0.1:8080**. Usar `127.0.0.1` en lugar de `localhost`: el callback
registrado es exactamente `http://127.0.0.1:8080/login/oauth2/code/local`.
Los tres servicios escuchan exclusivamente en loopback.

Para empaquetar, detener primero los servicios si se están ejecutando desde los JAR:

```powershell
mvn clean package
```

Ejecutar cada JAR en una terminal diferente:

```powershell
java -jar autorizacion/target/autorizacion-0.0.1-SNAPSHOT.jar
java -jar servidor/target/servidor-0.0.1-SNAPSHOT.jar
java -jar cliente/target/cliente-0.0.1-SNAPSHOT.jar
```

## Cuentas y prueba funcional

| Cuenta de demostración | Contraseña inicial |
|---|---|
| `usuario` | `Usuario123!` |
| `admin` | `Admin123!` |

Ambas cuentas pueden administrar **solo sus propios recursos**. El nombre `admin` no
habilita la edición de mascotas ajenas. También se pueden crear cuentas desde **Registrarme**.
El usuario de registro admite entre 3 y 40 letras minúsculas, números o guion bajo.
Las contraseñas se guardan con BCrypt en una base H2 de identidades separada.

1. Presionar **Ingresar**, autenticarse en el proveedor y aceptar los permisos solicitados.
2. En el primer acceso se crea el perfil local vinculado al emisor y al `sub` del token.
   La zona inicial es Centro; completar nombre, apellido y zona en **Mi perfil**.
   El correo aparece si el proveedor lo informa; el proveedor local utiliza nombres de usuario.
3. Crear una mascota con foto opcional. Editarla o darla de baja desde **Mis mascotas**.
4. Ingresar con otra cuenta y crear una mascota de la misma especie y zona y distinto sexo.
   Se puede usar otra ventana privada para mantener ambas sesiones abiertas.
5. En **Explorar**, seleccionar la mascota propia y enviar un voto.
6. La otra cuenta puede aceptar el voto en **Votos y matches**. El voto mutuo también genera match.
7. **Salir** cierra la sesión del cliente e inicia el cierre de sesión OIDC en el proveedor.

Las fotos se comprueban por su contenido PNG/JPEG, con máximo de 2 MB de entrada y
16 megapíxeles, y se recodifican. La exploración excluye mascotas propias, inactivas,
ya votadas, de otra zona o especie, o del mismo sexo. Los votos solo son visibles para
sus participantes. Este ejercicio no requiere SMTP ni envía correos.

## Seguridad del cliente

- Spring Security OAuth2 Client realiza el login OIDC y valida la respuesta del proveedor.
- Authorization Code con PKCE S256 obligatorio, `state` y `nonce`; no se usa password grant.
- Callback registrado exactamente y secreto de cliente utilizado únicamente del lado servidor.
- Los tokens se almacenan con `HttpSessionOAuth2AuthorizedClientRepository` y se envía
  solo una cookie `E5_CLIENT_SESSION` con `HttpOnly` y `SameSite=Lax` al navegador.
- No hay tokens OAuth en JavaScript, `localStorage`, `sessionStorage` ni en una API de sesión.
  El intercambio OIDC estándar usa el código en el callback y el ID token en el logout del proveedor.
- `OAuth2AuthorizedClientManager` permite renovar el access token mediante refresh token.
  El proveedor rota el refresh token y rechaza la reutilización del anterior.
- CSRF permanece activado en operaciones que modifican datos y en el logout.
  `/sesion` entrega el token CSRF, que el JavaScript agrega a sus peticiones.
- El proxy agrega el Bearer al consultar la API; no reenvía cookies del navegador ni acepta
  una dirección de destino enviada por el usuario. Conserva fotos multipart y respuestas de error.
- Si falla la renovación o la API responde 401, se invalida la sesión local y se solicita otro login.
- Se aplican CSP, escape de datos al generar HTML y cabeceras de seguridad de Spring.

## Seguridad del servidor

- OAuth2 Resource Server valida JWT RS256 usando la clave pública publicada en JWKS.
  La clave privada solo existe en el proveedor de autorización.
- Se comprueban firma, emisor (`iss`), audiencia (`aud=ejercicio5-api`), vencimiento (`exp`)
  y sujeto (`sub`). Un ID token dirigido al cliente no tiene la audiencia exigida por la API.
- `GET /api/**` requiere `api.read`; `POST`, `PUT` y `DELETE` requieren `api.write`.
- Además de scopes, los servicios verifican el propietario de cada mascota y voto.
  El propietario se obtiene de la identidad validada; no de un ID de usuario del formulario.
- La asociación del perfil usa **emisor + sub** con restricción única en la base,
  en lugar de asociar cuentas por un correo coincidente.
- La API no guarda contraseñas ni emite tokens propios. Es stateless, no acepta cookies
  de autenticación y no habilita CORS. Por eso deshabilita CSRF solo en este módulo.
- Un usuario dado de baja no puede seguir accediendo aunque conserve un JWT vigente.
- Sin token o con uno inválido: 401. Sin scope o ante recursos ajenos: 403.
  Datos inválidos: 400; recurso inexistente/inactivo: 404; duplicados: 409.

El access token vence a los **5 minutos** y el refresh token a los **30 minutos**.
El cierre de sesión elimina la sesión local y la del proveedor, pero no revoca instantáneamente
un JWT de acceso ya emitido: puede seguir siendo válido hasta su vencimiento. La API usa
validación local de JWT; no se implementa introspección ni una lista de revocación.

## Persistencia y configuración

Hay dos bases independientes: identidades del proveedor y datos del Tinder.
Las rutas H2 se interpretan desde el directorio de trabajo del proceso.
No conectar estos servicios a las bases de los ejercicios anteriores.

| Variable | Módulo | Valor predeterminado |
|---|---|---|
| `AUTH_PORT` | Autorización | `9000` |
| `CLIENT_PORT` | Cliente | `8080` |
| `API_PORT` | API | `8000` |
| `OAUTH_ISSUER` | Los tres | `http://127.0.0.1:9000` |
| `OAUTH_CLIENT_ID` | Autorización, cliente | `ejercicio5-cliente` |
| `OAUTH_CLIENT_SECRET` | Autorización, cliente | `cliente-local-demo-2026` |
| `CLIENT_URL` | Autorización | `http://127.0.0.1:8080` |
| `API_URL` | Cliente | `http://127.0.0.1:8000` |
| `OAUTH_JWKS_URI` | API | `http://127.0.0.1:9000/oauth2/jwks` |
| `OAUTH_AUDIENCE` | Autorización, API | `ejercicio5-api` |
| `AUTH_DB_URL` | Autorización | `jdbc:h2:file:./data/identidades-oauth2` |
| `AUTH_DB_USER` / `AUTH_DB_PASSWORD` | Autorización | `sa` / vacío |
| `DB_URL` | API | `jdbc:h2:file:./data/mascotas-oauth2` |
| `DB_USERNAME` / `DB_PASSWORD` | API | `sa` / vacío |
| `DEMO_USER_PASSWORD` | Autorización | `Usuario123!` |
| `DEMO_ADMIN_PASSWORD` | Autorización | `Admin123!` |

Los valores de contraseña inicial solo se aplican al crear las cuentas por primera vez.
Cambiar el puerto del proveedor requiere ajustar también el issuer y la URL de JWKS.
Cambiar el puerto del cliente requiere ajustar `CLIENT_URL` en el proveedor.

El proveedor conserva usuarios en H2, pero genera la clave RSA al arrancar y mantiene
clientes registrados, autorizaciones y consentimientos en memoria. Al reiniciarlo se
debe iniciar sesión otra vez; un refresh token anterior no puede renovarse.
Esto es una configuración de desarrollo local. Para un despliegue público se necesitan
HTTPS, cookies Secure, secretos fuera del código, persistencia de claves y autorizaciones,
límites de intentos y migraciones de esquema en lugar de `ddl-auto=update`.

## API conservada

Todas estas rutas requieren un access token válido si se llama directamente al puerto 8000.
Desde el navegador se consumen a través del cliente en el puerto 8080.

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/zonas` | Listar zonas |
| GET / PUT | `/api/usuarios/me` | Consultar / editar perfil propio |
| GET / PUT | `/api/usuarios/me/foto` | Consultar / cargar foto propia |
| GET / POST | `/api/mascotas` | Listar propias / crear mascota |
| PUT / DELETE | `/api/mascotas/{id}` | Editar / dar de baja mascota propia |
| GET / PUT | `/api/mascotas/{id}/foto` | Consultar / cargar foto |
| GET | `/api/mascotas/explorar?idMascotaPropia={id}` | Buscar compatibles |
| GET / POST | `/api/votos` | Consultar / enviar votos |
| POST | `/api/votos/{id}/responder` | Aceptar voto recibido y hacer match |

Crear/editar mascota: `{"nombre":"Luna","sexo":"HEMBRA","tipo":"PERRO"}`.
Especies: `PERRO`, `GATO`, `CONEJO`; sexos: `MACHO`, `HEMBRA`.
Votar: `{"idMascotaPropia":"ID_ORIGEN","idMascotaVotada":"ID_DESTINO"}`.
Perfil: `{"nombre":"Ana","apellido":"Pérez","idZona":"ID_ZONA"}`.
Fotos: `multipart/form-data` con campo `archivo`.

## Pruebas

```powershell
mvn test
```

Incluye 21 pruebas: 5 del proveedor, 8 de la API y 8 del cliente.
Las pruebas de API validan JWT firmados realmente y obtienen claves de un servidor JWKS
de prueba. Las bases de los tests son H2 en memoria y no modifican los datos locales.
Las pruebas del cliente verifican redirecciones, PKCE, CSRF, uso del Bearer, fotos y errores.

También se incluye una prueba HTTP del flujo completo, con Node 22 o superior:

```powershell
node scripts/verificar-flujo.cjs
```

Ejecutarla contra **instancias de prueba con las cuentas de demostración y zonas iniciales**.
El script ingresa con ambas cuentas, crea dos mascotas, comprueba aislamiento, carga una
foto, genera un match, da de baja las mascotas de prueba y realiza logout. No imprime
tokens ni contraseñas. Acepta tres argumentos opcionales: URL del cliente, del proveedor
y de la API. Para aislar la prueba se pueden iniciar los JAR con
`--spring.datasource.url=jdbc:h2:mem:prueba` en autorización y servidor.

Referencias: [Spring Authorization Server](https://docs.spring.io/spring-authorization-server/reference/getting-started.html),
[OAuth2 Client](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/client/authorized-clients.html)
y [Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
