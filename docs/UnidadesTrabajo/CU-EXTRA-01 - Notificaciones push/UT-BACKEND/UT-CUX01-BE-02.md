# UT-CUX01-BE-02 — Tablas y modelos de notificaciones y dispositivos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-02` |
| **Nombre** | Tablas y modelos de notificaciones y dispositivos |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (identificador interno/público, columnas comunes, borrado lógico, enums, migraciones)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok en models, `enums/`)
- Mirar `NOTIFICACION` y `DISPOSITIVO_FCM` en [`DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid) y, antes de empezar, `diseno_base_de_datos.md` §4 de la documentación de grado para confirmar los valores de `tipo_notificacion`.

## Solicitud de negocio

Hace falta dónde guardar los avisos de cada usuario y los teléfonos registrados para recibir push.

## Objetivo

La base tiene las tablas `notificacion` y `dispositivo_fcm`, y el backend tiene sus models y repositories.

## Alcance

- Migración `V20261106100000__crear_tablas_notificaciones.sql`:
  - Enum de PostgreSQL `tipo_notificacion` (`SUCCESS`, `INFO`, `WARNING`, `ERROR`; confirmar contra el diseño de grado).
  - `notificacion`: `id`, `codigo_notificacion` (UUID único), `usuario_id` (FK a `usuario`), `titulo` varchar(150), `mensaje` text, `tipo`, `leida` boolean `DEFAULT false`, `enviada_en` timestamptz nulo, más `creado_en`, `actualizado_en` y `eliminado_en`. Índice `(usuario_id, creado_en DESC)` parcial `WHERE eliminado_en IS NULL`. **Sin** `comunicado_id` ni `postulacion_id` (ver README).
  - `dispositivo_fcm`: `id`, `codigo_dispositivo_fcm`, `usuario_id` (FK), `token` varchar(512), `plataforma` varchar(20), columnas comunes. Índice único parcial sobre `token` `WHERE eliminado_en IS NULL` e índice por `usuario_id`.
- `B/features/notificaciones/enums/TipoNotificacion.java` y `PlataformaDispositivo.java` (`ANDROID`; se suma `IOS` cuando se haga), persistidos como `STRING`.
- `B/features/notificaciones/model/Notificacion.java` y `DispositivoFcm.java`, extendiendo `BaseModel` con `@AttributeOverride` del código público, `@Getter` y `@Setter` solo en los campos que se modifican (`leida`, `enviadaEn`, `usuario`/`eliminadoEn` del dispositivo).
- `B/features/notificaciones/repository/NotificacionRepository.java` y `DispositivoFcmRepository.java` (`findByToken`, `findAllByUsuarioId`).
- Tests: `T/features/notificaciones/repository/` con `@DataJpaTest` y `PostgresTestDatabase` (guardar y leer, índice único del token con borrado lógico, filtro por usuario).

Archivos esperados (crear): la migración, 2 enums, 2 models, 2 repositories y sus tests.

## Fuera de alcance (no tocar)

- Columnas `comunicado_id` y `postulacion_id` (las agrega el CU que cree esas tablas).
- Servicios, controllers y endpoints de lectura de la bandeja.
- Cambiar `DiagramaConceptual.mermaid` (la diferencia con el diagrama es temporal y está documentada en el README).

## Resultado esperado

Flyway aplica la migración, `ddl-auto=validate` no se queja y los repositories guardan y leen filas.

## Validación

1. `./mvnw test` pasa, incluidos los tests de repositorio (con Docker disponible).
2. Una segunda fila con el mismo `token` activo viola el índice único; con la primera eliminada lógicamente, se permite.
3. `./mvnw checkstyle:check` sin errores.
