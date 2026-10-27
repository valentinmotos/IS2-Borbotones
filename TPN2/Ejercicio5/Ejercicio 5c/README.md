# JWT Parte A - Spring Boot

Implementacion del ejercicio de seguridad JWT con Spring Boot, Maven, MVC, JPA y Lombok.

## Arquitectura

- `controller`: recibe requests HTTP y delega al servicio.
- `service`: contiene la logica de negocio. Puede comunicarse con otros servicios.
- `dao`: acceso a datos con Spring Data JPA.
- `entity`: entidades JPA con getters y setters generados por Lombok.
- `security`: configuracion de Spring Security y filtro JWT.

## Usuario inicial

Al iniciar la aplicacion se crea este usuario:

```txt
username: admin
password: 123456
```

## Endpoints

Endpoint publico:

```http
GET http://localhost:8080/api/demo/publico
```

Login:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

Endpoint protegido:

```http
GET http://localhost:8080/api/demo/privado
Authorization: Bearer <token>
```

## Ejecutar

```bash
mvn spring-boot:run
```

En este equipo no estaba disponible el comando `mvn` en el PATH, por eso la compilacion local queda pendiente.
