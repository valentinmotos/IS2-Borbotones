# TP N.º 2 · Ejercicio 4g · Historia clínica

Implementación del diagrama de la consigna con API REST Java, persistencia H2 y dos versiones de frontend. El código entregado incluye la refactorización con **Factory Method (creacional), Strategy (comportamiento) y Facade (estructural)** y conserva **Template Method** como algoritmo común de ABM.

| Versión | Frontend | Backend | Dirección |
|---|---|---|---|
| Java + Java | Spring MVC + Thymeleaf, módulo `frontend-java` | Spring Boot, módulo `backend` | http://localhost:8081 |
| React + Java | React + Vite, carpeta `frontend-react` | El mismo módulo `backend` | http://localhost:5173 |

Ambos frontends consumen la API por HTTP. El frontend Java no accede a JPA ni comparte entidades con el backend. Se puede ejecutar una versión o las dos a la vez; comparten los datos de la API. Compartir el backend evita mantener dos copias divergentes de las mismas reglas.

## Requisitos y ejecución

- JDK 17 a 25 y Maven 3.6.3 o superior. Compilación con `release 17` y Spring Boot 3.5.16. También se verificó en el JDK 26 disponible en este equipo, fuera del rango oficial de esa línea de Spring Boot.
- Node.js 22.12 o superior para React (verificado con Node 24).
- La primera compilación requiere Internet para Maven/npm. El template CSS ya está incluido localmente: no requiere CDN al navegar.

Desde **esta carpeta** (`TPN2/Ejercicio4/Ejercicio 4g`), compilar y probar:

```powershell
mvn clean verify
```

Terminal 1, backend de ambas versiones:

```powershell
cd backend
java -jar target/backend-1.0.0.jar
```

Terminal 2, versión Java + Java:

```powershell
cd frontend-java
java -jar target/frontend-java-1.0.0.jar
```

Abrir http://localhost:8081. Como alternativa, desde cada módulo: `mvn spring-boot:run`.

Terminal 3, versión React + Java:

```powershell
cd frontend-react
npm.cmd ci
npm.cmd run dev
```

Abrir http://localhost:5173. En Linux/macOS usar `npm` en lugar de `npm.cmd`.

**Credenciales de demostración:** `demo` / `Consultorio123!`. La primera ejecución con una base vacía agrega un usuario, un paciente, un médico, una historia y una atención con datos ficticios. `clinica.demo=false` desactiva esa carga; en una base vacía se deberá aprovisionar un usuario antes de poder autenticarse.

Si 8080 está ocupado, no es necesario detener otros proyectos:

```powershell
# Backend, desde backend/
java -jar target/backend-1.0.0.jar --server.port=8090
# Frontend Java, desde frontend-java/
java -jar target/frontend-java-1.0.0.jar --server.port=8091 --clinica.api=http://localhost:8090/api
# React, antes de npm.cmd run dev
$env:CLINICA_API = 'http://localhost:8090'
```

`CLINICA_API` en el frontend Java también permite configurar la URL, **incluyendo `/api`**; en Vite se configura la dirección raíz del backend, **sin `/api`**. El proxy Vite evita necesitar CORS en desarrollo.

## Funcionalidad

- Alta, consulta, edición y baja lógica de pacientes, médicos, usuarios e historias clínicas.
- Expediente con datos del paciente y responsable, total de atenciones y evolución ordenada por fecha descendente.
- Alta, edición, consulta y baja de atenciones, con médico y fecha obligatorios. Se rechazan fechas futuras.
- Política de consulta general (mínimo 10 caracteres) o control (mínimo 20), seleccionada en ambos frontends y validada mediante Strategy en el servidor.
- Subida, consulta, reemplazo y baja de fotos PNG/JPEG de hasta 2 MB; verificación del contenido y límite de 20 millones de píxeles.
- Reserva de documentos y nombres de usuario incluso después de una baja. Mensajes de error en las interfaces.
- Eliminación de una historia y sus detalles en una transacción. No se permite eliminar pacientes, usuarios o médicos con referencias clínicas activas.
- HTTP Basic con usuarios persistidos, contraseñas BCrypt y respuestas sin contraseñas ni bytes de foto en el JSON. El frontend Java mantiene credenciales en sesión de servidor y protege formularios con token CSRF; React las mantiene únicamente en memoria y las envía mediante encabezado explícito.

## Modelo y decisiones

Se mantienen las seis clases: `FotoPaciente`, `Paciente`, `HistoriaClinica`, `Usuario`, `DetalleHistoriaClinica` y `Medico`. Todas conservan `id:String` y `eliminado:boolean`. Los IDs son UUID en texto. `Date` se representa con `LocalDate`, ya que la consigna requiere fecha de atención, sin hora. `contenido:[]byte` se representa con `byte[]` y `@Lob`.

La notación `*...1` del dibujo se interpreta como muchos a uno: muchas historias pueden referenciar un paciente o usuario, muchos detalles un médico y muchos pacientes una foto. La interfaz asigna una foto nueva por paciente para evitar compartir accidentalmente imágenes. Una historia compone múltiples detalles: ninguna atención se crea sin una historia existente y la baja del agregado se propaga a sus detalles. No se agrega una restricción de una sola historia por paciente, porque no figura en el diagrama.

`tipo` es un parámetro de la operación de atención para elegir la política de validación; no agrega un atributo persistido al UML. Al editar se selecciona nuevamente la política, con consulta general como valor inicial. Los detalles se consultan únicamente a través de su historia y se verifica su pertenencia.

H2 guarda los datos en `backend/data/clinica.mv.db` si se inicia desde `backend/`. La ruta es relativa al directorio de ejecución. Los tests usan una base en memoria y revierten los cambios. Los archivos de ejecución y dependencias están excluidos por el `.gitignore` local.

## Template gratuito y fuentes

Las dos versiones adaptan **[Flatly de Bootswatch](https://bootswatch.com/flatly/)**, basado en Bootstrap 5.3.8, con navegación lateral y panel clínico propios. CSS distribuido localmente en `frontend-java/src/main/resources/static/vendor` y `frontend-react/public/vendor`. La [licencia MIT original](https://github.com/thomaspark/bootswatch/blob/v5.3.8/LICENSE) se incluye en ambas carpetas `vendor/BOOTSWATCH-LICENSE.txt`. No se necesita una cuenta ni un template pago.

Compatibilidad de Java: [requisitos oficiales de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html). El proyecto es una demostración académica con un único perfil de acceso de consultorio; un despliegue con datos clínicos reales requiere HTTPS, autorización por profesional, auditoría de accesos y políticas de respaldo.

## Documentación y verificaciones

- [Fundamentación teórica y refactorización](docs/PATRONES.md): conceptos, participantes, motivos, antes/después y diagramas.
- [Contrato REST y ejemplos](docs/API.md).
- [Recorrido de demostración y pruebas](docs/PRUEBAS.md).

```powershell
# Desde esta carpeta
mvn verify
# Desde frontend-react
npm.cmd run build
npm.cmd audit
```

El build React queda en `frontend-react/dist`. `npm.cmd run preview` lo sirve en http://localhost:4173 y conserva el proxy a la API para una revisión local. Para publicarlo se debe configurar un proxy inverso `/api` al backend.
