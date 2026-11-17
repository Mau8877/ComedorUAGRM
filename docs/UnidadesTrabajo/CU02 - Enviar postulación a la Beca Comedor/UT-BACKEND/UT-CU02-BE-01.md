# UT-CU02-BE-01 — Tabla `respuesta_socioeconomica`, errores `ERR_ENV_01` a `ERR_ENV_05` y consultas del envío

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-01` |
| **Nombre** | Tabla `respuesta_socioeconomica`, errores `ERR_ENV_01` a `ERR_ENV_05` y consultas del envío |
| **Caso de uso** | CU02-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

El formulario socioeconómico necesita dónde guardarse, el envío necesita sus códigos de error catalogados y el servicio, consultas que no dependan de repositorios de otros módulos.

## Objetivo

Existe la migración de `respuesta_socioeconomica`, las constantes `ERR_ENV_*` documentadas, el catálogo de opciones del formulario y `EnvioPostulacionConsultaRepository`.

## Alcance

- `src/main/resources/db/migration/V20261119100000__crear_respuesta_socioeconomica.sql`: PK = FK a `postulacion`, columnas de datos **nullables** (el borrador del CU17 guarda un avance parcial) y `CHECK` de rango solo cuando hay valor.
- `postulaciones/exception/EnvioPostulacionErrorCodes.java` y su catálogo en `docs/errors/` (formato de la guía).
- `postulaciones/service/OpcionesSocioeconomicas.java`: las listas del mockup (por definir con la DUBSS) y los topes numéricos.
- `postulaciones/repository/EnvioPostulacionConsultaRepository.java` (JDBC): `perfil`, `convocatoriaAbierta`, `proximaConvocatoria`, `postulacionDe`, `crearBorrador`, `cabecera`, `bloquearPostulacion`, `estadoActual`, `respuesta` y `guardarRespuesta` (`INSERT … ON CONFLICT`).
- `postulaciones/dto/`: `EnviarPostulacionRequest` y las respuestas del formulario y del comprobante.
- Su prueba con Postgres real: `EnvioPostulacionConsultaRepositoryTest`.

Archivos esperados (crear/tocar): la migración, `EnvioPostulacionErrorCodes.java`, `OpcionesSocioeconomicas.java`, `EnvioPostulacionConsultaRepository.java`, los DTO, `ERRORES_POSTULACIONES.md`, `errors/README.md`, `DiagramaConceptual.mermaid` (sin cambios de columnas) y el test del repositorio.

## Fuera de alcance (no tocar)

- El servicio y el controller (`UT-CU02-BE-02`).
- La llamada a la IA (`UT-CU02-BE-03`).
- Un `Model`/`BaseModelSinId` JPA de `respuesta_socioeconomica`: la escritura es un `UPSERT` JDBC (decisión 3 del README); el CU17 puede crearlo si lo necesita.
- `nota_academica`: el formulario no la pide.

## Resultado esperado

La tabla migra limpia y las consultas devuelven y guardan lo esperado.

## Validación

1. `./mvnw checkstyle:check` sin errores.
2. `EnvioPostulacionConsultaRepositoryTest` pasa contra un Postgres real (`-Dtest.postgres.url=…` si Testcontainers no conecta).
3. Cada `ERR_ENV_*` del `.java` tiene su sección y su enlace en `docs/errors/README.md`.
