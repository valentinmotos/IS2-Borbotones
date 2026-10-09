# TP2 - Ejercicio 2g - Introducción Spring IA (Parte A)

Proyecto del video "MASTER CLASS de Introducción SPRING AI": Spring Boot 4.1.1, Java 25 y Spring AI 2.0.0 usando el starter de OpenAI apuntando a OpenRouter (modelos gratuitos).

- `IaController`: `GET /ia/preguntar?pregunta=...` devuelve la respuesta con status 200.
- `IaService`: construye el `ChatClient` con el `ChatClient.Builder` (toma la configuración de `application.properties`) y envía el prompt con una intro de sistema ("Sos un profesor especializado en programación...") más la pregunta del usuario.

## Configuración

Se necesitan tres variables: `OPENROUTER_API_KEY`, `OPENROUTER_BASE_URL` y `OPENROUTER_MODEL`. Se pueden cargar de dos formas:

- Como variables de entorno en la configuración de ejecución del IDE (como en el video).
- Copiando `apikey.properties.example` como `apikey.properties` en esta carpeta y completando la key. Ese archivo no se sube al repositorio.

Si el proveedor de internet bloquea `openrouter.ai`, se puede usar Gemini sin cambiar el código, ya que ofrece un endpoint compatible con OpenAI:

```properties
OPENROUTER_API_KEY=key de https://aistudio.google.com/apikey
OPENROUTER_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
OPENROUTER_MODEL=gemini-3.8-flash
```

## Ejecutar

```powershell
.\mvnw.ps1 spring-boot:run
```

Probar con Postman o el navegador:

`http://localhost:8080/ia/preguntar?pregunta=¿Qué es la programación orientada a objetos en Java?`
