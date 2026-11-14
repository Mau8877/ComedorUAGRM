# UT-CU15-BE-02 — Columnas del enlace de restablecimiento en `usuario`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-02` |
| **Nombre** | Columnas del enlace de restablecimiento en `usuario` |
| **Caso de uso** | CU15-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (fuente de verdad del modelo, índices parciales, nombres de migración)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El sistema necesita recordar el enlace de recuperación vigente de cada cuenta y cuándo vence, sin guardar nunca el token. Solo hay un enlace vigente por cuenta: pedir otro reemplaza al anterior.

## Objetivo

`usuario` tiene dos columnas para el enlace de restablecimiento, y el modelo y el repositorio saben usarlas. El diagrama de la base de datos las refleja.

## Alcance

- `docs/DiagramaConceptual.mermaid`: en la entidad `USUARIO`, `varchar restablecimiento_token_hash UK "(64) nullable"` y `timestamptz restablecimiento_expira_en "nullable"`. Se anota en `docs/pendientes/DOCUMENTACION_GRADO_CU15.md` que la documentación de grado (fuera del repo) debe actualizarse igual.
- Migración `V{timestamp}__agregar_restablecimiento_a_usuario.sql` (formato `yyyyMMddHHmmss`, posterior a la última existente): `ALTER TABLE usuario ADD COLUMN restablecimiento_token_hash varchar(64), ADD COLUMN restablecimiento_expira_en timestamptz;` y un índice único parcial por `restablecimiento_token_hash` `WHERE restablecimiento_token_hash IS NOT NULL AND eliminado_en IS NULL`. Sin valores por defecto: las cuentas existentes quedan sin enlace.
- `B/features/usuarios/model/Usuario.java` (sin `@Setter` de los campos; solo métodos con intención): `iniciarRestablecimiento(String tokenHash, Instant expiraEn)` (**sobrescribe** lo anterior: así el enlace viejo deja de servir), `limpiarRestablecimiento()` (deja ambas columnas en `null`) y `tieneRestablecimientoVigente(Instant ahora)` (hash no nulo y `expiraEn` futuro).
- `B/features/usuarios/repository/UsuarioRepository.java`: `findByRestablecimientoTokenHash(String hash)`.
- Tests: `@DataJpaTest` con base real (`PostgresTestDatabase`): guardar y buscar por hash; `iniciarRestablecimiento` dos veces deja solo el último hash (el primero ya no se encuentra); `limpiarRestablecimiento` deja ambas columnas nulas; `tieneRestablecimientoVigente` (vigente, vencido, sin enlace); dos cuentas no pueden compartir el mismo hash (índice único).

Archivos esperados (crear): la migración y el test. (Tocar): `DiagramaConceptual.mermaid`, `Usuario.java` y `UsuarioRepository.java`.

## Fuera de alcance (no tocar)

- Generar o hashear tokens (`UT-CU15-BE-04`) y la lógica de solicitud o restablecimiento (UT-09 y UT-10).
- Una tabla aparte de enlaces o historial de enlaces emitidos (decisión del CU: un solo enlace vigente por cuenta).
- Que el hash de la contraseña (`password_hash`) cambie de forma: sigue sin setter (`UT-CU15-BE-06`).

## Resultado esperado

Flyway migra la base, Hibernate valida el modelo (`ddl-auto=validate`) y se puede guardar, buscar y limpiar el enlace de una cuenta.

## Validación

1. `./mvnw test` pasa (la prueba de repositorio queda SKIPPED sin Docker, ver `TESTING_BACKEND.md`).
2. Con la base levantada, la aplicación arranca sin errores de validación del esquema.
3. `./mvnw checkstyle:check` sin errores.
