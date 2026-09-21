# Ejercicio 6e - Seguridad aplicada a Videojuegos

Este proyecto parte del Ejercicio 4d y aplica los conceptos usados en el Ejercicio 6d y en la guia de seguridad del TP.

## Controles implementados

- Spring Security protege todas las operaciones de alta, modificacion, baja y administracion. El catalogo, la busqueda, el detalle, el login y el registro son publicos.
- Las contrasenas se almacenan con BCrypt (factor 12). Nunca se guarda ni se registra la contrasena en texto plano.
- Los formularios `POST` conservan la proteccion CSRF de Spring Security. Login, registro, logout y bajas usan `POST`.
- La sesion migra su identificador al autenticar, admite una sesion concurrente por usuario y elimina `JSESSIONID` al cerrar sesion.
- Se agrega una politica CSP y se mantienen las cabeceras seguras predeterminadas de Spring Security.
- HTTPS esta habilitado y parametrizado. Las instrucciones para el certificado local estan en `HTTPS.md`.
- Las credenciales de MySQL y del certificado se reciben desde variables de entorno; no se incluyen secretos reales en el repositorio.
- Las entidades y formularios tienen validacion del lado del servidor. Los servicios vuelven a validar datos criticos y normalizan nombre/email.
- Las actualizaciones copian solamente los campos permitidos sobre la entidad existente, por lo que un campo oculto manipulado no puede reactivar registros ni reemplazar propiedades protegidas.
- Las imagenes se renombran con UUID, se guardan en una ruta normalizada y solo se aceptan JPG/PNG de hasta 5 MB que puedan decodificarse como imagen. Tambien se limitan sus dimensiones.
- Los errores inesperados no exponen trazas, SQL, rutas locales ni mensajes internos al navegador.
- Las consultas usan repositorios Spring Data con parametros, sin concatenar entrada del usuario en SQL.

## Ejecucion local

1. Crear la base de datos `videojuegos` en MySQL.
2. Generar el certificado siguiendo `HTTPS.md`.
3. Definir, como minimo, `DB_USERNAME`, `DB_PASSWORD` y `SSL_KEY_STORE_PASSWORD`.
4. Ejecutar `mvn -s maven-settings.xml spring-boot:run`.
5. Abrir `https://localhost:8443`, registrar una cuenta e ingresar. Las nuevas cuentas reciben el rol `USUARIO`.

Variables opcionales: `DB_URL`, `SSL_KEY_STORE`, `SSL_KEY_ALIAS`, `SERVER_PORT` y `VIDEOJUEGOS_IMAGE_DIR`.

## Pruebas

```powershell
mvn -s maven-settings.xml test
```

Las pruebas usan H2 y SSL deshabilitado solamente en el entorno de test. Verifican acceso publico, autenticacion para el CRUD, CSRF, CSP y cifrado BCrypt.
