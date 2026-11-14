# UT-CU07-BE-03 — Model, enum y repository de la auditoría de expediente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-03` |
| **Nombre** | Model, enum y repository de la auditoría de expediente |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

El sistema necesita leer y guardar auditorías sin exponer ids internos.

## Objetivo

Existe el módulo `features/expedientes/` con el Model de la auditoría, su enum y su repository.

## Alcance

- `B/features/expedientes/enums/ResultadoAuditoria.java` (`VALIDADO`, `OBSERVADO`, `NO_VALIDO`).
- `B/features/expedientes/model/AuditoriaExpediente.java`: extiende `BaseModel` con `@AttributeOverride` a `codigo_auditoria_expediente`, `@Getter` sin ningún setter (es append-only), constructor con los campos de la auditoría; `finalistaId` y `auditorId` como ids simples (no depende de otras entidades); `resultado` con `@Enumerated(STRING)` y `@JdbcType(PostgreSQLEnumJdbcType.class)`; `observaciones`, `puntajeAcademico` (`BigDecimal`), `fechaAuditoria` (`Instant`).
- `AuditoriaExpedienteRepository extends JpaRepository<AuditoriaExpediente, Long>`: `findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc` y `findByFinalistaIdOrderByFechaAuditoriaDescIdDesc`.
- Tests `@DataJpaTest` contra Postgres real: guarda, devuelve la más reciente y respeta el borrado lógico.

Archivos esperados (crear): los tres de arriba y su test.

## Fuera de alcance (no tocar)

- Servicios, DTOs y controllers.
- Models de documentos o respuestas socioeconómicas: se leen con una consulta JDBC (UT-06); sus Models los crea el CU-16 / CU-02.

## Resultado esperado

Se puede guardar y consultar una auditoría desde un test.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
