# Ejercicio 2d: exportacion de reportes PDF y Excel

Extension de `TPN2/Ejercicio2/Ejercicio 2c`, con el mismo cliente web, API REST,
SQLite, prestamos y envios automaticos.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta, en dos terminales:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Abrir `http://localhost:8080`. El servidor usa el puerto 8000.
Detener otras aplicaciones que esten usando esos puertos.

## Descargar reportes

- En **Personas**, seleccionar **Descargar personas con alquileres (PDF)**.
- En **Libros**, seleccionar **Descargar libros disponibles (Excel)**.

El cliente solicita los archivos a la API y los entrega como descargas al navegador,
con nombre de archivo y tipo de contenido adecuados. Los reportes se generan en memoria
con los datos actuales: no se escriben en una carpeta del servidor.

### Personas con alquileres

PDF generado con **iText 9.3.0**, con ID, apellido, nombre, DNI y email.
Incluye a todas las personas que tienen al menos un prestamo registrado, tanto activo
como devuelto. Cada persona aparece una sola vez, ordenada por apellido y nombre.
Las personas sin prestamos no se incluyen. Un reporte vacio informa que no hay alquileres.
La tabla repite encabezados y continua en otras paginas cuando es necesario.

### Libros disponibles

Excel `.xlsx` generado con **Apache POI 5.4.1**, con ID, titulo, anio, genero, paginas y autor.
Un libro esta disponible si no tiene prestamos pendientes de devolucion:
`devuelto=false` o un estado sin informar se consideran pendientes, incluso si la fecha
limite ya vencio. Un prestamo devuelto no bloquea la disponibilidad. Si hay varios
prestamos para un libro, basta uno pendiente para excluirlo.

La hoja tiene encabezados, filtro, fila superior inmovilizada y columnas ajustadas.
ID, anio y paginas se exportan como numeros; los titulos y autores se exportan como texto.
Si no hay libros disponibles, se genera una hoja valida con los encabezados.

## Endpoints

| Capa | PDF | Excel |
| --- | --- | --- |
| Cliente | `/reportes/personas-alquileres.pdf` | `/reportes/libros-disponibles.xlsx` |
| API | `/api/reportes/personas-alquileres.pdf` | `/api/reportes/libros-disponibles.xlsx` |

Todos son endpoints GET. El servidor devuelve `Content-Disposition: attachment`.

## Compilar y verificar

```powershell
.\mvnw.ps1 package
```

Las pruebas usan una base SQLite temporal y comprueban el contenido de los archivos
con iText y Apache POI. Cubren personas sin prestamos, duplicados, prestamos devueltos,
pendientes vencidos, estados nulos, reportes vacios y PDFs de varias paginas.
Las pruebas del cliente comprueban los botones y que las descargas mantengan los bytes,
el nombre y el tipo de archivo recibidos del servidor.

## Documentacion de las bibliotecas

- [Instalacion de iText para Java](https://kb.itextpdf.com/itext/installing-itext-for-java)
- [XSSFWorkbook de Apache POI](https://poi.apache.org/apidocs/dev/org/apache/poi/xssf/usermodel/XSSFWorkbook.html)
