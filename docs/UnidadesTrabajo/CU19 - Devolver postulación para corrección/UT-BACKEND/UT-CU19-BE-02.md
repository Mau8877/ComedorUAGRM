# UT-CU19-BE-02 — Model y repository del historial; la postulación puede devolverse

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-02` |
| **Nombre** | Model y repository del historial; la postulación puede devolverse |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `B/features/expedientes/model/AuditoriaExpediente.java` (append-only, ids simples) y `B/features/postulaciones/model/Postulacion.java`.

## Solicitud de negocio

El sistema necesita guardar y leer el historial de estados de una postulación y poder cambiarle el estado a «devuelta».

## Objetivo

Existen el Model del historial y su repository, y `Postulacion` sabe devolverse.

## Alcance

- `B/features/postulaciones/model/PostulacionHistorial.java` (extiende `BaseModel`, `@AttributeOverride` a `codigo_postulacion_historial`, `@Getter` y **sin ningún setter**: es append-only, igual que `AuditoriaExpediente`). Campos `postulacionId` y `usuarioId` como ids simples, `estadoAnterior` y `estadoNuevo` con el enum existente `EstadoPostulacion` (`@JdbcTypeCode(SqlTypes.NAMED_ENUM)`), `motivo` y un constructor con los datos del cambio.
- `B/features/postulaciones/repository/PostulacionHistorialRepository.java` (`JpaRepository<PostulacionHistorial, Long>`).
- `Postulacion` gana un único método `devolver()` que pasa `estado` de `calificada` a `devuelta` y lanza `IllegalStateException` si no está «calificada» (red de seguridad; la validación de negocio con su código `ERR_DEV_01` es del `service`, UT-05). No se agregan otros setters ni el constructor del CU-02.
- Tests con `@DataJpaTest` o Testcontainers: se guarda y se lee una fila con su enum, `devolver()` cambia el estado y falla desde otro estado.

Archivos esperados (crear): `PostulacionHistorial`, `PostulacionHistorialRepository` y sus tests. (Tocar): `Postulacion`.

## Fuera de alcance (no tocar)

- Reglas de negocio (UT-05).
- El constructor ni el reenvío de la postulación (CU-02).
- Cambios de estado distintos de «calificada → devuelta» (CU-20, CU-05).
- Editar o borrar filas del historial.

## Resultado esperado

Se puede guardar una fila de historial y devolver una `Postulacion` desde el código.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
