# TP2 - Ejercicio 2g - Introducción Spring IA (Parte B)

Proyecto del video "Spring AI + Gemini: Tu Primera App de AI con Java": Spring Boot 3.5.14, Java 21, Spring AI 1.1.8 con el starter de Google GenAI (API key gratuita de Google AI Studio), Lombok y WebFlux.

- `ChatClientConfig`: crea el bean `ChatClient` a partir del `ChatClient.Builder` y le da personalidad con `defaultSystem` (solo responde sobre tecnología y programación).
- `ChatController` (`/api/v1/chat`):
  - `GET /api/v1/chat?message=...`: devuelve solo el texto de la respuesta.
  - `GET /api/v1/chat/full?message=...`: devuelve el record `ChatFullResponse` con el contenido y los tokens de la pregunta, de la respuesta y el total.
  - `GET /api/v1/chat/stream?message=...`: devuelve un `Flux<String>` como Server-Sent Events, la respuesta llega por partes.

## Configuración

1. Obtener una API key en `https://aistudio.google.com/apikey`.
2. Copiar `apikey.properties.example` como `apikey.properties` en esta carpeta y completar la key (o definir `GEMINI_API_KEY` y `GEMINI_MODEL` como variables de entorno). Ese archivo no se sube al repositorio.

El modelo del video (`gemini-2.5-flash`) ya no está disponible para cuentas nuevas, por eso se usa `gemini-3.8-flash`. La temperatura es 0.7.

## Ejecutar

```powershell
.\mvnw.ps1 spring-boot:run
```

Probar con Postman:

- `http://localhost:8080/api/v1/chat?message=¿Qué es Java en programación?`
- `http://localhost:8080/api/v1/chat/full?message=¿Qué es Java en programación?`
- `http://localhost:8080/api/v1/chat/stream?message=¿Qué es Java en programación?`
- `http://localhost:8080/api/v1/chat?message=¿Quién es Messi?` (responde que solo habla de tecnología y programación)
