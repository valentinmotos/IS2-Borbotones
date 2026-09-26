# CLAUDE.md – E-Commerce Zero (Integrador N°1 · Ingeniería del Software II)

## Contexto

E-commerce web para la tienda de ropa deportiva "Zero" (Mendoza). Lo desarrollamos cuatro personas (Maxi, Valen, Manu y Diego), todas hacen back y front. El trabajo está dividido en **issues** con ID `Ex-yy` (ej: `E2-06`), organizados por etapas.

- **Stack:** Java 21 + Spring Boot 3, Thymeleaf (Layout Dialect + Extras Spring Security), Spring Data JPA con Hibernate, SQLite (`hibernate-community-dialects`), Spring Security con BCrypt, Spring Mail, Bootstrap 5 (template del equipo), SDK de Mercado Pago.
- **Arquitectura:** un solo proyecto (sin back y front separados). MVC en capas: controller → service → repository (DAO), con DTOs para reportes, dashboard y catálogo.
- Esta carpeta (`IntegradorN1`) es la raíz del proyecto, pero **no la raíz del repositorio Git**, que está un nivel más arriba.

## Documentación: la fuente de verdad

Leé siempre estos archivos antes de implementar algo:

- `docs/PlanDeTrabajo.md`: el plan completo. Tiene las **decisiones de diseño**, las **convenciones técnicas**, las **reglas del template** y la descripción de **cada issue** (responsable, dependencias, marcas ESPERAR, descripción y criterio de aceptación).
- `docs/DiagramaUML.uxf`: el diagrama de clases de diseño (XML de UMLet). Define entidades, atributos, métodos y relaciones.
- `README.md` (en esta carpeta): reglas y convenciones que el equipo fue definiendo, cómo correr el proyecto, usuarios de prueba y guías como el paso a paso para crear un ABM nuevo. Sus reglas son obligatorias, igual que las del plan.
- El resto de `docs/`: revisalo por si hay material adicional relevante para el issue.

**Prioridades cuando algo no coincide:**

1. Las decisiones de diseño del plan.
2. El diagrama UML.
3. Los requerimientos funcionales.

Si el `README.md` y el plan se contradicen, no elijas por tu cuenta: avisame y preguntame cuál vale.

Si el plan menciona la rama `develop`, ignoralo: **solo existe `main`**.

## Cuando te pida "hacé el issue Ex-yy"

Seguí estos pasos en orden, sin esperar a que te los repita:

1. **Leé el issue completo** en `docs/PlanDeTrabajo.md`, junto con los contratos del kickoff de su etapa, las decisiones de diseño y las convenciones técnicas. Leé también el `README.md` completo, porque puede tener reglas nuevas que no están en el plan.
2. **Verificá las dependencias.**
   - Revisá el "Depende de" y la marca ESPERAR del issue.
   - Buscá en el código que eso ya exista: clases, métodos, fragments, rutas.
   - **ESPERAR a Ex-yy** que no está implementado: frená y avisame antes de seguir.
   - **ESPERAR firma de Ex-yy** que falta: podés crear el método con la firma del contrato y una implementación mínima (stub), marcado con `// TODO Ex-yy`. Avisame que lo hiciste.
3. **Explorá el código existente antes de escribir.**
   - Mirá cómo están hechos los ABM, services, controllers y vistas actuales, en especial el CRUD de referencia de Nacionalidad (E0-06). Copiá su estructura, nombres y estilo.
   - Revisá `templates/layout/`, `templates/fragments/` y la página `/dev/componentes` (kit de componentes).
4. **Contame un plan corto** con los archivos a crear o modificar y cualquier decisión que no esté en el plan. Si no hay nada dudoso, seguí sin esperar confirmación.
5. **Implementá solo el alcance del issue.**
   - No hagas cosas de otros issues, aunque parezcan relacionadas. Si algo falta y es de otro issue, anotalo en el resumen final.
   - No refactorices código ajeno salvo que sea imprescindible, y en ese caso avisame.
6. **Verificá.**
   - Compilá y corré los tests: `mvnw.cmd clean verify` en Windows, `./mvnw clean verify` en Linux o Mac.
   - Si el issue tiene pantallas, levantá la app y revisá que no haya errores de Thymeleaf al renderizar.
   - Repasá el **criterio de aceptación** punto por punto.
7. **Cerrá con un resumen:**
   - Archivos creados y modificados.
   - Decisiones que tomaste que no estaban en el plan.
   - Stubs o `TODO` que dejaste y para qué issue.
   - Cómo probarlo a mano: URLs, usuario y rol, pasos.
   - Datos que agregaste al `DataSeeder`.

## MUY IMPORTANTE: el front sigue el diseño actual

- Toda vista nueva decora uno de los layouts existentes (`layout/admin.html` para el panel, `layout/publico.html` para el sitio) con `layout:decorate`. Nunca armes un `<head>` propio.
- Usá los **fragments del kit** (tabla, formulario, mensajes flash, modal de confirmación, paginación, badges de estado, filtros, kpi, card de producto). Si necesitás un componente que no existe, **avisame** en lugar de inventar uno con otro estilo.
- Usá solo clases del template y de Bootstrap 5. **Sin estilos inline ni CSS por página.** Los ajustes de marca van únicamente en `static/css/zero.css`.
- Los assets de `static/vendor/template/` no se modifican.
- Antes de crear una pantalla, abrí una pantalla parecida ya hecha y replicá su estructura: títulos, breadcrumb, botones, disposición de filtros y tablas.
- El sitio público tiene que ser responsive.

## Reglas de código

- **Capas.** Un controller nunca usa un repository: siempre pasa por un service. Los services no saben nada de la vista.
- **Un service por entidad**, con los métodos del diagrama (`crearX`, `validar`, `modificarX`, `eliminarX`, `buscarX`, `listarX`, `listarXActivo`…). Para validar un padre se usa el service del padre, no su repository.
- **Controllers:** uno por pantalla o ABM. Si un caso de uso cruza varias entidades, como la API de ubicación, lleva su propio controller.
- **Rutas:** `/admin/...` para el panel, `/cliente/...` para el cliente logueado, `/api/...` para JSON y el resto público.
- **Inyección de dependencias** por constructor. Nada de `@Autowired` en atributos.
- **Entidades:** todas extienden `BaseEntity` (`id` String UUID y `eliminado`).
- **Baja lógica siempre:** `eliminarX()` pone `eliminado = true`. No se borra nada físicamente.
- **Validaciones:** en el `validar(...)` del service, lanzando `ErrorServiceException` con un mensaje claro en español. El controller lo muestra con el mensaje flash del kit.
- **Nombres en español**, igual que el diagrama.
- **No agregues atributos ni entidades** que no estén en el diagrama o en las decisiones de diseño sin preguntarme.
- **Tests:** para cada service nuevo, al menos un test unitario con JUnit + Mockito de su validación principal.
- **Datos de prueba** en `DataSeeder`, en el método del grupo correspondiente y solo si la base está vacía.
- **Nada de sobreingeniería:** sin clases genéricas, capas extra ni abstracciones que el issue no pida. Lo simple y fácil de explicar en la defensa es mejor.

## Entorno

- Base local en `data/zero.db`, fuera de Git. Para resetearla, se borra el archivo y el seeder la vuelve a poblar al arrancar.
- Credenciales de Mercado Pago en la variable de entorno `MP_ACCESS_TOKEN`. Nunca hardcodees credenciales ni las subas al repo.
- Usuarios de prueba: ver el `README.md`.

## Git

- No hagas commits, push ni cambios de rama salvo que te lo pida.
- Si te lo pido, la rama sigue el formato `feature/Ex-yy-nombre-corto` (ej: `feature/E2-06-abm-usuarios`) y el mensaje de commit empieza con el ID del issue (ej: `E2-06: ABM de usuarios`).
