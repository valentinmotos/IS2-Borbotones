# Ejercicio 5f: seguridad en la API REST

Copia del ejercicio 2e (biblioteca con préstamos, envíos automáticos, reportes PDF/Excel y
migración de proveedores) al que se le agrega seguridad con Spring Security y JWT.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta, iniciar en dos terminales:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Abrir `http://localhost:8080` e iniciar sesión. El servidor usa el puerto 8000.

## Usuarios

| Usuario   | Contraseña   | Rol     | Permisos                                    |
|-----------|--------------|---------|---------------------------------------------|
| `admin`   | `admin123`   | ADMIN   | Consultar, crear, editar, eliminar y migrar |
| `usuario` | `usuario123` | USUARIO | Solo consultar y descargar reportes         |

Se crean automáticamente al iniciar el servidor, con la contraseña encriptada con BCrypt.

## Cómo funciona la seguridad

```text
 NAVEGADOR            CLIENTE (8080)                     SERVIDOR (8000)
    |  usuario/clave      |                                     |
    |-------------------->|  POST /api/auth/login               |
    |                     |------------------------------------>| valida con BCrypt
    |                     |       { token JWT, usuario, rol }   | y firma el token
    |                     |<------------------------------------|
    |  cookie de sesión   | guarda el token en la sesión        |
    |<--------------------|                                     |
    |  GET /personas      |  GET /api/personas                  |
    |-------------------->|  Authorization: Bearer <token>      |
    |                     |------------------------------------>| JwtFilter valida firma,
    |                     |              200 / 401 / 403        | expiración y rol
    |                     |<------------------------------------|
```

### Servidor

- `POST /api/auth/login` es el único endpoint público. Recibe `{"username", "password"}` y
  devuelve un JWT firmado (HS384) con el usuario y su rol, válido por 60 minutos.
- `JwtFilter` lee el encabezado `Authorization: Bearer <token>` en cada petición y valida
  la firma y la expiración. La API es *stateless*: no guarda sesiones.
- Reglas de acceso:
  - `GET /api/**`: roles ADMIN y USUARIO.
  - `POST`, `PUT` y `DELETE /api/**`: solo ADMIN.
  - Sin token o con token inválido/vencido: `401 Unauthorized`. Sin permisos: `403 Forbidden`.
- La clave de firma y la duración del token se pueden cambiar con las variables de
  entorno `JWT_SECRET` (Base64, mínimo 256 bits) y `JWT_EXPIRACION_MINUTOS`.

### Cliente

- Pantalla de login. El token se guarda en la sesión del servidor del cliente (nunca en el
  navegador) y la cookie de sesión viaja solo por cookie, sin `jsessionid` en la URL.
- Un interceptor de `RestTemplate` agrega `Authorization: Bearer <token>` a cada llamada a la API.
- Si no hay sesión, cualquier pantalla redirige a `/login`.
- Si el servidor responde 401 (token vencido) se cierra la sesión y se vuelve al login;
  si responde 403 se muestra la página "Acceso denegado".
- El menú muestra el usuario conectado y el botón **Salir**.

## Probar con Postman

1. `POST http://localhost:8000/api/auth/login` con body JSON
   `{"username":"admin","password":"admin123"}` y copiar el `token`.
2. En cualquier otra petición, pestaña **Authorization** → **Bearer Token** y pegar el token.
3. Sin token se obtiene 401; con el token de `usuario` un `DELETE` devuelve 403.

## Tests

```powershell
.\mvnw.ps1 test
```

Incluye `SeguridadTests` en el servidor (401 sin token o con token falso, login incorrecto,
USUARIO no puede modificar, ADMIN sí) y en el cliente (redirección al login, envío del
Bearer token, token vencido y acceso denegado), además de los tests de reportes del 2e.
