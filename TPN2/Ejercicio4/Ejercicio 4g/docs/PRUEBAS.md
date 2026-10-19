# Demostración y verificaciones

## Pruebas automatizadas

Desde la raíz del ejercicio: `mvn clean verify`.

| Clase | Casos | Qué demuestra |
|---|---:|---|
| `ClinicaIntegrationTest` | 9 | CRUD paciente, validaciones, contraseñas ocultas y BCrypt, Strategy, Facade, fotos, referencias, pertenencia y baja lógica del agregado |
| `HttpIntegrationTest` | 1 | Servidor HTTP real en puerto aleatorio: autenticación sin transacción de test, carga demo, commit y lectura entre peticiones, baja y consulta del expediente |
| `FrontendIntegrationTest` | 4 | Renderizado Thymeleaf de todos los catálogos/ediciones y del expediente; protección de sesión/CSRF y login con consumo HTTP |

Las nueve pruebas de servicio/API usan MockMvc y base H2 en memoria con rollback. La prueba HTTP real tiene una base en memoria independiente, sin transacción de test, para detectar problemas que un contexto transaccional compartido podría ocultar. Las cuatro del frontend usan un servidor HTTP de respuestas controladas y comprueban el HTML generado por Thymeleaf, sin depender del puerto 8080 ni de la base persistida.

Desde `frontend-react`:

```powershell
npm.cmd ci
npm.cmd run build
npm.cmd audit
```

El build verifica la transformación JSX, módulos y empaquetado de producción. No equivale a una prueba de interacción del navegador. Se comprobó Vite 7.3.7, con cero vulnerabilidades reportadas al instalar las dependencias. No se ejecutó una comprobación visual en navegador porque la sesión de herramientas no dispone de uno conectado.

Los reportes JUnit se generan en `backend/target/surefire-reports` y `frontend-java/target/surefire-reports`. Java 26 puede emitir avisos de carga dinámica del agente de Mockito; no son fallos de las pruebas. Los requisitos oficiales de esta línea de Spring Boot se indican en el README.

Además se ejecutó `scripts/smoke-http.mjs` contra las dos aplicaciones Java empaquetadas y el build React servido con Vite Preview. Verifica autenticación real, consulta del expediente, sesión y token CSRF de Java, renderizado de los cuatro catálogos, entrega de los archivos estáticos React y comunicación a través del proxy `/api`. Finalizó correctamente en los puertos alternativos 8097/8098/4173, ya que 8080 estaba ocupado.

Con los tres procesos iniciados en sus puertos predeterminados, desde la raíz del ejercicio:

```powershell
node scripts/smoke-http.mjs
```

Se pueden configurar `CLINICA_SMOKE_BACKEND`, `CLINICA_SMOKE_JAVA` y `CLINICA_SMOKE_REACT` para otros puertos; `CLINICA_SMOKE_USUARIO` y `CLINICA_SMOKE_CLAVE` permiten usar otra cuenta. El script necesita al menos una historia activa y no modifica datos de negocio.

## Recorrido para exponer ambas versiones

1. Iniciar backend y entrar en http://localhost:8081 con `demo` / `Consultorio123!`. Observar la historia ficticia inicial.
2. En **Pacientes**, registrar una persona con documento nuevo. Editar el nombre y verificar el listado. Intentar repetir el documento para observar el conflicto 409.
3. Subir una imagen PNG/JPEG de menos de 2 MB. Ver el avatar. Reemplazarla y quitarla. Un archivo renombrado con extensión `.png` que no contenga una imagen debe rechazarse.
4. En **Médicos**, dar de alta un profesional. En **Usuarios**, crear un responsable de prueba. Al editar, dejar la contraseña vacía para conservarla.
5. En **Historias clínicas**, seleccionar paciente y responsable y guardar. Abrir el expediente.
6. Registrar una atención de hoy con el médico creado y tipo **Consulta general**. Verificar la evolución y el contador.
7. Intentar guardar una evolución de 15 caracteres como **Control**: debe rechazarse. Ampliar el texto a 20 o más caracteres y guardar. Esta diferencia demuestra Strategy; la secuencia de alta de ambos casos sigue siendo Template Method.
8. Editar la atención, cambiar la evolución y guardar. Darla de baja y comprobar que desaparece del expediente.
9. Dar de baja la historia y comprobar que ya no figura. Intentar acceder por su ID retorna 404; sus detalles permanecen en la base con `eliminado=true`.
10. Abrir http://localhost:5173 e ingresar. Verificar que los cambios de la versión Java aparecen en React. Repetir alta, edición y atención desde React; actualizar la página Java para comprobar los mismos datos persistidos.
11. Reiniciar el backend desde la misma carpeta. Los datos deben conservarse y el seed no debe duplicarse.

Los datos del seed son ficticios. Las bajas se ocultan del ABM, pero no son borrados físicos. El ejercicio no incluye una pantalla para restaurarlas. Si se elimina o cambia la propia cuenta, las credenciales de esa sesión dejan de ser válidas y se debe ingresar con otra cuenta activa.
