# Biblioteca segura

Aplicacion web Spring Boot para implementar el modelo de Autor, Editorial, Imagen, Libro, Prestamo y Usuario. Usa JPA/H2, Thymeleaf y Spring Security.

## Puesta en marcha

Desde esta carpeta, con Java 17 y Maven instalados:

```powershell
mvn spring-boot:run
```

Abrir `http://localhost:8080`.

Usuarios de demostracion:

- Administrador: `admin@biblioteca.local` / `admin123`
- Lector: `lector@biblioteca.local` / `lector123`

El administrador puede crear autores, editoriales y libros. Los lectores autenticados pueden solicitar ejemplares y devolver sus prestamos. Las contrasenas se almacenan con BCrypt y los formularios quedan protegidos por CSRF mediante la configuracion por defecto de Spring Security.