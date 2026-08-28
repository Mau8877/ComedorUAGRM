---
globs: projects/backend/**/*
---

# Testing — Backend

## Foco

Los tests se concentran en **`service`** (lógica de negocio) y **`controller`**
(contrato HTTP: status codes, forma del `ApiResponse`, validación de
input). No se exige cobertura exhaustiva de mappers triviales, DTOs o
getters/setters generados.

## Starters ya instalados — usar el que corresponde

El proyecto ya trae los starters de test de Spring Boot 4 en `pom.xml`; cada
tipo de test usa el que le corresponde, no todos en todos lados:

| Qué se testea | Starter / herramienta |
| --- | --- |
| Controllers (contrato HTTP, `MockMvc`) | `spring-boot-starter-webmvc-test` |
| Repositorios/queries JPA | `spring-boot-starter-data-jpa-test` (`@DataJpaTest`) |
| Endpoints protegidos / reglas de `SecurityConfig` | `spring-boot-starter-security-test` |
| Migraciones Flyway | `spring-boot-starter-flyway-test` |
| Validaciones Bean Validation en DTOs | `spring-boot-starter-validation-test` |
| Health/actuator | `spring-boot-starter-actuator-test` |

## Regla estricta: tests al día

Todo cambio de código que altera un comportamiento existente **debe**
actualizar sus tests correspondientes en el mismo cambio. No se permite:

- Dejar un test en rojo "para arreglar después".
- Dejar un test verde pero que ya no verifica lo que el código realmente
  hace (test desactualizado que pasa por casualidad).
- Comentar/skippear (`@Disabled`) un test para que el build pase, sin
  justificación explícita y un plan concreto de cuándo se re-habilita.

## Tests de integración con BD real: Testcontainers

Para tests que necesitan una base de datos real (no un mock del
repositorio) se usa **Testcontainers** con PostgreSQL — el mismo motor que
`docker-compose.yml` usa en desarrollo (`postgres:18-alpine`), para que el
comportamiento en test sea fiel al de verdad (constraints, tipos de dato,
etc.).

- **No está agregado todavía** en `pom.xml` — prerequisito antes de escribir
  el primer test de este tipo: agregar `org.testcontainers:postgresql` y
  `org.springframework.boot:spring-boot-testcontainers` (scope `test`).
- Se reservan para flujos donde el mock de repositorio no es suficiente
  (queries complejas, constraints de BD, comportamiento de Flyway). Para
  lógica de negocio pura en el `service`, se sigue mockeando el repositorio
  (`@Mock`/`@MockBean`) — no todo test pasa a ser de integración por default.

## Convenciones

- Clases de test: `XxxServiceTest`, `XxxControllerTest` (sufijo `Test`, no
  `Tests` ni `Spec`).
- Nombre de métodos: describen condición + resultado esperado, ej.
  `crearUsuario_conEmailDuplicado_lanzaConflictException`.
- Los tests de controller verifican también la forma del `ApiResponse`
  (`status`, `data`, `error` cuando corresponde) — no solo el status code
  HTTP. Un test que solo chequea `status().isOk()` sin mirar el body no
  cubre el contrato real de [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md).
