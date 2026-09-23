# Template visual del Integrador

El frontend usa el template **CozaStore** distribuido por ThemeWagon y creado por Colorlib.
La atribucion original se conserva en el pie de cada pagina, de acuerdo con su licencia CC BY 3.0.
El template fue migrado desde Bootstrap 4 beta y actualmente utiliza **Bootstrap 5.3.8**.

## Organizacion

- `src/main/resources/templates/`: vistas Thymeleaf adaptadas.
- `src/main/resources/static/cozastore/`: CSS, JavaScript, fuentes e imagenes del template.

Bootstrap se sirve localmente desde `static/cozastore/vendor/bootstrap/`. Las vistas cargan
`bootstrap.bundle.min.js`, que ya incluye Popper; no se debe volver a agregar `popper.js`
ni utilizar atributos antiguos como `data-toggle` o `data-target`.

Las vistas deben ser devueltas por un `@Controller`; no se deben enlazar archivos `.html`
directamente. Los recursos visuales se referencian desde `/cozastore/...`.

## Vistas base disponibles

| Ruta MVC | Vista Thymeleaf | Uso previsto |
| --- | --- | --- |
| `/` o `/inicio` | `inicio.html` | pagina principal |
| `/productos` | `productos.html` | catalogo |
| `/producto-detalle` | `producto-detalle.html` | detalle de producto |
| `/carrito` | `carrito.html` | carrito de compras |
| `/contacto` | `contacto.html` | contacto |
| `/demo/inicio-2` | `inicio-02.html` | variante visual de referencia |
| `/demo/inicio-3` | `inicio-03.html` | variante visual de referencia |

Estas paginas contienen por ahora los datos demostrativos originales. Al implementar cada
modulo se deben reemplazar por atributos y expresiones Thymeleaf, conservando el lenguaje
visual del template y las rutas MVC del proyecto.
