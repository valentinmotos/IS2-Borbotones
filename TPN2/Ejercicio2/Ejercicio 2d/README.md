# Ejercicio 2d: biblioteca con archivos PDF

Extension del proyecto cliente-servidor de `TPN2/Ejercicio2/Ejercicio 2c`.
Conserva las funciones de libros, personas, domicilios, prestamos y envios automaticos.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta, en dos terminales:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Abrir `http://localhost:8080/libros`. El servidor utiliza el puerto 8000;
detener las aplicaciones del ejercicio c si estan usando esos puertos.

## Cargar y consultar un PDF

1. Seleccionar **Nuevo libro**, completar sus datos y elegir un archivo PDF opcional (hasta 20 MB).
2. Al guardar, el cliente envia los datos y el archivo a la API mediante `multipart/form-data`.
3. El servidor crea la carpeta `C:/biblioteca` cuando se carga el primer PDF y guarda el archivo
   con el formato `libro_nombrelibro_.pdf`. Por ejemplo: `El principito` se guarda como
   `C:/biblioteca/libro_el_principito_.pdf`. Los espacios y signos se convierten en guiones bajos;
   las letras se normalizan a minusculas sin acentos.
4. En la lista de libros o al editarlo, **Abrir PDF** abre el documento en una nueva pestana.
   El cliente obtiene el PDF del servidor por HTTP; funciona aunque esten en equipos distintos.

Los libros sin archivo muestran **Sin PDF**. Se puede agregar un PDF al editar un libro que
todavia no tenga uno. Editar los datos de un libro conserva el archivo y su nombre original.
No se reemplazan PDFs existentes: si el nombre generado ya existe se muestra un error.
Eliminar un libro tambien elimina su PDF. La API valida la extension y la firma `%PDF-`.
La asociacion se persiste en SQLite en el campo `pdfNombre`; JPA actualiza la tabla al iniciar.
El proceso del servidor debe tener permiso de escritura en `C:/biblioteca`.

Para usar otra carpeta durante pruebas, configurar `BIBLIOTECA_DIRECTORIO` en el servidor.
La ruta predeterminada para la entrega es `C:/biblioteca`.

## API

- `POST /api/libros`: acepta JSON sin archivo o multipart con parte `libro` (JSON) y parte `pdf` opcional.
- `PUT /api/libros/{id}`: acepta las mismas variantes y conserva el PDF asociado.
- `GET /api/libros/{id}/pdf`: devuelve `application/pdf` con `Content-Disposition: inline`.
- `GET /api/libros`: incluye el nombre del PDF asociado, si existe.

El navegador accede al PDF mediante `GET /libros/{id}/pdf` en el cliente.

## Verificacion

```powershell
.\mvnw.ps1 test
.\mvnw.ps1 package
```

Las pruebas del servidor usan una base SQLite y una carpeta temporal independientes;
verifican carga, contenido servido, edicion, eliminacion, archivos invalidos, nombres
duplicados y libros sin PDF. Las del cliente verifican el envio multipart, el enlace
en otra pestana, la respuesta PDF y los errores del formulario.
