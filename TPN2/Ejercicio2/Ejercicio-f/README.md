# Tinder de Mascotas con API REST

Versión del ejercicio `TPN1/Ejercicio5/Ejercicio 5c` con endpoints JSON, un cliente HTTP **RestTemplate** y DTOs convertidos con **MapStruct**. El proyecto está en `TPN2/Ejercicio-f`, la carpeta solicitada; `TPN2/Ejercicio f` ya existía vacía.

Requiere JDK 17 o superior. Incluye Maven Wrapper; también permite usar `mvn` instalado. Usa Spring Boot 3.4.3, JPA, Hibernate Envers, Bean Validation y MapStruct 1.6.3. H2 guarda los datos en `data/mascotas.mv.db` al ejecutar la aplicación; los tests usan una base en memoria independiente. No requiere MySQL ni SMTP.

## Ejecutar y probar en PowerShell

Desde la raíz del repositorio:

```powershell
cd TPN2/Ejercicio-f
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Abrí **http://localhost:8080** en el navegador para usar la interfaz. La API escucha en `http://localhost:8080/api`. Con el servidor en ejecución, abrí otra terminal en `TPN2/Ejercicio-f` y ejecutá:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\probar-api.ps1
```

El script crea una zona y dos usuarios, registra mascotas compatibles, consulta candidatos, envía un voto, lo acepta, verifica el match, edita y da de baja una mascota. Imprime `PRUEBA REST OK` si todo funcionó. Usa mails únicos para poder repetirlo y deja los datos de ejemplo en la base local.

Para ejecutar la demostración Java que consume la API con **RestTemplate**, detené el servidor anterior con Ctrl+C y ejecutá:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--demo=true"
```

Al iniciar imprime candidatos, matches y `DEMO RESTTEMPLATE OK`. El servidor queda ejecutándose hasta Ctrl+C. Cada ejecución crea nuevos datos de ejemplo. `TinderRestClient` también puede instanciarse desde otro programa Java con un `RestTemplate` y la URL del servidor.

Alternativa con el JAR empaquetado:

```powershell
.\mvnw.cmd clean package
java -jar target/tinder-mascotas-rest-1.0.0.jar --demo=true
```

Para usar otro puerto, configurá también la URL del cliente de la demostración:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081 --tinder.api-url=http://localhost:8081 --demo=true"
powershell -NoProfile -ExecutionPolicy Bypass -File .\probar-api.ps1 -BaseUrl http://localhost:8081
```

Si ya tenés Maven, podés reemplazar `.\mvnw.cmd` por `mvn` en estos comandos.

## Usar la interfaz en la exposición

La interfaz está en **http://localhost:8080/** y reutiliza **One Page Wonder v5.0.7**, la misma plantilla del ejercicio original: Bootstrap 4, jQuery, fuentes Catamaran/Lato, imágenes, barra oscura, cabecera con degradado y círculos, formularios y tablas. El path `/api` contiene los endpoints; se consume desde la página y desde el cliente Java. No necesitás instalar Node ni ejecutar otro servidor: Spring Boot sirve el HTML, los recursos originales y los scripts del frontend.

Para una demostración de unos cinco minutos:

1. Iniciá el servidor con `.\mvnw.cmd spring-boot:run` y abrí `http://localhost:8080`.
2. Mostrá la sección **Demostración de la API REST**, siempre visible debajo del contenido principal y antes del pie original. El inspector muestra las últimas 30 peticiones con método, endpoint, código HTTP, duración, JSON enviado y respuesta. Oculta claves y Authorization.
3. Pulsá **Preparar una demostración**. Crea, mediante peticiones reales a la API, una zona y dos cuentas nuevas: Ana con Luna y Juan con Toby. Inicia sesión como Ana. Los botones de cambio de cuenta quedan disponibles mientras mantengas abierta la página.
4. Abrí **Explorar**, elegí **Explorar con Luna** y pulsá **Votar** en la fila de Toby. La columna «Dueño» muestra a Juan Demo, obtenida del DTO. Mostrá el `GET /api/mascotas/{id}/candidatos` y el `POST /api/votos` con respuesta **201**. El voto todavía tiene `match: false`.
5. Pulsá **Entrar como Juan**, abrí **Votos recibidos** y elegí **Aceptar y hacer match**. Mostrá el `PUT /api/votos/{id}/respuesta` con respuesta **200** y `match: true`.
6. Volvé a Ana y abrí **Matches**: ambas cuentas consultan el mismo match mediante `GET /api/matches`.
7. En **Mis Mascotas**, usá **¡Agrega una Mascota!**, **Editar** o **Eliminar**. El formulario permite adjuntar una foto. Para rehabilitarla, abrí **Mascotas dadas de baja**, elegí **Dar Alta** y confirmá. En **Perfil** podés modificar nombre, apellido, mail, zona, clave y foto.

También podés registrar tus propias cuentas desde la pantalla inicial; permite crear zonas cuando aún no hay ninguna. Para generar candidatos, usá la misma zona y tipo, distinto sexo y otro dueño. La demo genera mails únicos y puede repetirse sin borrar la base. Las cuentas de ejemplo quedan en H2, pero el navegador conserva sus credenciales solo en memoria; recargar la página cierra la sesión y elimina los botones para acceder a esas cuentas. Podés preparar otra demo o iniciar sesión con una cuenta propia.

### Rutas del frontend y fotos

Las pantallas recuperan las rutas originales `/login`, `/registro`, `/inicio`, `/usuario/editar-perfil`, `/mascota/mis-mascotas`, `/mascota/editar-perfil` y `/mascota/explorar-mascotas`. Se agregan `/mascota/mascotas-de-baja`, `/votos/recibidos` y `/matches`. El formulario admite `?id=...&accion=Actualizar`, `Eliminar` o `Alta`; la exploración admite `?idMascotaPropia=...`.

History API permite navegar y usar atrás/adelante sin recargar. Un controlador sirve la entrada HTML en esas rutas explícitas; el navegador llena el marcado adaptado del original mediante REST. No se ejecutan expresiones Thymeleaf ni se envían formularios a los controladores MVC anteriores. Si abrís o recargás una pantalla protegida, verás el login; después de autenticarte vuelve a la pantalla solicitada. Al actualizar mail/clave, las siguientes peticiones usan inmediatamente las nuevas credenciales.

El perfil o la mascota se guarda primero como JSON y su foto se envía después como multipart. Si falla la foto, el registro guardado se conserva y aparece **Reintentar foto** en la sección de exposición. El archivo pendiente vive solo en memoria y se descarta al cambiar de cuenta, salir o recargar; también se puede continuar sin esa foto. Las imágenes protegidas se descargan con Authorization y se muestran mediante URLs temporales. Los perfiles sin foto usan la imagen original `/img/m1.jpeg`.

Si ya tenías el servidor abierto antes de actualizar el frontend, detenelo con Ctrl+C y volvé a ejecutar `.\mvnw.cmd spring-boot:run`. No levantes dos instancias simultáneas con la misma base H2 de archivos. Las fuentes externas requieren conexión a Google Fonts; Bootstrap, jQuery, los estilos de la plantilla y las imágenes son locales.

### Cómo explicar API REST, MapStruct y RestTemplate

El recorrido de una acción es:

```text
Interfaz del navegador (fetch) ── HTTP + JSON ──┐
                                              ├─ TinderControlador
Cliente Java (RestTemplate) ─── HTTP + JSON ────┘        │
                                                  TinderServicio
                                                       │
                                            Repositorios JPA → H2
                                                       │
                                      Entidades → MapStruct → DTOs
                                                       │
                                              Respuesta JSON
```

Podés explicarlo así: «La interfaz es un cliente de la API. Cuando envío un voto, hace un POST con los IDs de las mascotas. El controlador recibe el DTO y el servicio valida quién está autenticado y si las mascotas son compatibles. JPA guarda el voto; MapStruct convierte la entidad a un DTO y la API devuelve JSON. Al aceptar el voto, un PUT actualiza su respuesta y crea el match».

**MapStruct** convierte entre entidades y DTOs; por ejemplo, transforma `mascota.usuario.id` en `usuarioId` y evita devolver todo el objeto Usuario con su clave. Mostrá `mapper/MascotasMapper.java` y la implementación generada en `target/generated-sources/annotations`.

**RestTemplate** es el cliente HTTP de Java en `cliente/TinderRestClient.java`. El navegador utiliza `fetch`, porque RestTemplate pertenece a Java. Para demostrar también ese requisito, ejecutá el programa con `--demo=true` siguiendo el comando anterior: consume los mismos endpoints e imprime el flujo completo en la terminal. Tanto la interfaz como ese cliente guardan y consultan datos reales mediante la API.

**REST** se muestra con recursos (`/mascotas`, `/votos`), métodos (`GET`, `POST`, `PUT`, `DELETE`), DTOs JSON y códigos HTTP. Cada petición protegida lleva sus credenciales Basic: el servidor no depende de una sesión HTTP iniciada por el navegador. El botón «Cerrar sesión» borra las credenciales en memoria del cliente.

## Contratos y autenticación

Las zonas, el registro y el login son públicos. El resto requiere `Authorization: Basic <base64(mail:clave)>` en cada petición. `/auth/login` verifica credenciales y devuelve el perfil; no emite un token ni abre una sesión. Las claves se almacenan con BCrypt y nunca se devuelven en los DTOs. Los permisos usan al usuario autenticado, sin aceptar un ID de dueño enviado por el cliente.

Registro o modificación del perfil (`UsuarioRequest`):

```json
{
  "nombre": "Ana",
  "apellido": "Perez",
  "mail": "ana@example.com",
  "clave": "mascotas123",
  "clave2": "mascotas123",
  "zonaId": "ID_DEVUELTO_AL_CREAR_ZONA"
}
```

Alta o modificación de mascota (`MascotaRequest`):

```json
{"nombre":"Luna","sexo":"HEMBRA","tipo":"PERRO"}
```

Valores de `sexo`: `MACHO`, `HEMBRA`. Valores de `tipo`: `PERRO`, `GATO`, `CONEJO`.

Voto (`VotoRequest`):

```json
{"mascota1Id":"ID_MASCOTA_PROPIA","mascota2Id":"ID_MASCOTA_DESTINO"}
```

## Endpoints

Todos los paths de esta tabla llevan el prefijo `/api`.

| Método | Path | Función |
|---|---|---|
| GET | `/zonas` | Listar zonas (público) |
| GET | `/zonas/{id}` | Consultar zona (público) |
| POST | `/zonas` | Crear zona con `nombre` y `descripcion` (público) |
| POST | `/usuarios` | Registrar usuario (público) |
| POST | `/auth/login` | Verificar `mail` y `clave` (público) |
| GET | `/usuarios/me` | Consultar perfil propio |
| PUT | `/usuarios/me` | Modificar perfil, mail, zona y clave |
| DELETE | `/usuarios/me` | Deshabilitar usuario |
| PUT | `/usuarios/me/habilitar` | Rehabilitar con las credenciales del usuario deshabilitado |
| PUT | `/usuarios/me/foto` | Subir foto propia |
| GET | `/mascotas?incluirBajas=false` | Listar mascotas propias; `true` incluye bajas |
| POST | `/mascotas` | Crear mascota propia |
| GET | `/mascotas/{id}` | Consultar mascota disponible |
| PUT | `/mascotas/{id}` | Editar mascota propia |
| DELETE | `/mascotas/{id}` | Dar de baja mascota propia |
| PUT | `/mascotas/{id}/habilitar` | Rehabilitar mascota propia |
| GET | `/mascotas/{id}/candidatos` | Explorar desde una mascota propia |
| PUT | `/mascotas/{id}/foto` | Subir foto de mascota propia |
| GET | `/fotos/{id}` | Descargar imagen con su MIME |
| POST | `/votos` | Votar una mascota compatible |
| GET | `/votos/{id}` | Consultar voto en el que participa el usuario |
| GET | `/votos/recibidos` | Consultar votos recibidos disponibles |
| PUT | `/votos/{id}/respuesta` | Aceptar voto como dueño de la mascota destino |
| GET | `/matches` | Listar votos aceptados de ambas partes |

Las altas devuelven **201** y `Location`; las bajas **204**. Las validaciones devuelven **400**, credenciales ausentes/incorrectas **401**, permisos insuficientes **403**, recursos inexistentes/no disponibles **404**, y duplicados/conflictos **409**. Los errores usan `ProblemDetail`, con detalle y errores de validación sin exponer las claves.

## Reglas del Tinder

- Los candidatos tienen el mismo tipo y zona, sexo opuesto y otro dueño; ambos usuarios y mascotas deben estar activos.
- Se excluyen los destinos que la mascota de origen ya votó.
- No se permite autovotar, votar mascotas propias, votar mascotas incompatibles ni repetir un voto dirigido.
- El dueño del destino acepta el voto mediante `PUT /votos/{id}/respuesta`; eso crea el match. Repetir la aceptación conserva la fecha original.
- Las bajas son lógicas y Envers conserva el historial de las entidades originales. Los votos de mascotas/usuarios dados de baja no aparecen como recibidos disponibles ni como matches activos.
- Las fotos se envían como `multipart/form-data` con un campo `archivo` (JPEG, PNG, GIF o WebP; máximo 5 MB). El contenido se entrega desde `/fotos/{id}` y queda fuera de los DTOs y del historial de bytes de Envers.

Ejemplo de foto usando curl en Windows, reemplazando las credenciales y el ID:

```powershell
curl.exe -u "ana@example.com:mascotas123" -X PUT -F "archivo=@C:/imagenes/luna.png;type=image/png" http://localhost:8080/api/mascotas/ID_MASCOTA/foto
```

## Organización y pruebas

`entidades` y `enumeracion` parten del original. `repositorios` maneja JPA, `servicios/TinderServicio` concentra las reglas y transacciones, y `controladores/TinderControlador` expone REST. `dto/Dto` define contratos de entrada y salida. `mapper/MascotasMapper` genera conversiones con `componentModel = "spring"`; Maven configura `mapstruct-processor`. La implementación generada se encuentra en `target/generated-sources/annotations` después de compilar y no se edita a mano.

`cliente/TinderRestClient` consume endpoints usando `postForObject` y `exchange`, con `ParameterizedTypeReference` para listas. `cliente/DemoRestTemplate` ejecuta un caso completo con `--demo=true`.

`TinderApiTests` inicia un servidor en puerto aleatorio y lo consume mediante RestTemplate: flujo hasta el match, autenticación, permisos, errores, filtros de compatibilidad, bajas y rehabilitación, cambio de credenciales, multipart/descarga, revisiones Envers, rutas del frontend y recursos originales. La propiedad de Byte Buddy en Surefire permite correr las dependencias de pruebas también con el JDK 26 instalado en este entorno.

`static/views.js` contiene el HTML adaptado de las nueve plantillas originales. `static/api.js` maneja HTTP y el estado en memoria; `static/app.js` conecta la navegación y los formularios. `styles.css` agrega únicamente ajustes funcionales y estilos de las herramientas de exposición; el diseño principal sigue en el CSS original de One Page Wonder, con sus avisos de licencia.

`MascotaDto` incluye `usuarioNombre` y `usuarioApellido`, mapeados desde el dueño con MapStruct, para mostrar la columna «Dueño». No devuelve claves ni la entidad Usuario completa. Los endpoints y el cliente RestTemplate conservan el resto de sus contratos.

La prueba opcional del frontend en `src/test/js/interfaz.cjs` ejecuta los scripts reales en un DOM de pruebas y consume una API levantada. Comprueba formularios, navegación, fotos y su reintento, credenciales nuevas, voto/match y el ocultamiento de claves. Requiere Node 24 o superior y no hace una comparación visual de píxeles. Desde `TPN2/Ejercicio-f`, con el servidor funcionando:

```powershell
npm.cmd install --prefix target/ui-check --no-save --package-lock=false jsdom@30.1.2
node src/test/js/interfaz.cjs http://localhost:8080
```

La dependencia de pruebas queda en `target/`, no es necesaria para ejecutar la aplicación. `clean` la elimina. Esta prueba crea cuentas de ejemplo únicas en la API indicada.

No se envían emails: los votos recibidos y matches se consultan por REST, sin configurar el servicio SMTP del original. La interfaz usa HTML, CSS y JavaScript y consume la API mediante `fetch`; el cliente Java de demostración utiliza RestTemplate.

Referencias: [RestTemplate en Spring Framework 6.2](https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html) y [documentación de MapStruct](https://mapstruct.org/documentation/reference-guide/).
