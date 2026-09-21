# Plan de Pruebas y Ejemplos de Pruebas
## Ejercicios 5c: sistema de Mascotas y 5d: sistema de Videojuegos

**Rol:** Ingenieria de QA Senior  
**Alcance documental:** analisis estatico de controladores, servicios, repositorios, entidades, configuracion y vistas Thymeleaf de los ejercicios 5c y 5d.

> Este documento describe una estrategia de pruebas. No implementa pruebas ni modifica el codigo de los ejercicios.

---

# Parte 1. Plan de Pruebas

## 1. Objetivo

Evaluar la correccion funcional, la persistencia, las reglas de negocio, el control de acceso y el comportamiento web de los sistemas de Mascotas y Videojuegos.

El plan busca verificar especialmente:

- Que las operaciones de alta, consulta, modificacion y baja logica respeten las reglas implementadas.
- Que los controladores coordinen correctamente las vistas Thymeleaf, los servicios y los repositorios.
- Que las validaciones de datos, estados activos/inactivos y errores se reflejen en la interfaz.
- Que la informacion persistida pueda recuperarse de forma consistente.
- Que el sistema mantenga un comportamiento aceptable bajo concurrencia, carga sostenida y saturacion.

## 2. Alcance

### 2.1. Ejercicio 5c: Mascotas

Se incluyen las siguientes capacidades observadas en el codigo:

- Registro de usuarios con nombre, apellido, correo, clave, zona y fotografia.
- Inicio y cierre de sesion mediante `UsuarioServicio.login` y la sesion HTTP `usuariosession`.
- Validacion de usuario, coincidencia de claves y longitud minima de la clave.
- Consulta y actualizacion del perfil de usuario.
- Alta de mascotas con nombre, sexo, tipo, usuario propietario, fecha de alta y fotografia.
- Listado de mascotas activas pertenecientes al usuario autenticado.
- Modificacion de una mascota, con verificacion de que el usuario sea su propietario.
- Baja logica de mascotas mediante la fecha `baja`.
- Consulta de una mascota activa por identificador.
- Votacion y respuesta de votos entre mascotas, incluyendo notificaciones.
- Persistencia de fotografias mediante `FotoServicio` y acceso a imagenes a traves de `/foto/usuario/{id}` y `/foto/mascota/{id}`.
- Auditoria de la entidad `Mascota` mediante Hibernate Envers.

Rutas web principales consideradas: `/login`, `/logout`, `/registrar`, `/inicio`, `/usuario/editar-perfil`, `/usuario/actualizar-perfil`, `/mascota/mis-mascotas`, `/mascota/editar-perfil`, `/mascota/actualizar-perfil` y `/mascota/eliminar-perfil`.

### 2.2. Ejercicio 5d: Videojuegos

Se incluyen las siguientes capacidades observadas en el codigo:

- Listado de videojuegos activos en el inicio y en `/exploradorJuegos`.
- Alta de videojuegos con titulo, descripcion, imagen, precio, stock, fecha de lanzamiento, estudio y categoria.
- Validacion de campos mediante Bean Validation.
- Validacion de imagen obligatoria en el alta, formato legible como imagen y limite de 15 MB.
- Almacenamiento de imagenes en la ruta configurada por el controlador.
- Modificacion de videojuegos, con reemplazo opcional de la imagen.
- Consulta de un videojuego activo y consulta de su detalle mediante `/detalleVideoJuego/{id}`.
- Baja logica mediante alternancia del atributo `activo`.
- Busqueda por titulo mediante `findByTitle`.
- Gestion de categorias y estudios: listado, alta, modificacion, consulta y baja logica segun sus controladores y servicios.

Rutas web principales consideradas: `/`, `/inicio`, `/exploradorJuegos`, `/altaVideojuego`, `/modificarVideojuego`, `/videojuego/aceptarEditVideojuego`, `/consultarVideojuego`, `/bajaVideojuego`, `/detalleVideoJuego/{id}`, `/categorias`, `/altaCategoria`, `/modificarCategoria`, `/consultarCategoria` y `/bajaCategoria`.

**Delimitacion funcional:** el ejercicio 5d no presenta una entidad de compra, carrito, orden ni endpoint de checkout. El precio y el stock existen como atributos de `Videojuego`, pero no se observa una operacion de compra ni un descuento de stock. Por ello, las pruebas de este plan cubren la gestion del catalogo y no una compra no implementada.

## 3. Fuera de alcance

- Funcionalidades no presentes en el codigo analizado, como pagos, carrito, pedidos o decremento de stock en 5d.
- Pruebas de infraestructura externa que no forma parte de estos proyectos.
- Modificacion, refactorizacion o correccion del codigo fuente.
- Validacion de reglas de negocio no expresadas en controladores, servicios, entidades, repositorios o plantillas analizados.

## 4. Entornos de Prueba

### 4.1. Entorno funcional

- Sistema operativo: entorno compatible con Java y Maven Wrapper.
- Aplicacion: cada ejercicio ejecutado de forma independiente.
- Servidor web: contenedor embebido provisto por la aplicacion Spring.
- Navegador: version estable de Chrome o Firefox, con viewport de escritorio y movil.
- Cliente HTTP: herramienta capaz de enviar formularios `GET`, `POST` y cargas `multipart/form-data`.

### 4.2. Persistencia del ejercicio 5c

- Motor configurado: MySQL.
- Base configurada: `mascotas_db`.
- Verificacion de tablas JPA para usuarios, mascotas, fotos, zonas y votos.
- Verificacion de tablas de auditoria generadas por Hibernate Envers para entidades auditadas.
- Datos de prueba aislados por ejecucion, evitando interferencias entre usuarios, mascotas y votos.

### 4.3. Persistencia del ejercicio 5d

- Motor configurado: SQLite.
- Archivo configurado: `database.db`.
- Verificacion de tablas de videojuegos, categorias y estudios.
- Datos de prueba con registros activos e inactivos, asociaciones validas y casos limite de precio, stock y fecha.
- Directorio de imagenes de prueba con archivos validos, archivos no imagen y archivos que superen 15 MB.

### 4.4. Datos y perfiles

Se requieren, como minimo:

- Usuarios habilitados, deshabilitados y usuarios sin mascotas en 5c.
- Mascotas con y sin foto, activas y dadas de baja, con propietarios distintos.
- Votos validos, autovoto y votos sobre mascotas no pertenecientes al usuario.
- Videojuegos activos e inactivos, con titulo repetido o parcialmente coincidente.
- Categorias y estudios existentes y ausentes para verificar asociaciones obligatorias.

## 5. Estrategia de Pruebas

### 5.1. Enfoque por niveles

Se aplicara una piramide de pruebas:

1. **Unitarias:** reglas de servicios aisladas con mocks de repositorios, fotografia y notificaciones.
2. **Integracion:** controladores, servicios, repositorios y base de datos real de prueba.
3. **Sistema/E2E:** navegacion desde las vistas Thymeleaf hasta la confirmacion de persistencia.
4. **No funcionales:** carga, rendimiento y estres sobre rutas de lectura y operaciones de escritura relevantes.

### 5.2. Prioridades de riesgo

**Prioridad alta:**

- Login, bloqueo de usuarios dados de baja y proteccion de rutas de mascotas en 5c.
- Verificacion de propietario antes de modificar o eliminar una mascota.
- Exclusión de mascotas dadas de baja en `buscarMascotasPorUsuario`.
- Alta y modificacion de videojuegos con validacion de datos e imagen.
- Baja logica y filtrado de videojuegos activos.
- Persistencia de imagenes y relaciones obligatorias con categoria y estudio.

**Prioridad media:**

- Listados, detalle, consulta y navegacion entre vistas.
- Gestion de categorias y estudios.
- Votos, respuestas y envio de notificaciones.
- Auditoria de cambios de mascotas.

**Prioridad baja:**

- Estilos visuales, textos estaticos y adaptacion de layout, siempre que no impidan la ejecucion del flujo.

### 5.3. Tecnicas

- Particion de equivalencia para credenciales, estados activo/inactivo, imagenes y relaciones.
- Analisis de valores limite para clave, precio, stock, descripcion, fecha y tamano de imagen.
- Tabla de decision para permisos por propietario y estados de usuario/mascota/videojuego.
- Pruebas de transicion de estados para altas, bajas logicas y rehabilitacion de usuarios.
- Pruebas negativas para identificadores inexistentes, formularios incompletos, sesiones ausentes y archivos invalidos.
- Pruebas de regresion sobre los flujos de listado despues de cada cambio de estado.

### 5.4. Observabilidad y evidencias

Cada ejecucion deberia conservar:

- URL, metodo HTTP, datos enviados, usuario de prueba y estado previo de la base.
- Resultado HTTP, vista renderizada, mensaje de error o redireccion.
- Registro persistido antes y despues de la operacion.
- Para 5c, revision generada y datos auditados cuando corresponda.
- Para cargas, tasa de errores, latencia media, percentiles altos, throughput y consumo de recursos.

## 6. Criterios de Aceptacion

### 6.1. Criterios funcionales

- Un usuario valido de 5c puede iniciar sesion y acceder a `/inicio`; un usuario no autenticado es redirigido al login.
- Un usuario dado de baja no puede autenticarse.
- El alta de usuario y mascota exige los datos obligatorios definidos por los servicios.
- Una mascota nueva queda asociada al usuario autenticado y aparece en su listado.
- Un usuario no puede modificar ni dar de baja una mascota de otro propietario.
- Una mascota dada de baja no aparece en el listado de mascotas activas ni en su consulta activa.
- Los cambios auditables de mascotas quedan registrados por Envers.
- En 5d, solo los videojuegos activos aparecen en inicio, explorador, busqueda por titulo y consulta activa.
- El alta rechaza titulo vacio, descripcion fuera de rango, precio o stock fuera de limites, fecha futura, categoria/estudio ausente, imagen vacia, imagen invalida o imagen de 15 MB o mas.
- La baja logica cambia el estado `activo` y el registro deja de aparecer en las consultas activas.
- Las operaciones validas redirigen a la vista esperada y los errores se muestran mediante la vista de error o el formulario correspondiente.

### 6.2. Criterios no funcionales orientativos

Los umbrales definitivos deben acordarse antes de la ejecucion formal. Como linea base inicial:

- Al menos 95 % de las solicitudes de lectura deben responder dentro de 1 segundo en el entorno de prueba bajo carga nominal.
- Al menos 95 % de las operaciones de escritura validas deben responder dentro de 2 segundos, sin incluir tiempos extraordinarios de preparacion del archivo de imagen.
- Error funcional o HTTP 5xx inferior al 1 % durante la carga nominal.
- No debe haber perdida, duplicacion ni corrupcion de datos despues de una prueba concurrente.
- En estres, el sistema debe degradarse de forma observable y recuperarse sin dejar transacciones parcialmente persistidas.

### 6.3. Condicion de aprobacion

Un ejercicio se considera aceptable cuando todos los casos de prioridad alta pasan, no existen defectos criticos o altos abiertos, los criterios funcionales se cumplen y las mediciones no funcionales alcanzan los umbrales acordados. Los casos no ejecutados deben quedar explicitamente justificados.

---

# Parte 2. Ejemplos de tipos de pruebas aplicados

## 1. Pruebas Unitarias

Las pruebas unitarias aislarian los servicios de sus dependencias mediante mocks. El objetivo seria verificar decisiones y cambios de estado, no la capacidad de JPA para persistir.

### 5c: Mascotas y usuarios

- `MascotaServicio.validar`: comprobar que rechaza nombre nulo o vacio y sexo nulo, y que acepta una combinacion valida.
- `MascotaServicio.agregarMascota`: simular usuario, fotografia y repositorio; verificar que construye una mascota con usuario, tipo, sexo, fecha de alta y foto y que invoca el guardado.
- `MascotaServicio.modificar`: comprobar actualizacion cuando el propietario coincide, rechazo cuando no coincide y error cuando el identificador no existe.
- `MascotaServicio.eliminar`: comprobar que realiza baja logica asignando fecha, sin eliminar fisicamente el registro.
- `MascotaServicio.buscarMascota`: verificar rechazo de id vacio, devolucion de mascota activa y rechazo de mascota con fecha de baja.
- `UsuarioServicio.validar`: cubrir campos obligatorios, clave de mas de seis caracteres, coincidencia entre claves y zona valida.
- `UsuarioServicio.login`: aislar `UsuarioRespositorio`; probar credenciales correctas, usuario inexistente, usuario deshabilitado, clave incorrecta y campos vacios.
- `UsuarioServicio.registrar`, `modificar`, `deshabilitar` y `habilitar`: verificar asociaciones con zona, fotografia y transiciones de la fecha de baja.
- `VotoServicio.votar`: probar autovoto, mascota inexistente, falta de permisos, guardado del voto y notificacion.
- `VotoServicio.responder`: probar respuesta autorizada, guardado de la fecha y notificacion del match.

### 5d: Videojuegos

- `VideojuegoService.findAll`, `findById`, `saveOne` y `updateOne`: simular `VideojuegoRepository` y comprobar delegacion, resultado y propagacion de errores.
- `VideojuegoService.deleteById`: verificar que cambia `activo` y guarda el videojuego, y que informa error para un id inexistente.
- `VideojuegoService.findAllByActivo`: comprobar que retorna solamente el resultado del repositorio de activos.
- `VideojuegoService.findByIdAndActivo`: comprobar consulta de un registro activo y error ante un registro no encontrado.
- `VideojuegoService.findByTitle`: verificar que delega la busqueda parcial por titulo.
- `VideojuegoController.validarExtension`: aislar un `MultipartFile` con contenido de imagen valido y otro no interpretable como imagen.
- Validaciones de `Videojuego`: cubrir titulo vacio, descripcion menor de 5 o mayor de 100 caracteres, precio menor de 5 o mayor de 10000, stock menor de 1 o mayor de 10000, fecha futura y categoria/estudio nulos.

## 2. Pruebas de Integracion

Estas pruebas utilizarian el contexto Spring y una base de datos de prueba, verificando el recorrido controlador-servicio-repositorio-base de datos.

### 5c: flujo de mascotas

1. Crear un usuario y una zona validos mediante el flujo de registro.
2. Iniciar sesion y enviar el formulario de `/mascota/actualizar-perfil` sin id, con nombre, sexo, tipo y archivo.
3. Comprobar que `MascotaRepositorio` contiene la mascota asociada al usuario, con fecha de alta y foto persistidas.
4. Modificar la misma mascota y comprobar que conserva el identificador, cambia sus datos y actualiza la fotografia cuando corresponde.
5. Ejecutar `/mascota/eliminar-perfil` y verificar que `baja` deja de ser nula, pero el registro permanece.
6. Consultar `/mascota/mis-mascotas` y confirmar que la consulta JPQL solo devuelve mascotas del usuario cuya baja es nula.
7. Revisar la revision de Envers generada por los cambios de la mascota.

Tambien se integraria el login con usuarios activos y deshabilitados, y el flujo de voto con repositorios de mascotas y votos.

### 5d: flujo de catalogo

1. Preparar una categoria y un estudio persistidos.
2. Enviar el formulario de `/altaVideojuego` con un videojuego valido y una imagen valida.
3. Comprobar que la entidad se guarda con sus relaciones, nombre de imagen y estado activo.
4. Consultar `/inicio`, `/exploradorJuegos` y `/detalleVideoJuego/{id}`; verificar que el registro activo se recupera y renderiza.
5. Ejecutar la modificacion con y sin nuevo archivo y comprobar el estado persistido.
6. Ejecutar la baja logica y confirmar que el registro continua en la base pero ya no aparece en `findAllByActivo` ni en `findByIdAndActivo`.
7. Verificar que la busqueda por titulo solo devuelve coincidencias activas.

## 3. Pruebas de Sistema (E2E)

### 5c: alta y gestion desde Thymeleaf

- Abrir la vista de login, registrar un usuario desde `/registro`, completar nombre, apellido, mail, claves, zona y foto, y confirmar la pantalla de exito.
- Iniciar sesion desde la vista `login.html` y verificar la redireccion a `/inicio`.
- Entrar en `/mascota/mis-mascotas`, abrir el formulario `mascota.html`, cargar nombre, tipo, sexo y foto, y enviar "Crear Mascota".
- Volver al listado y comprobar que la fila muestra nombre, sexo, tipo y fotografia.
- Abrir "Editar", modificar datos, volver a consultar y comprobar persistencia.
- Abrir "Eliminar", confirmar la baja y comprobar que la mascota ya no aparece en el listado activo.
- Cerrar sesion y verificar que el acceso posterior a `/inicio` redirige a `/login`.

### 5d: alta y gestion desde Thymeleaf

- Abrir el inicio o explorador y verificar que las tarjetas se construyen desde la coleccion `videojuegos`.
- Acceder al formulario de alta, completar titulo, descripcion, precio, stock, fecha, categoria, estudio e imagen, y guardar.
- Confirmar la redireccion a `/inicio`, localizar el videojuego y abrir su detalle.
- Acceder a modificar, cambiar datos y opcionalmente reemplazar la imagen; comprobar el resultado en la vista.
- Consultar el registro en modo solo lectura y verificar que los campos aparecen deshabilitados.
- Ejecutar la baja logica y comprobar que el videojuego deja de mostrarse en inicio y explorador.
- Repetir con datos invalidos y verificar que el formulario conserva los datos utiles y muestra el error correspondiente.

La asercion final de cada flujo debe contrastar la pantalla con el estado real de la base de datos, no solo con el texto renderizado.

## 4. Pruebas de Carga y Rendimiento

La carga debe separar lecturas, escrituras y operaciones con archivos para evitar que una categoria o endpoint distorsione toda la medicion.

### Endpoints de 5c

- `GET /login`: concurrencia de accesos a la pantalla.
- `POST /login`: concurrencia de autenticaciones validas y fallidas, midiendo consultas a `buscarPorMail`.
- `GET /inicio`: lectura protegida por sesion.
- `GET /mascota/mis-mascotas`: consulta JPQL por usuario con filtrado de bajas.
- `GET /mascota/editar-perfil`: carga del formulario y, cuando existe id, consulta de mascota.
- `POST /mascota/actualizar-perfil`: altas y modificaciones con `multipart/form-data`; debe medirse por separado con y sin imagen.
- `GET /foto/mascota/{id}` y `GET /foto/usuario/{id}`: descarga concurrente de imagenes.

### Endpoints de 5d

- `GET /`, `GET /inicio` y `GET /exploradorJuegos`: lectura del catalogo activo.
- `GET /detalleVideoJuego/{id}`: consultas concurrentes por identificador.
- `GET /consultarVideojuego?id=...`: carga de datos y relaciones.
- `GET /categorias` y los listados de estudios: lectura de catalogos auxiliares.
- `GET /altaVideojuego` y `POST /altaVideojuego`: carga del formulario y altas con imagen, con medicion separada del tiempo de escritura de archivos.
- `POST /videojuego/aceptarEditVideojuego`: modificaciones con imagen opcional.

Se mediran latencias, throughput, errores, conexiones a base de datos, memoria y tiempo de renderizado. En 5d, el catalogo activo y el detalle son los candidatos principales por ser rutas de lectura potencialmente muy frecuentes.

## 5. Pruebas de Estres

El objetivo es superar progresivamente la capacidad nominal y observar el primer punto de degradacion, la calidad de los errores y la recuperacion.

### 5c

- Aumentar gradualmente las peticiones a `/mascota/mis-mascotas` con usuarios que tengan muchos registros y verificar el costo de la consulta filtrada.
- Saturar `POST /login` con credenciales validas y no validas para observar el limite de consultas y sesiones.
- Enviar de forma concurrente altas de mascotas con archivos de imagen grandes, sin superar inicialmente los limites del entorno, y luego introducir archivos invalidos.
- Ejecutar modificaciones y bajas simultaneas sobre la misma mascota para comprobar consistencia del estado `baja` y ausencia de persistencia parcial.
- Generar muchos cambios de mascotas y revisar que la auditoria Envers no provoque errores ni perdida de revisiones.

### 5d

- Saturar `/inicio`, `/exploradorJuegos` y `/detalleVideoJuego/{id}` con un catalogo amplio para observar el costo de las consultas activas y la renderizacion.
- Enviar en paralelo consultas de detalle con ids existentes, inexistentes y dados de baja.
- Incrementar altas y modificaciones `multipart/form-data` con archivos proximos a 15 MB para presionar memoria, disco y tiempos de escritura.
- Enviar formularios con DTO/modelos incompletos y relaciones ausentes a alta y modificacion para observar la capacidad de rechazo controlado.
- Ejecutar bajas logicas y lecturas del mismo videojuego concurrentemente para comprobar que no se muestran estados inconsistentes.

El resultado esperado del estres no es mantener la misma latencia indefinidamente, sino alcanzar un rechazo controlado, sin corrupcion de datos, y recuperar el servicio al retirar la carga.

## 6. Patrones POM y BDD

### 6.1. Page Object Model para 5c

Se modelarian objetos de pagina basados en las vistas actuales:

- `LoginPage`: campos `email` y `clave`, boton de ingreso, mensaje de error y mensaje de logout.
- `RegistroPage`: nombre, apellido, mail, claves, zona, archivo y envio; tambien mensaje de error y pantalla de exito.
- `InicioMascotasPage`: acceso al listado y estado de sesion.
- `MisMascotasPage`: tabla de mascotas, boton de alta, enlaces de editar/eliminar y datos visibles de nombre, sexo y tipo.
- `MascotaFormPage`: id oculto, nombre, tipo, sexo, archivo y botones condicionados por `accion` (`Crear`, `Actualizar`, `Eliminar`, `Alta`).
- `PerfilPage`: campos del usuario, zona, fotografia y actualizacion.

Cada Page Object encapsularia selectores y acciones como iniciar sesion, crear mascota, editar mascota, solicitar baja y comprobar una fila. Las aserciones de negocio se mantendrian en los escenarios, no en los selectores.

### 6.2. Page Object Model para 5d

Se modelarian:

- `InicioPage` y `ExploradorJuegosPage`: enlaces de navegacion, grilla de tarjetas y acceso al detalle.
- `DetalleVideojuegoPage`: titulo, descripcion, precio, stock, imagen y enlace de retorno.
- `FormularioVideojuegoPage`: titulo, descripcion, imagen, precio, stock, fecha, categoria, estudio, modo visible y mensajes de validacion.
- `ConfirmacionBajaVideojuegoPage`: videojuego mostrado y envio de la baja.
- `CategoriasPage`, `FormularioCategoriaPage`, `EstudiosPage` y `FormularioEstudioPage`: listados, altas, modificaciones, consultas y bajas observables en sus vistas.
- `NavbarComponent`: componente reutilizable definido en `components/navbar.html`.

Los objetos deberian distinguir los modos `alta`, `modificar` y `consulta`, porque la plantilla `editVideojuego.html` cambia formulario, accion y editable segun `modo`.

### 6.3. BDD aplicado al 5c

Los escenarios se expresarian desde el comportamiento del usuario y sus reglas reales:

- **Dado** un usuario habilitado y una zona existente, **cuando** completa el registro con claves iguales y validas, **entonces** se crea el usuario y se muestra la pantalla de exito.
- **Dado** un usuario autenticado, **cuando** crea una mascota con nombre, sexo y tipo, **entonces** la mascota queda asociada al usuario y aparece en "Mis Mascotas".
- **Dado** un usuario que no es propietario, **cuando** intenta modificar o eliminar una mascota ajena, **entonces** la operacion es rechazada por permisos.
- **Dado** una mascota dada de baja, **cuando** se consulta el listado activo, **entonces** no aparece.
- **Dado** un usuario deshabilitado, **cuando** intenta iniciar sesion, **entonces** se muestra el error de usuario deshabilitado.

### 6.4. BDD aplicado al 5d

- **Dado** un videojuego con datos validos y una imagen legible menor de 15 MB, **cuando** se envia el alta, **entonces** se guarda activo y aparece en el catalogo.
- **Dado** un formulario con precio menor de 5, stock menor de 1 o fecha futura, **cuando** se intenta guardar, **entonces** el alta se rechaza y se muestran errores de validacion.
- **Dado** un videojuego activo, **cuando** se consulta por id, **entonces** se muestra su formulario en modo consulta o su detalle.
- **Dado** un videojuego activo, **cuando** se confirma la baja, **entonces** se alterna `activo` y deja de aparecer en las listas activas.
- **Dado** un titulo parcial, **cuando** se ejecuta la busqueda, **entonces** se devuelven las coincidencias activas correspondientes.
- **Dado** un formulario de alta con imagen vacia, no imagen o imagen de 15 MB o mas, **cuando** se envia, **entonces** la vista conserva el formulario y muestra el motivo del rechazo.

Los escenarios BDD deberian ejecutarse en conjunto con una verificacion de persistencia para evitar falsos positivos producidos por una vista que renderiza datos antiguos o incompletos.

---

## Resultado esperado del plan

La ejecucion debe proporcionar evidencia suficiente para afirmar, por separado para 5c y 5d, que las reglas de negocio identificadas funcionan, que los estados persistidos son coherentes con las vistas y que los limites de carga y estres son conocidos. Cualquier capacidad no observada en el codigo, especialmente la compra de videojuegos, debe registrarse como requisito pendiente y no como resultado de prueba.
