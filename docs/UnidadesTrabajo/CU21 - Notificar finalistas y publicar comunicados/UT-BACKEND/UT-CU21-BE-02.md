# UT-CU21-BE-02 — Models, enums y repositories de `finalista` y `comunicado`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-02` |
| **Nombre** | Models, enums y repositories de `finalista` y `comunicado` |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok, `enums/`, «Model», sin `@Data`) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`@AttributeOverride` del código, enum nativo de PostgreSQL).
- Como referencia: `B/features/postulaciones/model/Postulacion.java` y `B/features/notificaciones/model/Notificacion.java`.

## Solicitud de negocio

El código necesita leer y guardar finalistas, comunicados y el contexto de cada aviso.

## Objetivo

Existen los Models, el enum y los repositories, y `Notificacion` guarda su postulación y su comunicado.

## Alcance

- `B/features/finalistas/model/Finalista.java` (`BaseModel`, `@AttributeOverride` → `codigo_finalista`, `@Getter`; `postulacionId` y `posicion` de solo lectura; setters solo de `plazoEntrega`, `notificadoEn` y `documentacionRecibidaEn`) y `repository/FinalistaRepository` con `findByCodigo` y `findByCodigoIn`.
- `B/features/comunicados/enums/DestinatariosComunicado.java` (`TODOS`, `FINALISTAS`, `ADJUDICADOS`), `model/Comunicado.java` (`@JdbcTypeCode(NAMED_ENUM)`; `convocatoriaId` y `publicadoPor` como ids simples) y `repository/ComunicadoRepository` con `findByCodigo`.
- `Notificacion`: columnas `postulacionId` y `comunicadoId` (ids simples, no relaciones) y un constructor que las recibe.
- Tests de repositorio (`@DataJpaTest` con base real, patrón `PostgresTestDatabase`): guardar y leer cada Model, `postulacionId` único, el borrado lógico los oculta, y el enum se guarda como texto.

Archivos esperados (crear): `Finalista`, `FinalistaRepository`, `DestinatariosComunicado`, `Comunicado`, `ComunicadoRepository` y sus tests. (Tocar): `Notificacion.java`.

## Fuera de alcance (no tocar)

- La lógica de negocio (UT-05 a 08) y las consultas de listado (UT-05).
- `auditoria_expediente`, adjudicaciones y cualquier columna de CU-05/07.

## Resultado esperado

Se puede guardar y leer un finalista, un comunicado y un aviso con su contexto.

## Validación

1. `./mvnw test` pasa (con base real o con `-Dtest.postgres.url`).
2. `./mvnw checkstyle:check` sin errores.
