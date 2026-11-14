# UT-CU07-BE-09 — Acceso temporal a un documento del expediente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-09` |
| **Nombre** | Acceso temporal a un documento del expediente |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-03`, `UT-CU07-BE-04`, `UT-CU07-BE-05` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

El administrador necesita abrir los documentos digitales para cotejarlos con los físicos.

## Objetivo

`ExpedienteService.accesoDocumento` devuelve una URL temporal para abrir un documento del finalista.

## Alcance

- `ExpedienteService.accesoDocumento(finalistaCodigo, documentoCodigo)` con `@PreAuthorize`: finalista inexistente → `404` `ERR_FIN_01`; documento inexistente o de otra postulación → `404` `ERR_EXP_05`.
- Genera la URL con `AlmacenamientoService.urlTemporal` (5 minutos) y devuelve `{ url, expiraEn }`. Nunca devuelve ni loguea la ruta guardada.
- Si MinIO falla, el error sube como excepción de infraestructura (`500` genérico, ya logueado); la pantalla ofrece reintentar (A4) y el resto del expediente sigue disponible.
- No anota la bitácora (es una lectura).
- Tests: documento propio, documento de otro finalista, documento inexistente y fallo de MinIO.

Archivos esperados (tocar): `ExpedienteService` y su test.

## Fuera de alcance (no tocar)

- Subir, borrar o reemplazar documentos (CU-16).
- Descargar todos los documentos en un archivo.

## Resultado esperado

El servicio devuelve una URL que se abre durante 5 minutos y deja de servir después.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
