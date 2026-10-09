# Ejercicio 1d: libros con archivos PDF

Extension de `TPN2/Ejercicio1/Ejercicio 1c`. Incluye los proyectos completos
`cliente` y `servidor`, fuentes Java, plantillas, configuracion, Maven Wrapper y pruebas.

## Consigna

Al crear un libro se puede cargar su PDF en el disco C del servidor, en
`C:/biblioteca/libro_nombrelibro_.pdf`. Al consultar el libro se puede abrir el
PDF en una solapa distinta. Se refactoriza el codigo y el diagrama de clases.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta, en dos terminales:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Abrir `http://localhost:8080/libros`. El servidor usa el puerto 8000.
Detener otras aplicaciones que esten usando esos puertos.

## Cargar y abrir un PDF

1. Seleccionar **Nuevo libro**, completar los datos y elegir un PDF opcional, de hasta 20 MB.
2. Guardar. El cliente envia los datos y el archivo al servidor mediante multipart.
3. El servidor crea `C:/biblioteca` cuando se carga el primer PDF. Por ejemplo, el titulo
   `El principito` genera `C:/biblioteca/libro_el_principito_.pdf`.
4. En la lista de libros o al editarlo, seleccionar **Abrir PDF**. Se abre en una pestana nueva.

Los espacios y signos del titulo se convierten en guiones bajos y se normalizan las letras
sin acentos y en minusculas. La API valida la extension y la firma del archivo.
No se sobrescriben archivos de otro libro: un nombre duplicado informa un error.
Los libros sin archivo muestran **Sin PDF**. Se puede adjuntar un PDF al editar un libro
que todavia no tenga uno. Editar sus datos conserva el PDF y su nombre original.
Eliminar el libro elimina tambien su archivo.

El nombre del PDF se persiste en SQLite en `Libro.pdfNombre`. Los datos se conservan
al reiniciar, en `servidor.sqlite`. El contenido PDF se obtiene por HTTP: el navegador
no necesita acceso al disco C del servidor.

El proceso del servidor debe poder escribir en `C:/biblioteca`.
`BIBLIOTECA_DIRECTORIO` permite configurar otra carpeta durante pruebas;
la ruta predeterminada de la entrega es `C:/biblioteca`.

## Refactorizacion y diagrama

[Diagrama de clases actualizado](docs/diagrama-clases.md), renderizable directamente en GitHub.

- `Libro` y `LibroDTO` incorporan `pdfNombre`.
- `PdfStorageService` concentra las operaciones sobre el archivo.
- Los controladores delegan en sus servicios y los DAO del cliente envian multipart.
- La API admite creacion y edicion JSON o multipart y consulta del PDF por ID.
- Las respuestas PDF usan `Content-Disposition: inline` y el enlace del cliente abre otra pestana.

## Compilar y verificar

```powershell
.\mvnw.ps1 package
```

Las pruebas del servidor usan una SQLite y una carpeta temporal independientes.
Verifican carga, nombre seguro, lectura del contenido, edicion, eliminacion,
archivos invalidos, duplicados y libros sin PDF. Las pruebas del cliente verifican
multipart, el enlace en una pestana nueva, la respuesta del PDF y los errores del formulario.

Los directorios `target/` y logs son archivos generados y quedan excluidos de Git.
El codigo fuente, los POM, el Wrapper y el diagrama si se incluyen en el repositorio.
