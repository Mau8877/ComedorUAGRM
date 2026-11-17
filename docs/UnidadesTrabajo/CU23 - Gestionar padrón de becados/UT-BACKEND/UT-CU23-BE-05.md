# UT-CU23-BE-05 — Reemplazo de un cupo liberado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-05` |
| **Nombre** | Reemplazo de un cupo liberado |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | UT-CU23-BE-01, UT-CU23-BE-04 |

## Antes de implementar

[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y el README de esta
capa (decisiones sobre el reemplazante).

## Solicitud de negocio

Un cupo liberado por una baja se vuelve a ocupar con el siguiente postulante priorizado.

## Objetivo

Proponer al siguiente postulante disponible y, al confirmar, crear su beca y vincularla a la liberada.

## Alcance

- `PadronConsultaRepository.siguienteDisponible(convocatoriaId)`: el siguiente finalista por `posicion` con última
  auditoría «Validado», puntaje total ≥ mínimo, postulación «calificada» y sin beca en la convocatoria.
- `BecaEstadoService.candidatoDeReemplazo(codigo)` (solo lectura): exige beca en baja (`ERR_BEC_06`), sin reemplazo
  previo (`ERR_BEC_09`) y un candidato (`ERR_BEC_07`).
- `BecaEstadoService.reemplazar(actor, codigo, postulanteCodigo)` con `@Transactional` y `@Auditar(REEMPLAZO_BECA)`:
  bloquea la convocatoria y la beca, vuelve a calcular el candidato y exige que sea el confirmado (`ERR_BEC_08`);
  crea la `Beca` vigente (sin adjudicación, inicio = hoy en La Paz) y el `MovimientoBeca` `REEMPLAZO`.
- DTOs `CandidatoReemplazoResponse`, `ReemplazarRequest`, `ReemplazoResponse` y los dos endpoints.

Archivos esperados (crear/tocar): `PadronConsultaRepository.java`, `BecaEstadoService.java`, `BecaController.java`,
los tres DTO, `BitacoraAcciones.java`, `docs/errors/becas/ERRORES_BECAS.md`.

## Fuera de alcance (no tocar)

- Crear una adjudicación o cambiar el estado de la postulación del reemplazante.
- Una lista de espera de suplentes aparte de los finalistas.

## Resultado esperado

El Administrador ve a quién se propone y, al confirmar, esa persona queda con una beca vigente; la beca liberada queda
vinculada por el movimiento.

## Validación

1. `./mvnw compile` sin errores.
2. Pruebas de BE-06: propone al finalista correcto (el primero por posición que cumple), ignora no validados, bajo el
   mínimo y con beca; `409` sin baja previa, sin candidato, candidato distinto o ya reemplazada; una sola beca nueva
   aunque se repita la confirmación.
