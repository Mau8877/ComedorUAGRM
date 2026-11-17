# UT-CU17-BE-01 — Reglas de valor compartidas, consultas del borrador y `BorradorPostulacionService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-01` |
| **Nombre** | Reglas de valor compartidas, consultas del borrador y `BorradorPostulacionService` |
| **Caso de uso** | CU17-BE |
| **Depende de** | — (usa lo que ya existe del CU02, CU16 y CU18) |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (solo el `service` loguea; nunca datos personales)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El postulante necesita guardar su avance sin tener que completar todo, y volver a verlo después.

## Objetivo

Existe un servicio que guarda las respuestas parciales de una postulación en `borrador` o `devuelta`, valida solo lo escrito, y resume el borrador de la convocatoria abierta.

## Alcance

- `service/ReglasRespuestaSocioeconomica.java` (nuevo, paquete): `invalidos(Respuesta)` con las reglas de valor (ingresos, dependientes, opciones, promedio) que hoy están en el envío, aplicadas solo a los campos presentes, y `limpio(String)`. `EnvioPostulacionService.validarRespuestas` pasa a usarla (mismos mensajes).
- `repository/EnvioPostulacionConsultaRepository.java`: `guardadoEn(postulacionId)` (`actualizado_en` de la respuesta) y `borradorAbierto(usuarioCodigo, ahora)` (`BorradorAbierto`: solo `borrador` de una convocatoria `ABIERTA` y vigente).
- `dto/RespuestaSocioeconomicaResponse.java`: suma `guardadoEn`; `EnvioPostulacionService.formulario` lo llena.
- `dto/GuardarBorradorRequest.java`, `BorradorGuardadoResponse.java`, `BorradorResumenResponse.java`.
- `service/BorradorPostulacionService.java`: `guardar(usuarioCodigo, postulacionCodigo, request)` (`@PreAuthorize POSTULANTE`, `@Transactional`: cabecera → `404` `ERR_ENV_02`; bloqueo `FOR UPDATE`; estado releído, solo `borrador` o `devuelta` → si no, `409` `ERR_ENV_03`; `ValidacionPostulacionService.validar` (CU18); `ReglasRespuestaSocioeconomica.invalidos` → `400` `ERR_ENV_05`; `guardarRespuesta`; log `INFO` sin datos) y `obtener(usuarioCodigo)` (`@Transactional(readOnly)`: campos y documentos obligatorios que faltan).
- `BorradorPostulacionServiceTest` y los dos tests nuevos de `EnvioPostulacionConsultaRepositoryTest`.

Archivos esperados (crear/tocar): los listados arriba.

## Fuera de alcance (no tocar)

- El controller (UT-CU17-BE-02) y las pantallas.
- El estado, el historial, la bitácora y la IA: guardar no los toca.
- Eliminar el borrador y borrar borradores de convocatorias cerradas.
- Cualquier migración: la tabla ya admite campos vacíos.

## Resultado esperado

`BorradorPostulacionService.guardar` guarda parcial y rechaza lo inválido; `obtener` devuelve el resumen o vacío.

## Validación

1. `./mvnw test -Dtest='BorradorPostulacionServiceTest,EnvioPostulacionServiceTest'` en verde (el segundo prueba que el envío sigue con los mismos mensajes).
2. `./mvnw checkstyle:check` sin errores.
3. Con Docker: `EnvioPostulacionConsultaRepositoryTest` (`guardadoEn_…`, `borradorAbierto_…`) en verde.
