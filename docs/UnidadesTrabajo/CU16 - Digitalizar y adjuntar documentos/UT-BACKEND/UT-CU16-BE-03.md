# UT-CU16-BE-03 — Model `DocumentoAdjunto` y consultas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-03` |
| **Nombre** | Model `DocumentoAdjunto` y consultas |
| **Caso de uso** | CU16-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- `DevolucionConsultaRepository` como modelo de estilo

## Solicitud de negocio

Hay que leer y escribir los archivos adjuntos y saber qué documentos exige cada convocatoria, sin leer repositorios de otros módulos.

## Objetivo

Existen el Model de `documento_adjunto`, su repositorio y las consultas de los documentos de una postulación.

## Alcance

- `B/features/postulaciones/model/DocumentoAdjunto.java`: extiende `BaseModel`, `@Table(name = "documento_adjunto")`, `codigo_documento_adjunto`; `postulacionId` y `tipoDocumentoId` como ids simples; `reemplazarArchivo(...)`. Solo `@Getter`.
- `B/features/postulaciones/repository/DocumentoAdjuntoRepository.java`: `findByCodigoAndPostulacionId` y `countByPostulacionIdAndTipoDocumentoId`.
- `B/features/postulaciones/repository/DocumentosPostulacionConsultaRepository.java` (JDBC): `cabecera(postulacion, usuario)` (la postulación **con** el usuario del token), `bloquearPostulacion`, `requeridos`, `tipoRequerido` y `adjuntos`.
- Tests: `T/features/postulaciones/repository/DocumentosPostulacionRepositoryTest.java` (`@DataJpaTest` con `PostgresTestDatabase`).

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Reglas de negocio (quién puede modificar y cuándo): `UT-CU16-BE-04`.
- Migraciones: la tabla ya existe.
- La consulta del expediente del CU07.

## Resultado esperado

Las consultas devuelven lo del postulante y nada eliminado ni ajeno.

## Validación

1. `./mvnw test -Dtest=DocumentosPostulacionRepositoryTest` pasa (con Docker o con `-Dtest.postgres.url`; sin base queda sin ejecutar).
2. `./mvnw checkstyle:check` sin errores.
