# Ejercicio 3e - Carrito de compras con Template Method

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
- API: `http://localhost:8081/api/articulos`, `/api/proveedores`, `/api/imagenes`, `/api/usuarios`, `/api/carritos`
- Contenido de una imagen: `http://localhost:8081/api/imagenes/{id}/contenido`

El servidor carga al iniciar un usuario de prueba, dos proveedores y tres articulos con su imagen.

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
