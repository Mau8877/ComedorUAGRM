# UT-CU01-BE-16 — Anotación `@Auditar` para la bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-16` |
| **Nombre** | Anotación `@Auditar` para la bitácora |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-04`, `UT-CU01-BE-11`, `UT-CU01-BE-13` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Anotar un evento en la bitácora no debe ensuciar la lógica de cada service ni obligar a pasar la IP por todas las firmas: los CU que vienen (P4, P5) van a anotar muchos eventos y el código tiene que seguir legible.

## Objetivo

Los eventos `REGISTRO_USUARIO` e `INICIO_SESION` se anotan con `@Auditar` sobre el método que los produce, y `AuthService` ya no llama a la bitácora ni recibe la IP.

## Alcance

- `B/common/auditoria/Auditar.java`: anotación de método con `accion` y `entidad`.
- `B/common/auditoria/Auditable.java`: contrato del resultado (`actorId`, `actorCodigo`, y por defecto `entidadId`, `detalle`, `auditar`). Vive en `common/` para que los módulos de negocio no dependan de `bitacora/`.
- `B/features/bitacora/AuditoriaAspect.java`: `@AfterReturning` que anota si el método terminó bien y su resultado es un `Auditable` con `auditar() == true`. La IP sale del request en curso (`RequestContextHolder`).
- `BitacoraService.registrar(Long actorId, UUID actorCodigo, ...)`: sobrecarga que usa una referencia (`EntityManager.getReference`) en lugar de consultar al usuario. La llamada directa existente se mantiene para eventos que no encajan en la anotación.
- Adopción: `UsuarioService.registrarPostulante` (`REGISTRO_USUARIO`, solo si `creado`) y `SesionService.abrir` (`INICIO_SESION`); `ResultadoRegistro` y `SesionAbierta` implementan `Auditable`. `AuthService` y `AuthController` dejan de recibir `ip` y `AuthService` deja de depender de `BitacoraService`.
- `pom.xml`: `spring-boot-starter-aspectj` (ya venía de forma transitiva; se declara por claridad).
- Tests: `AuditoriaAspectTest` (anota, sin IP fuera del request, `auditar() == false`, excepción, método sin anotar, resultado no auditable, llamada interna) y las aserciones de `AuthIntegrationTest` (actor, entidad, rol e IP; el refresh no anota).

Archivos esperados (crear/tocar): los de arriba, más `AuthServiceTest`, `AuthControllerTest` y `BitacoraServiceTest` ajustados a las nuevas firmas.

## Fuera de alcance (no tocar)

- Cambiar qué eventos se anotan o su contenido: se anota exactamente lo mismo que antes.
- Consultar la bitácora (CU25) y registrar intentos fallidos de inicio de sesión.
- Anotar eventos de otros CU.

## Resultado esperado

Un service anota un evento agregando `@Auditar(accion = ..., entidad = ...)` a un método cuyo resultado implemente `Auditable`.

## Validación

1. `./mvnw test` pasa, incluido `AuditoriaAspectTest`.
2. Con una base real (Docker, o `-Dtest.postgres.url=...` a una base vacía), `AuthIntegrationTest` pasa: una fila `REGISTRO_USUARIO` y una `INICIO_SESION` por registro, con actor, entidad, rol e IP; el refresh no anota.
3. `./mvnw checkstyle:check` sin errores.
