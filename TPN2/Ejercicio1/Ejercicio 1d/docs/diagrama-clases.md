# Diagrama de clases: Ejercicio 1d

El modelo del ejercicio 1c incorpora `Libro.pdfNombre` para persistir el nombre del PDF.
El archivo se guarda en el sistema de archivos del servidor. `PdfStorageService` concentra
la validacion del archivo, la generacion del nombre y su almacenamiento y lectura.

## Modelo del servidor

```mermaid
classDiagram
    class Libro {
        -Long id
        -String titulo
        -Integer fecha
        -String genero
        -Integer paginas
        -String autor
        -String pdfNombre
        -Persona persona
    }
    class Persona {
        -Long id
        -String nombre
        -String apellido
        -Integer dni
        -Domicilio domicilio
    }
    class Domicilio {
        -Long id
        -String calle
        -Integer numero
        -Localidad localidad
    }
    class Localidad {
        -Long id
        -String denominacion
    }
    class Autor {
        -Long id
        -String nombre
        -String apellido
        -String biografia
    }
    Libro "0..*" --> "0..1" Persona : persona
    Persona "0..1" --> "0..1" Domicilio : domicilio
    Domicilio "0..*" --> "0..1" Localidad : localidad
```

`Autor` se conserva como entidad del punto c; el libro mantiene su autor en un campo
de texto, por lo que no existe una asociacion JPA entre ambas clases.

## Clases que intervienen en la carga y consulta del PDF

Los sufijos Cliente y Servidor distinguen clases con el mismo nombre en aplicaciones
diferentes; en el codigo ambas se llaman `LibroController` o `LibroService`.

```mermaid
classDiagram
    direction LR
    class LibroControllerCliente {
        +listar(Model) String
        +nuevo(Model) String
        +editar(Long, Model) String
        +guardar(LibroDTO, MultipartFile, Model) String
        +abrirPdf(Long) ResponseEntity
        +eliminar(Long) String
    }
    class LibroServiceCliente {
        +listar() List
        +buscar(Long) LibroDTO
        +nuevo() LibroDTO
        +guardar(LibroDTO, MultipartFile) void
        +abrirPdf(Long) ResponseEntity
    }
    class LibroDAOCliente {
        -RestTemplate restTemplate
        -String url
        +guardar(LibroDTO, MultipartFile) void
        +abrirPdf(Long) ResponseEntity
    }
    class LibroDTO {
        -Long id
        -String titulo
        -Integer fecha
        -String genero
        -Integer paginas
        -String autor
        -String pdfNombre
        -PersonaDTO persona
    }
    class LibroControllerServidor {
        +crearConPdf(Libro, MultipartFile) Libro
        +actualizarConPdf(Long, Libro, MultipartFile) Libro
        +abrirPdf(Long) ResponseEntity
    }
    class LibroServiceServidor {
        +guardar(Libro) Libro
        +guardar(Libro, MultipartFile) Libro
        +buscar(Long) Libro
        +abrirPdf(Long) Resource
        +eliminar(Long) void
    }
    class PdfStorageService {
        -Path directorio
        +guardar(String, MultipartFile) String
        +abrir(String) Resource
        +eliminar(String) void
        -resolver(String) Path
    }
    class LibroDAOServidor {
        <<interface>>
    }
    class JpaRepository {
        <<interface>>
    }
    class PersonaServiceServidor
    class Libro
    LibroControllerCliente --> LibroServiceCliente
    LibroServiceCliente --> LibroDAOCliente
    LibroDAOCliente ..> LibroDTO
    LibroDAOCliente ..> LibroControllerServidor : HTTP multipart / PDF
    LibroControllerServidor --> LibroServiceServidor
    LibroServiceServidor --> PdfStorageService
    LibroServiceServidor --> LibroDAOServidor
    LibroServiceServidor --> PersonaServiceServidor
    LibroServiceServidor ..> Libro
    LibroDAOServidor --|> JpaRepository
```

La carga envia una parte JSON `libro` y una parte opcional `pdf`. El servicio del servidor
guarda el archivo y su nombre en SQLite. Al consultar el libro, el enlace `target="_blank"`
abre `/libros/{id}/pdf` en una pestana nueva; el cliente obtiene el contenido de
`/api/libros/{id}/pdf`, cuya respuesta usa `application/pdf` y `Content-Disposition: inline`.

`ApiExceptionHandler` entrega errores de validacion desde la API y `UploadExceptionHandler`
informa al usuario cuando el archivo supera el limite configurado.
