# Ejercicio 1g: Catalogue Management con interfaz web

Basado en [CatalogueManagement de liyabonasaki](https://github.com/liyabonasaki/CatalogueManagement),
commit `21763b6054afbd96776b1aff02bfb3b75f2f61c4`.
La documentacion original se conserva en `README-original.md`.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

Si tenes Maven instalado, tambien podes ejecutar `mvn spring-boot:run`.
La primera ejecucion del wrapper descarga Maven y las dependencias necesarias.

Abrir **http://localhost:8082**. El front y la API se ejecutan juntos:
no requiere Node.js, instalar paquetes del front ni iniciar otro servidor.

Para ejecutar el JAR:

```powershell
.\mvnw.cmd package
java -jar target/CatalogueManagement-0.0.1-SNAPSHOT.jar
```

## Interfaz web

- Catalogo en tarjetas, con resumen de libros impresos y digitales.
- Busqueda por titulo o ISBN y filtro por formato.
- Alta y edicion de titulo, ISBN, fecha de publicacion, precio y formato.
- Eliminacion con confirmacion.
- Mensajes de carga, exito, error y catalogo vacio.
- Diseno adaptable a pantallas chicas, etiquetas accesibles y navegacion con teclado.

Formatos admitidos: tapa dura (`HARDCOVER`), tapa blanda (`SOFTCOVER`) y digital (`EBOOK`).
El catalogo comienza vacio: presionar **Agregar libro** para cargar el primero.

## Datos y configuracion

Los libros se conservan al reiniciar en H2, en `data/catalogue.mv.db`.
Ejecutar siempre desde esta carpeta para utilizar la misma base de datos.
No se requiere instalar una base de datos externa.

- `SERVER_PORT`: cambia el puerto predeterminado 8082.
- `CATALOGUE_DB_URL`: cambia la conexion; por ejemplo `jdbc:h2:mem:catalogue` para una base temporal.
- `OTEL_SDK_DISABLED`: `true` por defecto para ejecutar localmente sin un colector de telemetria.

## API REST y GraphQL

Se conservan los endpoints del repositorio original:

| Metodo | URL | Operacion |
| --- | --- | --- |
| GET | `/api/books/getAllBooks` | Listar |
| GET | `/api/books/{id}` | Consultar un libro |
| POST | `/api/books/addBook` | Crear |
| PUT | `/api/books/updateBook/{id}` | Editar |
| DELETE | `/api/books/deleteBook/{id}` | Eliminar |

Swagger: http://localhost:8082/swagger-ui/index.html
GraphQL: `POST /graphql`, con el esquema en `src/main/resources/graphql/schema.graphqls`.
Se completo `getBookById` y la actualizacion parcial de GraphQL conserva los campos omitidos.
La API rechaza datos incompletos o precios negativos con HTTP 400 y devuelve 404 para libros inexistentes.

## Verificacion

```powershell
.\mvnw.cmd package
node --test src/test/js/frontend.test.cjs
```

Las 13 pruebas Java cubren los tests originales y la integracion HTTP con H2:
recursos del front, alta, listado, consulta, edicion, eliminacion, validacion y GraphQL.
Las 3 pruebas JavaScript ejecutan el script real en un DOM aislado y verifican
los eventos CRUD, la busqueda, los filtros, el tratamiento de titulos como texto y los errores.
Node.js solo es necesario para ejecutar estas pruebas, no para usar el sistema.
La revision visual en un navegador no se pudo realizar en la sesion de desarrollo
porque no habia navegadores conectados.

## Cambios sobre el ejemplo original

Se agrego el front HTML/CSS/JavaScript, persistencia local, validaciones, consulta individual,
correcciones de GraphQL y pruebas de integracion. Lombok se actualizo a 1.18.42 y se configuro
su procesador para compilar tambien con el JDK instalado. Se agrego la configuracion faltante
del Maven Wrapper. El front usa recursos locales y no depende de servicios o librerias externas.
