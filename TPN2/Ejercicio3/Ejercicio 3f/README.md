# Ejercicio 3f - Proveedores en OpenStreetMap

Es el sistema del ejercicio 3e (carrito de compras con Template Method) con la direccion de los proveedores, que guarda la latitud y longitud de cada uno, y un mapa de OpenStreetMap con la ubicacion de todos los proveedores.

Proyecto Spring Boot con dos aplicaciones locales:

- `servidor`: API REST en `http://localhost:8081`, JPA y SQLite3.
- `cliente`: sitio HTML en `http://localhost:8080`, Thymeleaf, Bootstrap 5 y `RestTemplate`.

## Requisitos

- Java 17 o superior.
- No hace falta Maven global: se incluye `mvnw.ps1`.

## Compilar

```powershell
.\mvnw.ps1 clean package -DskipTests
```

## Ejecutar

En una terminal:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

En otra terminal:

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Luego abrir:

- Cliente HTML: `http://localhost:8080`
- Mapa y ABM de proveedores: `http://localhost:8080/proveedores`
- API: `http://localhost:8081/api/articulos`, `/api/proveedores`, `/api/imagenes`, `/api/usuarios`, `/api/carritos`
- Contenido de una imagen: `http://localhost:8081/api/imagenes/{id}/contenido`

El servidor carga al iniciar un usuario de prueba, dos proveedores (con direccion en Mendoza) y tres articulos con su imagen.

## Diagrama de clases

Entidades del diagrama: `Carrito`, `Detalle`, `Usuario`, `Articulo`, `Imagen` y `Proveedor`, todas con `id` String (UUID) y `eliminado` para la baja logica.

- `Carrito` -> `Usuario` (`@ManyToOne`).
- `Carrito` <>- `Detalle` (composicion: `@OneToMany` con `cascade` y `orphanRemoval`).
- `Detalle` -> `Articulo`, `Articulo` -> `Imagen` y `Articulo` -> `Proveedor` (`@ManyToOne`).

Ademas del diagrama se agregaron `Carrito.finalizado`, `Detalle.cantidad` y `Detalle.precioUnitario`, necesarios para el funcionamiento del carrito.

## Template Method

- `service/BaseRestService`: los metodos plantilla `crear`, `modificar` y `eliminar` son `final` y siempre siguen los mismos pasos (`preAlta`/`validar`/`save`/`postAlta`, etc.). Las subclases redefinen los pasos `validar`, `actualizar` y `marcarEliminada`.
- `template/CarritoTemplate`: los metodos plantilla `agregarArticulo`, `editarDetalle` y `finalizarCompra` definen el algoritmo de cada caso de uso del carrito (validar cantidad, obtener carrito activo, validar que sea editable, modificar detalles, recalcular total y guardar).
- `controller/BaseRestController`: endpoints REST comunes (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`) con los pasos `modificarRecurso` y `eliminarRecurso` redefinibles.

## Template HTML/CSS

El cliente usa como base el template gratuito [Shop Homepage de Start Bootstrap](https://startbootstrap.com/template/shop-homepage) (licencia MIT), con Bootstrap 5 y Bootstrap Icons por CDN.

## OpenStreetMap

### Que es

[OpenStreetMap](https://www.openstreetmap.org) (OSM) es un mapa del mundo libre y colaborativo, armado por voluntarios. Sus datos se publican con licencia abierta (ODbL), asi que se pueden usar gratis siempre que se muestre la atribucion "© OpenStreetMap contributors". A diferencia de Google Maps, no hace falta crear una cuenta ni una API key.

### Piezas que se usan

- **Tiles de OSM**: el mapa se dibuja con imagenes cuadradas (tiles) que se piden a `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, donde `z` es el zoom y `x`, `y` la posicion.
- **[Leaflet](https://leafletjs.com) 1.9.4**: libreria JavaScript liviana para mostrar mapas interactivos. Se carga por CDN y se encarga de pedir los tiles, el zoom, los marcadores (`L.marker`) y los popups.
- **[Nominatim](https://nominatim.org)**: servicio de geocodificacion de OSM. Convierte una direccion en latitud y longitud (`/search?format=json&q=...`). Su politica de uso pide no hacer mas de una consulta por segundo, por eso solo se llama cuando el usuario aprieta "Buscar direccion en el mapa".

### Como se aplico

- Servidor: se agrego la clase `entity/Direccion` (`@Embeddable`) con `calle`, `numero`, `ciudad`, `latitud` y `longitud`, embebida en `Proveedor`. `ProveedorService` redefine el paso `validar` del Template Method para exigir latitud y longitud y controlar sus rangos (-90 a 90 y -180 a 180).
- Cliente: la pagina `/proveedores` tiene el ABM de proveedores y un mapa de Leaflet con un marcador por cada proveedor (el popup muestra nombre y direccion). Al cargar o editar un proveedor, la latitud y longitud se completan haciendo clic en el mapa o buscando la direccion con Nominatim.
